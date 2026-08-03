# OpsTrack DevOps

OpsTrack is a task tracking application with a Java 21 Spring Boot API, React frontend and PostgreSQL persistence.

## Current Scope

- Java 21
- Spring Boot
- Maven Wrapper
- PostgreSQL configuration
- REST CRUD API
- Bean Validation
- Global exception handling
- Actuator health endpoint
- React and Vite frontend
- Nginx HTTPS reverse proxy
- Zabbix 7.0 server, web UI and host monitoring agent
- Loki, Grafana Alloy and Grafana centralized container logging
- Unit and integration tests

Docker Compose runs PostgreSQL, the API, the frontend and the public Nginx reverse proxy. PostgreSQL and the API are available only on the internal Compose network.

## Requirements

Installed locally:

- Java 21
- Git
- Docker and Docker Compose, required for the containerized stack and PostgreSQL Testcontainers-based integration tests

Maven does not need to be installed system-wide because the project includes `./mvnw`.

## Configuration

Create local environment variables from `.env.example` when running against a local PostgreSQL instance. Do not commit real passwords or secrets.

Default application values:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/opstrack
SPRING_DATASOURCE_USERNAME=opstrack
SPRING_DATASOURCE_PASSWORD=opstrack
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

## Run

To run the application directly against an available PostgreSQL instance:

```bash
./mvnw spring-boot:run
```

## Run with Docker

Docker Compose builds the application images and starts the complete stack. Copy the example environment file, replace the placeholder database password and point the TLS variables at the existing self-signed certificate and key:

```bash
cp .env.example .env
docker compose up -d --build
```

The UI is available at `https://localhost`, the API at `https://localhost/api/v1/tasks`, and Swagger UI at `https://localhost/swagger-ui.html`. The raw OpenAPI document is available at `https://localhost/v3/api-docs`. A browser warning is expected while using a self-signed certificate. Compose connects the application and PostgreSQL only through the internal network; neither port 8080 nor 5432 is published to the host.

Check service state and health:

```bash
docker compose ps
curl -k https://localhost/actuator/health
```

Stop the containers without deleting PostgreSQL data:

```bash
docker compose down
```

Start them again using the existing named volume:

```bash
docker compose up -d
```

Do not add `--volumes` to `docker compose down` when the database data must be retained. The `opstrack_postgres_data` named volume survives ordinary container removal and restart.

To follow all logs, or only one service's logs:

```bash
docker compose logs -f
docker compose logs -f app
docker compose logs -f postgres
docker compose logs -f frontend
docker compose logs -f nginx
```

### Docker environment variables

| Variable | Purpose | Example value |
| --- | --- | --- |
| `POSTGRES_DB` | PostgreSQL database name | `opstrack` |
| `POSTGRES_USER` | PostgreSQL user | `opstrack` |
| `POSTGRES_PASSWORD` | Local PostgreSQL password; replace the placeholder | `use-a-local-secret` |
| `HTTP_PORT` | Public HTTP port (redirects to HTTPS) | `80` |
| `HTTPS_PORT` | Public HTTPS port | `443` |
| `TLS_CERT_PATH` | Existing certificate path on the host | `./nginx/certs/certificate.crt` |
| `TLS_KEY_PATH` | Existing private key path on the host | `./nginx/certs/private.key` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema behavior | `update` |
| `ZABBIX_DB_NAME` | Dedicated Zabbix PostgreSQL database | `zabbix` |
| `ZABBIX_DB_USER` | Dedicated Zabbix PostgreSQL user | `zabbix` |
| `ZABBIX_DB_PASSWORD` | Zabbix database password | `use-another-local-secret` |
| `ZABBIX_ADMIN_USER` | Zabbix bootstrap administrator | `Admin` |
| `ZABBIX_ADMIN_PASSWORD` | Zabbix bootstrap administrator password | `zabbix` |
| `ZABBIX_AGENT_HOSTNAME` | Host name shown in Zabbix | `opstrack-docker-host` |
| `ZABBIX_WEB_PORT` | Zabbix web UI host port | `8082` |
| `TZ` | Zabbix web UI timezone | `Europe/Istanbul` |
| `ZABBIX_SMTP_SERVER` | SMTP server used by the Zabbix Gmail media type | Gmail SMTP hostname |
| `ZABBIX_SMTP_PORT` | SMTP submission port | `587` |
| `ZABBIX_SMTP_SECURITY` | SMTP transport security | `STARTTLS` |
| `ZABBIX_SMTP_EMAIL` | Notification sender address | Local `.env` only |
| `ZABBIX_SMTP_USERNAME` | SMTP authentication username | Local `.env` only |
| `ZABBIX_SMTP_APP_PASSWORD` | Google application password | Local `.env` only |
| `ZABBIX_ALERT_RECIPIENT` | OpsTrack alarm recipient | Local `.env` only |
| `GRAFANA_PORT` | Grafana web UI host port | `3000` |
| `GRAFANA_ADMIN_USER` | Local Grafana administrator | `admin` |
| `GRAFANA_ADMIN_PASSWORD` | Local Grafana administrator password | Local `.env` only |

