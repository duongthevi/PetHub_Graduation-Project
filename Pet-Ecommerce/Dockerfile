FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S petshop && adduser -S petshop -G petshop
COPY --from=build /app/target/Pet-Ecommerce-0.0.1-SNAPSHOT.jar app.jar
USER petshop
EXPOSE 2424
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
