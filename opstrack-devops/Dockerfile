# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
	&& cp target/*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S opstrack \
	&& adduser -S -G opstrack -h /app opstrack

WORKDIR /app
COPY --from=build --chown=opstrack:opstrack /workspace/application.jar application.jar

USER opstrack:opstrack
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
	CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/application.jar"]
