#!/bin/sh
set -eu

case "${1:-}" in
  backend_errors)
    query='sum(count_over_time({service="backend"} |~ "(?i)ERROR" [5m]))'
    ;;
  nginx_5xx)
    query='sum(count_over_time({service="nginx",status=~"5.."} [5m]))'
    ;;
  *)
    echo "ZBX_NOTSUPPORTED: unknown Loki query" >&2
    exit 1
    ;;
esac

encoded=$(printf '%s' "$query" | od -An -tx1 | tr -d ' \n' | sed 's/\(..\)/%\1/g')
response=$(wget -qO- "http://loki:3100/loki/api/v1/query?query=$encoded")
value=$(printf '%s' "$response" | sed -n 's/.*"value":\[[^,]*,"\([^"]*\)"\].*/\1/p')
printf '%s\n' "${value:-0}"
