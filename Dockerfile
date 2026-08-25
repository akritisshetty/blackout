# enigma/ is a local-only helper (see README.md) — not copied to the hosted image.
# It helps users get the correct answer of any algorithm (just for help).
# Only users who clone the repo and run locally can use it; hosted users won't see the repo.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/blackout-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