Keep real credentials in the local `.env` file. It is excluded from Git; only `.env.example` should be committed.

## Monitoring with Zabbix

The Compose stack includes a dedicated Zabbix PostgreSQL database. It does not
reuse the OpsTrack database or `opstrack_postgres_data`. Zabbix data is stored in
`opstrack_zabbix_db_data`; scripts use their own named volumes.

The Zabbix Agent 2 container mounts the host filesystem read-only at `/hostfs`
and shares the host PID namespace. Explicit agent items collect host CPU,
memory, root-filesystem and network metrics without mistaking container bind
mounts for host filesystems. The agent port is only available on
`opstrack_monitoring_network` and is not published to the host.

Start or update the complete stack:

```bash
cp .env.example .env
# Replace the placeholder passwords in .env.
docker compose up -d --build
docker compose ps -a
```

Open Zabbix at `http://localhost:8082`. On a new database, sign in with the
official initial account (`Admin` / `zabbix`) and change its password
immediately. If it is changed, also update `ZABBIX_ADMIN_PASSWORD` in the local
`.env`; this keeps the idempotent bootstrap service able to authenticate.

The one-shot `zabbix-bootstrap` service creates:

- the `opstrack-docker-host` host and explicit host-root CPU, RAM, disk and
  network items;
- `OpsTrack actuator health`, which expects `"status":"UP"` from
  `https://nginx/actuator/health`;
- `OpsTrack HTTPS availability`, which expects HTTP 200 from `https://nginx/`.

### Day 17 triggers and dashboard

The bootstrap also creates or updates these triggers idempotently:

| Trigger | Expression/threshold | Severity |
| --- | --- | --- |
| Host CPU utilization | Every collected value remains above 80% for 5 minutes | Warning |
| Host RAM utilization | Used RAM is above 85% | Warning |
| Host root disk utilization | Used root filesystem space is above 80% | Warning |
| Actuator health check | `web.test.fail[OpsTrack actuator health]` is non-zero | High |
| HTTPS availability | `web.test.fail[OpsTrack HTTPS availability]` is non-zero | High |
| Zabbix Agent availability | No `agent.ping` data for 3 minutes | Average |

All triggers have `service=opstrack` and `managed-by=bootstrap` tags. Web
scenarios run once per minute with two retries, so a web alarm is not expected
to appear immediately after a service stops.

The global dashboard named `OpsTrack Operations` contains:

- a resource graph for CPU, RAM and root-disk utilization;
- a network receive/transmit throughput graph;
- an OpsTrack web-monitoring status widget;
- an active-problems widget filtered to the OpsTrack host.

In the web UI, open **Dashboards → OpsTrack Operations**. Trigger definitions
are under **Data collection → Hosts → opstrack-docker-host → Triggers**.
Generated and recovered alarms are under **Monitoring → Problems**.

