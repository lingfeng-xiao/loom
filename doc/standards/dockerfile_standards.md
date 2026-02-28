# Dockerfile Standards

This document defines the standards for Dockerfile files in the project.

---

## 1. Overview

Dockerfile is used to define the build process for Docker images. This document outlines the best practices and standards for writing Dockerfiles to ensure consistency, efficiency, and maintainability.

---

## 2. File Naming Conventions

### 2.1 Dockerfile Naming
- The main Dockerfile should be named `Dockerfile` (no extension)
- Use `Dockerfile.<variant>` for variant builds (e.g., `Dockerfile.dev`, `Dockerfile.prod`)
- Place Dockerfiles in the `docker/` directory at the project root

---

## 3. Dockerfile Structure

### 3.1 Base Image Selection
- Use official, minimal base images whenever possible
- Specify exact versions (e.g., `node:18-alpine`, not just `node:18`)
- Prefer Alpine-based images for smaller image sizes

### 3.2 Multi-Stage Builds
- Use multi-stage builds to reduce final image size
- Separate build and runtime environments
- Only copy necessary artifacts to the final stage

### 3.3 Layer Optimization
- Order instructions from least to most frequently changing
- Combine related commands to reduce layer count
- Use `&&` to chain commands and reduce intermediate layers

---

## 4. Best Practices

### 4.1 Security
- Run containers as non-root user whenever possible
- Avoid installing unnecessary packages
- Use `--no-install-recommends` for package installations
- Keep base images and dependencies updated

### 4.2 Efficiency
- Use `.dockerignore` to exclude unnecessary files
- Cache dependencies by copying package files first
- Minimize the number of layers
- Use `--chown` to set proper ownership when copying files

### 4.3 Maintainability
- Add comments to explain complex steps
- Use consistent indentation and formatting
- Keep Dockerfile logic clear and concise
- Document any build arguments or environment variables

---

## 5. Standard Structure

### 5.1 Example Dockerfile

```dockerfile
# ===============================
# Stage 1: Build Stage
# ===============================
FROM base-image:version AS build

WORKDIR /app

# Copy dependency files first for caching
COPY package*.json ./
RUN npm install

# Copy source code
COPY . .

# Build application
RUN npm run build

# ===============================
# Stage 2: Runtime Stage
# ===============================
FROM base-image:version AS runtime

WORKDIR /app

# Create non-root user
RUN adduser --system --group appuser
USER appuser

# Copy built artifacts from build stage
COPY --from=build /app/dist ./

# Expose ports
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["node", "server.js"]
```

---

## 6. Build Arguments

### 6.1 Common Build Arguments
- `BUILD_ARG` - Description of the argument
- Example: `ARG NODE_ENV=production`

### 6.2 Environment Variables
- Use `ENV` for runtime environment variables
- Example: `ENV NODE_ENV=production`

---

## 7. .dockerignore

### 7.1 Recommended .dockerignore

```
# Dependencies
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Build outputs
dist/
build/

# IDE and editor files
.vscode/
.idea/
*.swp
*.swo
*~

# OS files
.DS_Store
Thumbs.db

# Environment files
.env
.env.local
.env.development.local
.env.test.local
.env.production.local
```

---

## 8. Build Commands

### 8.1 Standard Build Command

```bash
docker build -t image-name -f docker/Dockerfile .
```

### 8.2 Build with Arguments

```bash
docker build --build-arg ARG_NAME=value -t image-name -f docker/Dockerfile .
```

---

## 9. Review Checklist

Before submitting Dockerfile changes:
- [ ] Uses official, minimal base images
- [ ] Implements multi-stage builds when appropriate
- [ ] Optimizes layer caching
- [ ] Runs as non-root user when possible
- [ ] Includes proper comments and documentation
- [ ] Has a corresponding `.dockerignore` file
- [ ] Follows the standard structure and naming conventions

---

## 10. Related Documents

- [Docker Documentation](https://docs.docker.com/engine/reference/builder/)
- [Dockerfile Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

*Last updated: 2026-02-28*