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

The UI is available at `https://localhost` and the API at `https://localhost/api/v1/tasks`. A browser warning is expected while using a self-signed certificate. Compose connects the application and PostgreSQL only through the internal network; neither port 8080 nor 5432 is published to the host.

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

Keep real credentials in the local `.env` file. It is excluded from Git; only `.env.example` should be committed.

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
