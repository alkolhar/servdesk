# syntax=docker/dockerfile:1

# Multi-stage build: only the final image ships, keeping the JDK/Maven toolchain out of it.
# Tests are not run here (-DskipTests) — CI (.github/workflows/ci.yml) is where ./mvnw verify
# actually runs, against Testcontainers/PostgreSQL; a Docker build has no business re-running that.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependencies first so the resolution layer is only invalidated by a pom.xml change, not by
# every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -q package -DskipTests

# Extract the layered jar (dependencies / spring-boot-loader / snapshot-dependencies / application)
# so the runtime image below gets one Docker layer per Spring Boot layer, in most-to-least-stable
# order — a source change only invalidates the last, smallest one.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /workspace
COPY --from=build /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system --create-home --shell /usr/sbin/nologin servdesk
USER servdesk
WORKDIR /app

COPY --from=extract --chown=servdesk /workspace/extracted/dependencies/ ./
COPY --from=extract --chown=servdesk /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=servdesk /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=servdesk /workspace/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
