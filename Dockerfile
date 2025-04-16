FROM maven:3.9.9-amazoncorretto-21-alpine AS build
WORKDIR /app
# Since working directory is /app hence we can just copy things in current working directory
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/docker-app1-1.0-SNAPSHOT.jar .
CMD ["java", "-jar", "docker-app1-1.0-SNAPSHOT.jar"]