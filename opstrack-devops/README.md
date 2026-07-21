# OpsTrack DevOps

OpsTrack is a small internship-oriented DevOps project. This first stage contains a Java 21 Spring Boot REST API for managing tasks with PostgreSQL persistence.

## Current Scope

- Java 21
- Spring Boot
- Maven Wrapper
- PostgreSQL configuration
- REST CRUD API
- Bean Validation
- Global exception handling
- Actuator health endpoint
- Unit and integration tests

Docker and Docker Compose are included for running the API with PostgreSQL. Nginx, Jenkins and Zabbix are intentionally not configured yet.

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

Docker Compose builds the Java 21 application image and starts it together with PostgreSQL. Copy the example environment file and replace the placeholder password with a local value:

```bash
cp .env.example .env
docker compose up -d --build
```

The API is available at `http://localhost:8080`. Compose connects the application to PostgreSQL through the internal hostname `postgres`; PostgreSQL is not exposed to the host.

Check service state and health:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
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
```

### Docker environment variables

| Variable | Purpose | Example value |
| --- | --- | --- |
| `POSTGRES_DB` | PostgreSQL database name | `opstrack` |
| `POSTGRES_USER` | PostgreSQL user | `opstrack` |
| `POSTGRES_PASSWORD` | Local PostgreSQL password; replace the placeholder | `use-a-local-secret` |
| `APP_PORT` | Host port mapped to container port 8080 | `8080` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema behavior | `update` |

Keep real credentials in the local `.env` file. It is excluded from Git; only `.env.example` should be committed.

## Test

```bash
./mvnw test
```

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
curl -X POST http://localhost:8080/api/v1/tasks \
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
curl -i -X POST http://localhost:8080/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare Docker setup","description":"Containerize OpsTrack","status":"OPEN"}'
```

List tasks and retrieve a task by ID:

```bash
curl -i http://localhost:8080/api/v1/tasks
curl -i http://localhost:8080/api/v1/tasks/1
```

Update task `1`:

```bash
curl -i -X PUT http://localhost:8080/api/v1/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare Docker setup","description":"Docker stage verified","status":"DONE"}'
```

Delete task `1`:

```bash
curl -i -X DELETE http://localhost:8080/api/v1/tasks/1
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
