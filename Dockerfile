FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
COPY build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
