#!/bin/sh
set -eu

apk add --no-cache curl jq >/dev/null

api_call() {
  method=$1
  params=$2
  auth=${3:-null}
  curl --fail --silent --show-error \
    --header 'Content-Type: application/json-rpc' \
    --data "{\"jsonrpc\":\"2.0\",\"method\":\"${method}\",\"params\":${params},\"auth\":${auth},\"id\":1}" \
    "$ZABBIX_URL"
}

echo "Waiting for the Zabbix API..."
until api_call apiinfo.version '{}' | jq -e '.result' >/dev/null 2>&1; do
  sleep 5
done

login_response=$(api_call user.login "$(jq -cn \
  --arg username "$ZABBIX_ADMIN_USER" \
  --arg password "$ZABBIX_ADMIN_PASSWORD" \
  '{username:$username,password:$password}')")
auth=$(printf '%s' "$login_response" | jq -r '.result // empty')
if [ -z "$auth" ]; then
  echo "Zabbix login failed: $login_response" >&2
  exit 1
fi
auth_json=$(printf '%s' "$auth" | jq -R .)

group_id=$(api_call hostgroup.get '{"filter":{"name":["Linux servers"]}}' "$auth_json" | jq -r '.result[0].groupid')

host_id=$(api_call host.get "$(jq -cn --arg host "$ZABBIX_AGENT_HOSTNAME" \
  '{filter:{host:[$host]}}')" "$auth_json" | jq -r '.result[0].hostid // empty')

if [ -z "$host_id" ]; then
  host_response=$(api_call host.create "$(jq -cn \
    --arg host "$ZABBIX_AGENT_HOSTNAME" \
    --arg group_id "$group_id" \
    '{host:$host,groups:[{groupid:$group_id}],
      interfaces:[{type:1,main:1,useip:0,ip:"",dns:"zabbix-agent",port:"10050"}]}')" "$auth_json")
  host_id=$(printf '%s' "$host_response" | jq -r '.result.hostids[0] // empty')
  if [ -z "$host_id" ]; then
    echo "Could not create monitored host: $host_response" >&2
    exit 1
  fi
  echo "Created Zabbix host: $ZABBIX_AGENT_HOSTNAME"
else
  echo "Zabbix host already exists: $ZABBIX_AGENT_HOSTNAME"
fi

# The generic Linux template discovers container bind mounts. Day 16 uses
# explicit host-root items below so only real host metrics are collected.
linked_templates=$(api_call host.get "$(jq -cn --arg host_id "$host_id" \
  '{hostids:[$host_id],selectParentTemplates:["templateid"]}')" "$auth_json" |
  jq -c '.result[0].parentTemplates // []')
if [ "$linked_templates" != "[]" ]; then
  api_call host.update "$(jq -cn \
    --arg host_id "$host_id" --argjson templates "$linked_templates" \
    '{hostid:$host_id,templates_clear:$templates}')" "$auth_json" >/dev/null
  echo "Removed container-oriented template; explicit host-root items remain."
fi

interface_id=$(api_call hostinterface.get "$(jq -cn --arg host_id "$host_id" \
  '{hostids:[$host_id],filter:{type:1,main:1}}')" "$auth_json" | jq -r '.result[0].interfaceid')

create_web_scenario() {
  scenario_name=$1
  scenario_url=$2
  required_text=$3
  verify_peer=$4

  existing=$(api_call httptest.get "$(jq -cn \
    --arg host_id "$host_id" --arg name "$scenario_name" \
    '{hostids:[$host_id],filter:{name:[$name]}}')" "$auth_json" | jq -r '.result[0].httptestid // empty')
  if [ -n "$existing" ]; then
    echo "Web scenario already exists: $scenario_name"
    return
  fi

  response=$(api_call httptest.create "$(jq -cn \
    --arg host_id "$host_id" --arg name "$scenario_name" \
    --arg url "$scenario_url" --arg required "$required_text" \
    --argjson verify_peer "$verify_peer" \
    '{hostid:$host_id,name:$name,delay:"1m",retries:"2",
      verify_peer:$verify_peer,verify_host:0,
      steps:[{name:$name,url:$url,status_codes:"200",required:$required,no:1,timeout:"10s"}]}')" "$auth_json")
  if ! printf '%s' "$response" | jq -e '.result.httptestids[0]' >/dev/null; then
    echo "Could not create web scenario '$scenario_name': $response" >&2
    exit 1
  fi
  echo "Created web scenario: $scenario_name"
}

create_web_scenario "OpsTrack actuator health" "https://nginx/actuator/health" '"status":"UP"' 0
create_web_scenario "OpsTrack HTTPS availability" "https://nginx/" "" 0