### Gmail notifications

The bootstrap creates or updates the following objects:

- the enabled `OpsTrack Gmail SMTP` media type using port 587, STARTTLS, peer
  verification and host verification;
- an enabled recipient media record on the bootstrap administrator user;
- the `OpsTrack Gmail notifications` trigger action;
- problem and recovery HTML message templates.

The notification action matches trigger events tagged
`service=opstrack`. Its problem operation sends through the Gmail media type,
and its recovery operation notifies the same involved recipient.

Sender, SMTP username, Google application password and recipient must exist
only in the Git-ignored `.env`. Do not pass them on the command line or include
them in logs. Apply changes without displaying the resolved Compose
configuration:

```bash
docker compose config --quiet
docker compose run --rm zabbix-bootstrap
```

To test both notification directions, use the safe frontend interruption
procedure below. In the Zabbix UI, inspect delivery under
**Reports → Action log** and confirm that both the PROBLEM and RECOVERY entries
have status **Sent**. SMTP acceptance confirms that Zabbix handed each message
to Gmail; mailbox placement can additionally be checked in the recipient
mailbox.

To safely verify the HTTPS alarm without stopping the backend health endpoint:

```bash
docker compose stop frontend
curl --max-time 15 -k -o /dev/null -w '%{http_code}\n' \
  https://localhost/

# Allow approximately 1-2 minutes for the web scenario retries, then inspect:
docker compose logs --since=3m zabbix-server

docker compose start frontend
until [ "$(docker inspect -f '{{.State.Health.Status}}' \
  opstrack-devops-frontend-1)" = healthy ]; do sleep 2; done
curl -k -I https://localhost/
```

Always start `frontend` again even if an intermediate check fails. This test
does not remove containers, networks, volumes or database data.

The development certificate is self-signed, so peer and host verification are
disabled only for these internal Zabbix web scenarios.

Useful checks:

```bash
docker compose ps -a
docker compose logs zabbix-server zabbix-agent zabbix-web zabbix-bootstrap
curl -k https://localhost/actuator/health
curl -I http://localhost:8082/
```

Restart the bootstrap after changing its configuration:

```bash
docker compose run --rm zabbix-bootstrap
```

Stop without deleting either database:

```bash
docker compose down
```

Never add `--volumes` when existing OpsTrack or Zabbix monitoring history must
be retained.

## Centralized logging

The low-resource logging path is:

```text
app / nginx / frontend stdout
          |
          v
 Docker json-file logs -- Grafana Alloy --> Loki --> Grafana
                                           |
                                           v
                            Zabbix external items and Gmail action
```

Grafana Alloy discovers only the `app`, `nginx` and `frontend` containers in
this Compose project through the read-only Docker socket. Logs receive
`service`, `compose_service`, `container` and `project` labels:

| Compose service | `service` label | Parsed label |
| --- | --- | --- |
| `app` | `backend` | Spring log `level` |
| `nginx` | `nginx` | HTTP `status` |
| `frontend` | `frontend` | HTTP `status` |

Promtail is not used because it reached end of life in March 2026. Alloy is its
supported, lightweight collection replacement and sends logs to Loki through
the internal-only `logging_network`. Loki has no published host port.

Open Grafana at `http://localhost:3000` and sign in with the credentials from
the Git-ignored `.env`. The provisioned **OpsTrack Logs** dashboard is under
**Dashboards → OpsTrack**. Its panels show all selected container logs,
backend WARN/ERROR entries and public Nginx 4xx/5xx responses. The Loki data
source is provisioned automatically.

Useful queries in **Explore → Loki**:

```logql
{service="backend"} |~ "(?i)(WARN|ERROR)"
{service="nginx", status=~"4..|5.."}
{compose_service="app"}
{container="opstrack-devops-nginx-1"}
```

