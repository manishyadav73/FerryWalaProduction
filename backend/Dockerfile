# Build: docker build -t ferrywala-backend backend/backend
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY .mvn .mvn
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package && cp target/*.jar /app.jar

FROM eclipse-temurin:17-jre
RUN useradd --system --create-home --uid 10001 ferry && mkdir -p /data/uploads && chown ferry /data/uploads
USER ferry
WORKDIR /home/ferry
COPY --from=build /app.jar app.jar
ENV UPLOAD_DIR=/data/uploads JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"
VOLUME /data/uploads
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
