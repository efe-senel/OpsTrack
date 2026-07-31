# OpsTrack

OpsTrack is a production-oriented task management application developed to practice backend development, containerization, CI/CD, centralized logging, monitoring, alerting and deployment workflows.

The project combines a Spring Boot REST API with PostgreSQL, a web frontend, Nginx, Docker Compose, Jenkins, Grafana, Loki, Alloy and Zabbix.

---

## Architecture

```text
                           User
                             |
                             v
                    +----------------+
                    |     Nginx      |
                    | Reverse Proxy  |
                    | HTTPS / TLS    |
                    +--------+-------+
                             |
                +------------+-------------+
                |                          |
                v                          v
        +---------------+          +---------------+
        |   Frontend    |          |  Spring Boot  |
        |     Nginx     |          |   REST API    |
        +---------------+          +-------+-------+
                                           |
                                           v
                                   +---------------+
                                   |  PostgreSQL   |
                                   +---------------+

        Logging and Monitoring

        Docker Logs
             |
             v
        +----------+       +----------+       +----------+
        |  Alloy   | ----> |   Loki   | ----> | Grafana  |
        +----------+       +----------+       +----------+

        Infrastructure and application monitoring

        +----------------+
        |     Zabbix     |
        | Server / Agent |
        +----------------+
```

---

## Technologies

### Backend

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Hibernate
- Bean Validation
- Spring Boot Actuator
- Maven

### Database

- PostgreSQL 16

### Testing

- JUnit
- Mockito
- Spring Boot Test
- Testcontainers
- MockMvc

### DevOps

- Docker
- Multi-stage Docker build
- Docker Compose
- Nginx
- HTTPS with a self-signed certificate
- Jenkins
- Docker Hub
- systemd
- Cron-based database backups

### Monitoring and Logging

- Grafana
- Loki
- Grafana Alloy
- Zabbix Server
- Zabbix Agent 2

---

## Features

- Task creation
- Task listing
- Task retrieval by ID
- Task update
- Task deletion
- Request validation
- Global exception handling
- PostgreSQL persistence
- Application health checks
- Container health checks
- Reverse proxy configuration
- HTTPS access
- Centralized container logging
- Grafana dashboards
- Zabbix monitoring and alerting
- Jenkins CI/CD pipeline
- Automated PostgreSQL backups
- Docker-based deployment

---

## Task Status Values

Tasks can have one of the following statuses:

```text
OPEN
IN_PROGRESS
DONE
```

---

## API Endpoints

Base path:

```text
/api/v1/tasks
```

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/tasks` | List all tasks |
| `GET` | `/api/v1/tasks/{id}` | Retrieve a task |
| `POST` | `/api/v1/tasks` | Create a task |
| `PUT` | `/api/v1/tasks/{id}` | Update a task |
| `DELETE` | `/api/v1/tasks/{id}` | Delete a task |
| `GET` | `/actuator/health` | Check application health |

---

## Example Request

Create a task:

```bash
curl -k -X POST https://localhost/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Prepare deployment",
    "description": "Deploy OpsTrack using Docker Compose",
    "status": "OPEN"
  }'
```

List tasks:

```bash
curl -k https://localhost/api/v1/tasks
```

Check application health:

```bash
curl -k https://localhost/actuator/health
```

Expected health response:

```json
{
  "status": "UP"
}
```

---

## Project Structure

```text
OpsTrack/
├── .github/
├── .mvn/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── ...
├── logging/
│   ├── alloy/
│   ├── grafana/
│   ├── zabbix/
│   └── loki.yml
├── nginx/
│   └── nginx.conf
├── src/
│   ├── main/
│   │   ├── java/com/opstrack/
│   │   │   ├── common/
│   │   │   └── task/
│   │   └── resources/
│   └── test/
├── zabbix/
├── .dockerignore
├── .env.example
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── Jenkinsfile
├── mvnw
├── pom.xml
└── README.md
```

---

## Environment Configuration

Create a local environment file:

```bash
cp .env.example .env
```

Then configure the required values in `.env`.

Example:

```env
POSTGRES_DB=opstrack
POSTGRES_USER=opstrack
POSTGRES_PASSWORD=change-me

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/opstrack
SPRING_DATASOURCE_USERNAME=opstrack
SPRING_DATASOURCE_PASSWORD=change-me

GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=change-me
```

Do not commit the real `.env` file.

Sensitive values such as passwords, SMTP credentials and private certificate keys must never be stored directly in the repository.

---

## TLS Certificate

The Nginx container expects a certificate and private key.

Example local certificate generation:

```bash
sudo mkdir -p /etc/ssl/opstrack

sudo openssl req \
  -x509 \
  -nodes \
  -days 365 \
  -newkey rsa:2048 \
  -keyout /etc/ssl/opstrack/opstrack.key \
  -out /etc/ssl/opstrack/opstrack.crt
```

Because the certificate is self-signed, browsers may display a security warning in the local environment.

---

## Run with Docker Compose

Validate the Compose configuration:

```bash
docker compose config
```

Build and start the application:

```bash
docker compose up -d --build
```

Check running containers:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f
```

Stop the application:

```bash
docker compose down
```

Stop the application and remove volumes:

```bash
docker compose down -v
```

Use the volume-removal command carefully because it deletes persisted database data.

---

## Local Access

| Service | Address |
|---|---|
| Frontend | `https://localhost` |
| REST API | `https://localhost/api/v1/tasks` |
| Health endpoint | `https://localhost/actuator/health` |
| Grafana | `http://localhost:3000` |
| Zabbix | `http://localhost:8082` |

---

## Testing

Run all tests:

```bash
./mvnw test
```

The project includes:

- Application context tests
- Service unit tests
- Controller integration tests
- PostgreSQL integration through Testcontainers

Latest local test result:

```text
Tests run: 5
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

---

## Docker Image

The backend application uses a multi-stage Docker build.

The build stage:

- Uses Maven and the JDK
- Downloads dependencies
- Compiles the project
- Creates the application JAR

The runtime stage:

- Uses a smaller Java runtime image
- Copies only the generated JAR
- Starts the Spring Boot application

This approach reduces the final image size and prevents build tools from being included in the runtime container.

---

## CI/CD Pipeline

The Jenkins pipeline contains stages for:

```text
Checkout
   |
   v
Build
   |
   v
Test
   |
   v
Docker Build
   |
   v
Docker Compose Deployment
   |
   v
Health Verification
```

Environment values required by Docker Compose are provided securely through Jenkins credentials rather than being hardcoded in the pipeline.

---

## Logging

Docker container logs are collected by Grafana Alloy.

The logging flow is:

```text
Docker containers
       |
       v
Grafana Alloy
       |
       v
Loki
       |
       v
Grafana
```

Grafana can be used to query and visualize centralized application and infrastructure logs.

---

## Monitoring and Alerting

Zabbix is used for infrastructure and service monitoring.

The monitoring stack includes:

- Zabbix Server
- Zabbix Web
- Zabbix Agent 2
- PostgreSQL database for Zabbix
- Automated host configuration
- Alert configuration

Grafana is used for dashboards and log analysis, while Zabbix is used for host monitoring and alert generation.

---

## Database Backup

PostgreSQL backups can be created using `pg_dump`.

Example:

```bash
docker compose exec -T postgres \
  pg_dump -U opstrack opstrack \
  | gzip > opstrack-backup.sql.gz
```

Example restore:

```bash
gzip -dc opstrack-backup.sql.gz \
  | docker compose exec -T postgres \
    psql -U opstrack -d opstrack
```

Backups can be scheduled using Cron.

---

## Security Notes

- Real `.env` files are ignored by Git.
- Passwords must not be committed.
- SMTP application passwords must not be committed.
- TLS private keys must not be committed.
- Database backups must not be committed.
- Jenkins credentials should be stored in Jenkins Credentials.
- Production deployments should use a trusted TLS certificate.
- Default passwords should be replaced before deployment.

---

## Development Progress

The project was developed incrementally through the following stages:

1. Spring Boot REST API
2. PostgreSQL persistence
3. Validation and global exception handling
4. Unit and integration testing
5. Multi-stage Dockerfile
6. Docker Compose deployment
7. Frontend integration
8. Nginx reverse proxy
9. HTTPS configuration
10. Jenkins pipeline
11. Grafana, Loki and Alloy logging
12. Zabbix monitoring and alerting
13. Automated backup and restore workflow

---

## Future Improvements

- JWT authentication and authorization
- Role-based access control
- Database migrations with Flyway
- Prometheus metrics
- Kubernetes deployment
- Cloud deployment
- Trusted domain certificate
- Automated release workflow
- Improved frontend features
- Additional unit and integration tests

---

## Author

**Mustafa Efe Şenel**

Computer Engineering Student  
Backend and DevOps Learner
