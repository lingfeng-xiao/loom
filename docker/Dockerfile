FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY ../backend/target/app.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