create_agent_item() {
  item_name=$1
  item_key=$2
  units=$3
  value_type=$4

  existing=$(api_call item.get "$(jq -cn \
    --arg host_id "$host_id" --arg key "$item_key" \
    '{hostids:[$host_id],filter:{key_:[$key]}}')" "$auth_json" | jq -r '.result[0].itemid // empty')
  if [ -n "$existing" ]; then
    echo "Agent item already exists: $item_name"
    return
  fi

  response=$(api_call item.create "$(jq -cn \
    --arg host_id "$host_id" --arg name "$item_name" \
    --arg key "$item_key" --arg units "$units" --arg interface_id "$interface_id" \
    --argjson value_type "$value_type" \
    '{hostid:$host_id,name:$name,key_:$key,type:0,value_type:$value_type,
      units:$units,delay:"1m",interfaceid:$interface_id}')" "$auth_json")
  if ! printf '%s' "$response" | jq -e '.result.itemids[0]' >/dev/null; then
    echo "Could not create agent item '$item_name': $response" >&2
    exit 1
  fi
  echo "Created agent item: $item_name"
}

create_agent_item "Host CPU utilization" "opstrack.host.cpu.util" "%" 0
create_agent_item "Host memory total" "opstrack.host.memory.total" "B" 3
create_agent_item "Host memory available" "opstrack.host.memory.available" "B" 3
create_agent_item "Host root disk total" "opstrack.host.disk.total" "B" 3
create_agent_item "Host root disk available" "opstrack.host.disk.available" "B" 3
create_agent_item "Host network received" "opstrack.host.network.rx" "B" 3
create_agent_item "Host network transmitted" "opstrack.host.network.tx" "B" 3

create_agent_item "Zabbix agent availability" "agent.ping" "" 3

create_calculated_item() {
  item_name=$1
  item_key=$2
  units=$3
  formula=$4

  item_id=$(api_call item.get "$(jq -cn \
    --arg host_id "$host_id" --arg key "$item_key" \
    '{hostids:[$host_id],filter:{key_:[$key]}}')" "$auth_json" | jq -r '.result[0].itemid // empty')
  if [ -n "$item_id" ]; then
    echo "Calculated item already exists: $item_name"
    return
  fi

  response=$(api_call item.create "$(jq -cn \
    --arg host_id "$host_id" --arg name "$item_name" \
    --arg key "$item_key" --arg units "$units" --arg formula "$formula" \
    '{hostid:$host_id,name:$name,key_:$key,type:15,value_type:0,
      units:$units,delay:"1m",params:$formula}')" "$auth_json")
  if ! printf '%s' "$response" | jq -e '.result.itemids[0]' >/dev/null; then
    echo "Could not create calculated item '$item_name': $response" >&2
    exit 1
  fi
  echo "Created calculated item: $item_name"
}

create_calculated_item "Host memory utilization" "opstrack.host.memory.util" "%" \
  '100*(last(//opstrack.host.memory.total)-last(//opstrack.host.memory.available))/last(//opstrack.host.memory.total)'
create_calculated_item "Host root disk utilization" "opstrack.host.disk.util" "%" \
  '100*(last(//opstrack.host.disk.total)-last(//opstrack.host.disk.available))/last(//opstrack.host.disk.total)'
create_calculated_item "Host network receive rate" "opstrack.host.network.rx.rate" "Bps" \
  'change(//opstrack.host.network.rx)/60'
create_calculated_item "Host network transmit rate" "opstrack.host.network.tx.rate" "Bps" \
  'change(//opstrack.host.network.tx)/60'

create_external_item() {
  item_name=$1
  item_key=$2

  item_id=$(api_call item.get "$(jq -cn \
    --arg host_id "$host_id" --arg key "$item_key" \
    '{hostids:[$host_id],filter:{key_:[$key]}}')" "$auth_json" | jq -r '.result[0].itemid // empty')
  if [ -n "$item_id" ]; then
    echo "External item already exists: $item_name"
    return
  fi

  response=$(api_call item.create "$(jq -cn \
    --arg host_id "$host_id" --arg name "$item_name" --arg key "$item_key" \
    '{hostid:$host_id,name:$name,key_:$key,type:10,value_type:3,
      units:"events",delay:"1m"}')" "$auth_json")
  if ! printf '%s' "$response" | jq -e '.result.itemids[0]' >/dev/null; then
    echo "Could not create external item '$item_name': $response" >&2
    exit 1
  fi
  echo "Created external item: $item_name"
}

