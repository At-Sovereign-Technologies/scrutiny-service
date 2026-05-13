# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk AS compilacion
WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1000 app && adduser -u 1000 -G app -S app

COPY --from=compilacion /app/target/ConfiguracionEleccion-0.0.1-SNAPSHOT.jar aplicacion.jar
RUN chown app:app /app/aplicacion.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/aplicacion.jar"]