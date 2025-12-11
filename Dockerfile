
# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy the Maven wrapper and pom.xml first to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Grant execution rights on the mvnw wrapper
RUN chmod +x mvnw
# Download dependencies (this step will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built artifact from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
