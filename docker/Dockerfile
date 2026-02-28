# ===============================
# Stage 1: Build Frontend
# ===============================
FROM node:18-alpine AS frontend

WORKDIR /frontend

# Copy dependency files first for caching
COPY frontend/package*.json ./
RUN npm install

# Copy source code
COPY frontend .

# Build frontend application
RUN npm run build

# ===============================
# Stage 2: Build Backend
# ===============================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy backend source code
COPY backend .

# Copy frontend build artifacts to Spring Boot static resources
COPY --from=frontend /frontend/dist ./src/main/resources/static

# Maven build with tests skipped
RUN mvn clean package -DskipTests

# ===============================
# Stage 3: Runtime
# ===============================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user
RUN adduser --system --group appuser
USER appuser

# Copy built jar from build stage
COPY --from=build /build/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]