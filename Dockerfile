FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately so `mvn package` doesn't redownload the
# internet every time only src/ changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p data
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