The Zabbix bootstrap adds two one-minute external items that query the last
five minutes of Loki data. Their tagged triggers reuse the existing
`service=opstrack` Gmail action:

| Trigger | Threshold | Severity |
| --- | --- | --- |
| Repeated backend ERROR logs | At least 3 entries in 5 minutes | Average |
| Nginx 5xx response | At least 1 entry in 5 minutes | High |

Inspect them in Zabbix under **Data collection → Hosts →
opstrack-docker-host → Items/Triggers**. Active and recovered events are under
**Monitoring → Problems**, and message delivery is under **Reports → Action
log**. SMTP credentials and recipient values remain only in `.env`.

Each collected application container and each added logging container uses
Docker's `json-file` rotation with a 10 MiB maximum file and three files per
container. Loki stores at most seven days in `opstrack_loki_data`; its
compactor removes expired chunks. These limits prevent unbounded log growth
while keeping Docker and Loki data separate from both PostgreSQL volumes.

Start or update logging without removing any volume:

```bash
docker compose config --quiet
docker compose up -d loki alloy grafana zabbix-server
docker compose run --rm zabbix-bootstrap
docker compose ps -a
```

Safe health and troubleshooting commands:

```bash
curl -fsS http://localhost:3000/api/health
docker exec opstrack-devops-grafana-1 \
  wget -qO- http://loki:3100/ready
docker compose logs --since=10m loki alloy grafana
docker compose logs --since=10m app nginx frontend
docker stats --no-stream
```

To create a controlled Nginx 5xx log while leaving the backend running, stop
the frontend briefly and always start it again:

```bash
docker compose stop frontend
curl --max-time 15 -k -o /dev/null -w '%{http_code}\n' https://localhost/
docker compose start frontend
until [ "$(docker inspect -f '{{.State.Health.Status}}' \
  opstrack-devops-frontend-1)" = healthy ]; do sleep 2; done
curl -k https://localhost/actuator/health
```

The request should return `502`. In Grafana, use
`{service="nginx", status="502"}`. Zabbix should open the high-severity log
trigger on its next poll, then recover after the five-minute query window
expires. Do not use `docker compose down --volumes` during any logging test.

## Test

```bash
./mvnw test
cd frontend
npm install
npm run build
```

For frontend development, start the backend separately and run:

```bash
cd frontend
cp .env.example .env.local
npm run dev
```

Vite proxies relative `/api` requests to `VITE_DEV_API_TARGET`. Production code does not contain a direct backend host dependency.

## API Endpoints

Base path: `/api/v1/tasks`

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/tasks` | List all tasks |
| `GET` | `/api/v1/tasks/{id}` | Get one task |
| `POST` | `/api/v1/tasks` | Create a task |
| `PUT` | `/api/v1/tasks/{id}` | Update a task |
| `DELETE` | `/api/v1/tasks/{id}` | Delete a task |
| `GET` | `/actuator/health` | Application health |

Example create request:

```bash
curl -k -X POST https://localhost/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare DevOps pipeline","description":"Plan Docker, Jenkins and monitoring stages","status":"OPEN"}'
```

Valid task statuses:

- `OPEN`
- `IN_PROGRESS`
- `DONE`

## API test examples

Create a task:

```bash
curl -k -i -X POST https://localhost/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare Docker setup","description":"Containerize OpsTrack","status":"OPEN"}'
```

List tasks and retrieve a task by ID:

```bash
curl -k -i https://localhost/api/v1/tasks
curl -k -i https://localhost/api/v1/tasks/1
```

Update task `1`:

```bash
curl -k -i -X PUT https://localhost/api/v1/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare Docker setup","description":"Docker stage verified","status":"DONE"}'
```

Delete task `1`:

```bash
curl -k -i -X DELETE https://localhost/api/v1/tasks/1
```

## Error Format

Errors return a consistent JSON body:

```json
{
  "timestamp": "2026-07-20T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/v1/tasks",
  "validationErrors": {
    "title": "Title is required"
  }
}
```
