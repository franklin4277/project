# Multi-stage build for a smaller runtime image
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests clean package
RUN cp target/*.jar /tmp/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN mkdir -p /app/data

COPY --from=build /tmp/app.jar /app/app.jar

ENV BANK_DB_URL=jdbc:sqlite:/app/data/bank.db
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
