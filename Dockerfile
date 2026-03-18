FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/
COPY server/pom.xml server/
COPY shared/src shared/src
COPY server/src server/src
RUN mvn -pl shared,server package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/server/target/server-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
