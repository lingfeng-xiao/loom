# ===============================
# Stage 1: Build Frontend
# ===============================
FROM node:18-alpine AS frontend

WORKDIR /frontend

# 只复制依赖文件（利用缓存）
COPY frontend/package.json frontend/package-lock.json ./

# 更稳定的安装方式
RUN npm ci --registry=https://registry.npmmirror.com

# 复制源码
COPY frontend .

RUN npm run build


# ===============================
# Stage 2: Build Backend
# ===============================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# 先复制 pom.xml，利用缓存
COPY backend/pom.xml .

# 先下载依赖（极大提升缓存命中率）
RUN mvn dependency:go-offline -B

# 再复制源码
COPY backend .

# 拷贝前端产物
COPY --from=frontend /frontend/dist ./src/main/resources/static

# 打包
RUN mvn clean package -DskipTests


# ===============================
# Stage 3: Runtime
# ===============================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appuser && adduser -S appuser -G appuser

# 复制 jar
COPY --from=build /build/target/*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]