# Maven Standards

This document defines the Maven configuration standards for the project.

---

## 1. Overview

This document establishes the Maven configuration standards, including project structure, pom.xml configuration, dependency management, and best practices. The standards aim to ensure consistency, maintainability, and reliability across all Maven-based projects.

---

## 2. Project Structure

### 2.1 Directory Layout

```
project-root/
├── pom.xml              # Project pom
├── module1/             # Module 1
│   ├── pom.xml
│   └── src/
├── module2/             # Module 2
│   ├── pom.xml
│   └── src/
└── README.md
```

### 2.2 Source Directory Structure

```
src/
├── main/
│   ├── java/            # Java source files
│   ├── resources/       # Resource files
│   └── webapp/          # Web application files (if applicable)
└── test/
    ├── java/            # Test source files
    └── resources/       # Test resource files
```

---

## 3. POM Configuration

### 3.1 Basic Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Project coordinates -->
    <groupId>com.example</groupId>
    <artifactId>project-name</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <!-- Project metadata -->
    <name>Project Name</name>
    <description>Project description</description>
    <url>https://example.com/project</url>

    <!-- Properties -->
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <!-- Dependencies -->
    <dependencies>
        <!-- Add dependencies here -->
    </dependencies>

    <!-- Build configuration -->
    <build>
        <plugins>
            <!-- Add plugins here -->
        </plugins>
    </build>
</project>
```

### 3.2 Dependency Management

- Use property variables for version numbers
- Group dependencies by functionality
- Use dependency management for multi-module projects
- Specify scope for each dependency

### 3.3 Plugin Configuration

- Use property variables for plugin versions
- Configure plugins in the build section
- Use appropriate plugins for the project needs
- Configure plugin goals and phases

---

## 4. Best Practices

### 4.1 Version Management

- Use semantic versioning (MAJOR.MINOR.PATCH)
- Use SNAPSHOT versions for development
- Use release versions for production
- Avoid hardcoding versions in dependencies

### 4.2 Dependency Management

- Use the latest stable versions
- Remove unused dependencies
- Avoid dependency conflicts
- Use dependency tree to analyze dependencies

### 4.3 Build Optimization

- Use Maven Wrapper for consistent builds
- Configure build profiles for different environments
- Use incremental builds
- Optimize build time with parallel builds

### 4.4 Code Quality

- Use Maven plugins for code quality checks
- Configure checkstyle, PMD, and findbugs
- Enforce code coverage with JaCoCo
- Run tests during the build process

---

## 5. Multi-module Projects

### 5.1 Parent POM

- Define common dependencies in parent POM
- Configure plugin management in parent POM
- Set properties in parent POM
- Inherit from parent POM in child modules

### 5.2 Module Structure

- Organize modules by functionality
- Use consistent naming conventions
- Define module dependencies clearly
- Configure module build order

---

## 6. Maven Wrapper

### 6.1 Configuration

- Include Maven Wrapper in the project
- Configure wrapper version in .mvn/wrapper/maven-wrapper.properties
- Use wrapper scripts for consistent builds

### 6.2 Usage

```bash
# Unix/Linux
./mvnw clean install

# Windows
mvnw.cmd clean install
```

---

## 7. CI/CD Integration

### 7.1 Build Automation

- Configure Maven goals for CI/CD
- Use Maven profiles for different environments
- Set up automated testing
- Configure deployment goals

### 7.2 Artifact Management

- Deploy artifacts to a repository
- Use Maven Central or private repository
- Configure artifact signing
- Manage release process

---

## 8. Troubleshooting

### 8.1 Common Issues

- Dependency conflicts
- Build failures
- Plugin configuration issues
- Repository connectivity issues

### 8.2 Resolution Strategies

- Use dependency tree to identify conflicts
- Check Maven logs for errors
- Verify plugin configurations
- Test with different Maven versions

---

## 9. Related Documents

- [Documentation Standards](documentation_standards.md)
- [Backend Coding Standards](backend_coding_standards.md)
- [Git Standards](git_standards.md)

---

*Last updated: 2026-02-28*