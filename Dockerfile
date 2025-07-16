# Base image
FROM openjdk:17-jdk-slim

# Set the working directory in the Docker container
WORKDIR /app

RUN set -x \
    && apt-get update \
    && apt-get install -y wget gnupg2 unzip software-properties-common xz-utils libnss3 libxss1 libx11-xcb1 libxi6 libglib2.0-0

# Install necessary packages
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt install -y ./google-chrome-stable_current_amd64.deb \
    && rm google-chrome-stable_current_amd64.deb

RUN wget https://storage.googleapis.com/chrome-for-testing-public/124.0.6367.155/linux64/chromedriver-linux64.zip \
    && unzip chromedriver-linux64.zip -d /usr/local/bin \
    && mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver \
    && chmod +x /usr/local/bin/chromedriver \
    && rm -rf chromedriver-linux64.zip /usr/local/bin/chromedriver-linux64

# Copy the Gradle Wrapper and build scripts
COPY gradlew gradlew.bat /app/
COPY gradle /app/gradle
COPY build.gradle settings.gradle gradle.properties /app/

# Make the Gradle Wrapper executable
RUN chmod +x ./gradlew

# Copy the rest of the project
COPY . .

# Build the application
RUN ./gradlew build --no-daemon --info

# Copy the built application jar
COPY build/libs/*.jar /app/application.jar

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "application.jar"]
