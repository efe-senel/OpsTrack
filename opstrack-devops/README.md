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

Docker, Docker Compose, Nginx, Jenkins and Zabbix are intentionally not configured yet in this stage.

## Requirements

Installed locally:

- Java 21
- Git
- Docker and Docker Compose, only required for PostgreSQL Testcontainers-based integration tests

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

```bash
./mvnw spring-boot:run
```

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