create_external_item "Backend ERROR logs in the last 5 minutes" \
  "loki-count.sh[backend_errors]"
create_external_item "Nginx 5xx responses in the last 5 minutes" \
  "loki-count.sh[nginx_5xx]"

create_trigger() {
  trigger_name=$1
  expression=$2
  priority=$3

  trigger_id=$(api_call trigger.get "$(jq -cn \
    --arg host_id "$host_id" --arg name "$trigger_name" \
    '{hostids:[$host_id],filter:{description:[$name]}}')" "$auth_json" | jq -r '.result[0].triggerid // empty')
  if [ -n "$trigger_id" ]; then
    response=$(api_call trigger.update "$(jq -cn \
      --arg trigger_id "$trigger_id" --arg expression "$expression" \
      --argjson priority "$priority" \
      '{triggerid:$trigger_id,expression:$expression,priority:$priority,
        tags:[{tag:"service",value:"opstrack"},{tag:"managed-by",value:"bootstrap"}]}')" "$auth_json")
    action=Updated
  else
    response=$(api_call trigger.create "$(jq -cn \
      --arg name "$trigger_name" --arg expression "$expression" \
      --argjson priority "$priority" \
      '{description:$name,expression:$expression,priority:$priority,
        manual_close:1,
        tags:[{tag:"service",value:"opstrack"},{tag:"managed-by",value:"bootstrap"}]}')" "$auth_json")
    action=Created
  fi
  if ! printf '%s' "$response" | jq -e '.result.triggerids[0]' >/dev/null; then
    echo "Could not create/update trigger '$trigger_name': $response" >&2
    exit 1
  fi
  echo "$action trigger: $trigger_name"
}

create_trigger "OpsTrack host CPU utilization is above 80% for 5 minutes" \
  "min(/${ZABBIX_AGENT_HOSTNAME}/opstrack.host.cpu.util,5m)>80" 2
create_trigger "OpsTrack host RAM utilization is above 85%" \
  "last(/${ZABBIX_AGENT_HOSTNAME}/opstrack.host.memory.util)>85" 2
create_trigger "OpsTrack host disk utilization is above 80%" \
  "last(/${ZABBIX_AGENT_HOSTNAME}/opstrack.host.disk.util)>80" 2
create_trigger "OpsTrack actuator health check failed" \
  "last(/${ZABBIX_AGENT_HOSTNAME}/web.test.fail[OpsTrack actuator health])>0" 4
create_trigger "OpsTrack HTTPS availability check failed" \
  "last(/${ZABBIX_AGENT_HOSTNAME}/web.test.fail[OpsTrack HTTPS availability])>0" 4
create_trigger "OpsTrack Zabbix agent is unreachable" \
  "nodata(/${ZABBIX_AGENT_HOSTNAME}/agent.ping,3m)=1" 3
create_trigger "OpsTrack backend has repeated ERROR logs" \
  "last(/${ZABBIX_AGENT_HOSTNAME}/loki-count.sh[backend_errors])>=3" 3
create_trigger "OpsTrack Nginx returned a 5xx response" \
  "last(/${ZABBIX_AGENT_HOSTNAME}/loki-count.sh[nginx_5xx])>0" 4

item_id() {
  api_call item.get "$(jq -cn \
    --arg host_id "$host_id" --arg key "$1" \
    '{hostids:[$host_id],filter:{key_:[$key]}}')" "$auth_json" | jq -r '.result[0].itemid'
}

cpu_item_id=$(item_id "opstrack.host.cpu.util")
memory_item_id=$(item_id "opstrack.host.memory.util")
disk_item_id=$(item_id "opstrack.host.disk.util")
network_rx_item_id=$(item_id "opstrack.host.network.rx.rate")
network_tx_item_id=$(item_id "opstrack.host.network.tx.rate")

dashboard_name="OpsTrack Operations"
dashboard_id=$(api_call dashboard.get "$(jq -cn --arg name "$dashboard_name" \
  '{filter:{name:[$name]}}')" "$auth_json" | jq -r '.result[0].dashboardid // empty')

