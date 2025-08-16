# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the project files (pom.xml first to take advantage of Docker cache)
COPY pom.xml .

# Copy the rest of the source code
COPY src ./src

# Build the application using Maven
# The -DskipTests flag skips tests to speed up the build process in Docker.
RUN mvn clean package -DskipTests

# Stage 2: Create the final, lightweight image
FROM eclipse-temurin:17-jre-focal

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the 'build' stage
# The 'target' folder contains the generated JAR file.
COPY --from=build /app/target/notes_summarizer-0.0.1-SNAPSHOT.jar summarizer.jar

# Expose the port that the application will run on
# This should match the server.port in your application.properties file (default is 8080).
EXPOSE 8080

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "summarizer.jar"]