dashboard_pages=$(jq -cn \
  --arg host_id "$host_id" \
  --arg cpu "$cpu_item_id" --arg memory "$memory_item_id" --arg disk "$disk_item_id" \
  --arg network_rx "$network_rx_item_id" --arg network_tx "$network_tx_item_id" \
  '[{name:"Day 17 monitoring",display_period:30,widgets:[
    {type:"svggraph",name:"Host resource utilization",x:0,y:0,width:36,height:6,view_mode:0,fields:[
      {type:1,name:"reference",value:"RESRC"},
      {type:0,name:"ds.0.dataset_type",value:0},{type:4,name:"ds.0.itemids.0",value:$cpu},{type:1,name:"ds.0.color.0",value:"FF465C"},
      {type:0,name:"ds.1.dataset_type",value:0},{type:4,name:"ds.1.itemids.0",value:$memory},{type:1,name:"ds.1.color.0",value:"00A650"},
      {type:0,name:"ds.2.dataset_type",value:0},{type:4,name:"ds.2.itemids.0",value:$disk},{type:1,name:"ds.2.color.0",value:"2774A4"},
      {type:0,name:"show_problems",value:1}
    ]},
    {type:"svggraph",name:"Host network throughput",x:36,y:0,width:36,height:6,view_mode:0,fields:[
      {type:1,name:"reference",value:"NETWK"},
      {type:0,name:"ds.0.dataset_type",value:0},{type:4,name:"ds.0.itemids.0",value:$network_rx},{type:1,name:"ds.0.color.0",value:"00A650"},
      {type:0,name:"ds.1.dataset_type",value:0},{type:4,name:"ds.1.itemids.0",value:$network_tx},{type:1,name:"ds.1.color.0",value:"FFB300"}
    ]},
    {type:"web",name:"OpsTrack web checks",x:0,y:6,width:24,height:5,view_mode:0,fields:[
      {type:1,name:"reference",value:"WEBCK"},{type:3,name:"hostids.0",value:$host_id},{type:0,name:"rf_rate",value:30}
    ]},
    {type:"problems",name:"OpsTrack active alarms",x:24,y:6,width:48,height:5,view_mode:0,fields:[
      {type:1,name:"reference",value:"PROBS"},{type:3,name:"hostids.0",value:$host_id},
      {type:0,name:"show",value:3},{type:0,name:"show_opdata",value:1},{type:0,name:"rf_rate",value:30}
    ]}
  ]}]')

if [ -n "$dashboard_id" ]; then
  dashboard_response=$(api_call dashboard.update "$(jq -cn \
    --arg dashboard_id "$dashboard_id" --arg name "$dashboard_name" \
    --argjson pages "$dashboard_pages" \
    '{dashboardid:$dashboard_id,name:$name,display_period:30,auto_start:1,pages:$pages}')" "$auth_json")
  dashboard_action=Updated
else
  dashboard_response=$(api_call dashboard.create "$(jq -cn \
    --arg name "$dashboard_name" --argjson pages "$dashboard_pages" \
    '{name:$name,display_period:30,auto_start:1,pages:$pages}')" "$auth_json")
  dashboard_action=Created
fi
if ! printf '%s' "$dashboard_response" | jq -e '.result.dashboardids[0]' >/dev/null; then
  echo "Could not create/update dashboard: $dashboard_response" >&2
  exit 1
fi
echo "$dashboard_action dashboard: $dashboard_name"

case "$ZABBIX_SMTP_SECURITY" in
  STARTTLS) smtp_security=1 ;;
  NONE) smtp_security=0 ;;
  SSL|SSL/TLS) smtp_security=2 ;;
  *)
    echo "Unsupported ZABBIX_SMTP_SECURITY value; use STARTTLS, SSL/TLS or NONE." >&2
    exit 1
    ;;
esac

media_type_name="OpsTrack Gmail SMTP"
message_templates=$(jq -cn '[
  {eventsource:0,recovery:0,
   subject:"[OpsTrack PROBLEM] {EVENT.NAME}",
   message:"<b>OpsTrack monitoring problem</b><br>Host: {HOST.NAME}<br>Problem: {EVENT.NAME}<br>Severity: {EVENT.SEVERITY}<br>Started: {EVENT.DATE} {EVENT.TIME}<br>Operational data: {EVENT.OPDATA}<br>Event ID: {EVENT.ID}"},
  {eventsource:0,recovery:1,
   subject:"[OpsTrack RECOVERY] {EVENT.NAME}",
   message:"<b>OpsTrack monitoring recovered</b><br>Host: {HOST.NAME}<br>Problem: {EVENT.NAME}<br>Recovered: {EVENT.RECOVERY.DATE} {EVENT.RECOVERY.TIME}<br>Duration: {EVENT.DURATION}<br>Event ID: {EVENT.ID}"}
]')

media_type_id=$(api_call mediatype.get "$(jq -cn --arg name "$media_type_name" \
  '{filter:{name:[$name]}}')" "$auth_json" | jq -r '.result[0].mediatypeid // empty')
media_type_params=$(jq -cn \
  --arg name "$media_type_name" \
  --arg server "$ZABBIX_SMTP_SERVER" \
  --argjson port "$ZABBIX_SMTP_PORT" \
  --arg email "$ZABBIX_SMTP_EMAIL" \
  --arg username "$ZABBIX_SMTP_USERNAME" \
  --arg password "$ZABBIX_SMTP_APP_PASSWORD" \
  --argjson security "$smtp_security" \
  --argjson templates "$message_templates" \
  '{name:$name,type:0,provider:1,smtp_server:$server,smtp_port:$port,
    smtp_email:$email,smtp_security:$security,smtp_verify_peer:1,smtp_verify_host:1,
    smtp_authentication:1,username:$username,passwd:$password,status:0,
    message_format:1,maxattempts:3,attempt_interval:"10s",
    message_templates:$templates}')

if [ -n "$media_type_id" ]; then
  media_type_response=$(api_call mediatype.update "$(printf '%s' "$media_type_params" |
    jq --arg mediatype_id "$media_type_id" '. + {mediatypeid:$mediatype_id}')" "$auth_json")
  media_type_action=Updated
else
  media_type_response=$(api_call mediatype.create "$media_type_params" "$auth_json")
  media_type_action=Created
fi
media_type_id=$(printf '%s' "$media_type_response" | jq -r '.result.mediatypeids[0] // empty')
if [ -z "$media_type_id" ]; then
  echo "Could not create/update Gmail media type." >&2
  exit 1
fi
echo "$media_type_action Gmail media type."

recipient_user=$(api_call user.get "$(jq -cn --arg username "$ZABBIX_ADMIN_USER" \
  '{filter:{username:[$username]},selectMedias:"extend"}')" "$auth_json")
recipient_user_id=$(printf '%s' "$recipient_user" | jq -r '.result[0].userid // empty')
if [ -z "$recipient_user_id" ]; then
  echo "Could not find the Zabbix notification recipient user." >&2
  exit 1
fi
preserved_medias=$(printf '%s' "$recipient_user" | jq -c \
  --arg mediatype_id "$media_type_id" \
  '[.result[0].medias[]? | select(.mediatypeid != $mediatype_id) |
    {mediatypeid,sendto,active,severity,period}]')
recipient_medias=$(jq -cn \
  --argjson existing "$preserved_medias" \
  --arg mediatype_id "$media_type_id" \
  --arg recipient "$ZABBIX_ALERT_RECIPIENT" \
  '$existing + [{mediatypeid:$mediatype_id,sendto:[$recipient],
    active:0,severity:63,period:"1-7,00:00-24:00"}]')
user_media_response=$(api_call user.update "$(jq -cn \
  --arg user_id "$recipient_user_id" --argjson medias "$recipient_medias" \
  '{userid:$user_id,medias:$medias}')" "$auth_json")
if ! printf '%s' "$user_media_response" | jq -e '.result.userids[0]' >/dev/null; then
  echo "Could not create/update recipient media." >&2
  exit 1
fi
echo "Updated notification recipient media."

notification_action_name="OpsTrack Gmail notifications"
notification_action_id=$(api_call action.get "$(jq -cn --arg name "$notification_action_name" \
  '{eventsource:0,filter:{name:[$name]}}')" "$auth_json" | jq -r '.result[0].actionid // empty')
notification_action_params=$(jq -cn \
  --arg name "$notification_action_name" \
  --arg user_id "$recipient_user_id" \
  --arg mediatype_id "$media_type_id" \
  '{name:$name,eventsource:0,status:0,esc_period:"1m",
    filter:{evaltype:1,conditions:[
      {conditiontype:26,operator:0,value:"opstrack",value2:"service"}
    ]},
    operations:[{operationtype:0,esc_step_from:1,esc_step_to:1,
      opmessage_usr:[{userid:$user_id}],
      opmessage:{default_msg:1,mediatypeid:$mediatype_id}}],
    recovery_operations:[{operationtype:11,
      opmessage:{default_msg:1}}]}')

if [ -n "$notification_action_id" ]; then
  notification_action_response=$(api_call action.update "$(printf '%s' "$notification_action_params" |
    jq --arg action_id "$notification_action_id" '. + {actionid:$action_id}')" "$auth_json")
  notification_action=Updated
else
  notification_action_response=$(api_call action.create "$notification_action_params" "$auth_json")
  notification_action=Created
fi
if ! printf '%s' "$notification_action_response" | jq -e '.result.actionids[0]' >/dev/null; then
  echo "Could not create/update notification action: $notification_action_response" >&2
  exit 1
fi
echo "$notification_action Gmail notification action."

echo "Zabbix bootstrap completed."
