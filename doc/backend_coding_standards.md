# Backend Coding Standards

This document defines the coding standards for all backend code in the project.

---

## 1. Overview

This document establishes the coding standards for backend development, including file structure, naming conventions, code style, and best practices. The standards aim to ensure consistency, readability, and maintainability across all backend codebase.

---

## 2. File Structure

### 2.1 Directory Organization

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── controller/    # API controllers
│   │   │           ├── service/       # Business logic
│   │   │           ├── repository/    # Data access layer
│   │   │           ├── model/         # Data models
│   │   │           ├── dto/           # Data transfer objects
│   │   │           ├── config/        # Configuration
│   │   │           ├── util/          # Utility classes
│   │   │           └── exception/     # Custom exceptions
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
│       └── java/
│           └── com/
│               └── example/
├── pom.xml
└── README.md
```

### 2.2 File Naming

- **Java files**: Use PascalCase (e.g., `UserController.java`)
- **Configuration files**: Use kebab-case (e.g., `application.properties`)
- **Test files**: Append `Test` to the class name (e.g., `UserServiceTest.java`)

---

## 3. Naming Conventions

### 3.1 Packages

- Use lowercase letters and dots
- Follow reverse domain name pattern (e.g., `com.example.controller`)

### 3.2 Classes

- Use PascalCase
- Use descriptive names that reflect the class's purpose
- Avoid abbreviations unless they are widely accepted

### 3.3 Methods

- Use camelCase
- Use verb-noun combinations (e.g., `getUser`, `createOrder`)
- Keep method names concise and descriptive

### 3.4 Variables

- Use camelCase
- Use descriptive names that reflect the variable's purpose
- Avoid single-letter variables except in loops and temporary contexts

### 3.5 Constants

- Use uppercase letters with underscores
- Define at the class level
- Use meaningful names (e.g., `MAX_PAGE_SIZE`)

---

## 4. Code Style

### 4.1 Indentation

- Use 4 spaces for indentation
- Avoid tabs

### 4.2 Line Length

- Keep lines under 120 characters
- Break long lines appropriately

### 4.3 Braces

- Use opening braces on the same line as the statement
- Use closing braces on a new line

### 4.4 Blank Lines

- Use blank lines to separate logical sections
- Use a single blank line between methods
- Use a blank line after class opening brace and before class closing brace

---

## 5. Best Practices

### 5.1 Error Handling

- Use try-catch blocks for exceptional cases
- Throw specific exceptions rather than generic ones
- Log exceptions appropriately

### 5.2 Security

- Validate all user input
- Use parameterized queries to prevent SQL injection
- Implement proper authentication and authorization
- Avoid hardcoding sensitive information

### 5.3 Performance

- Use appropriate data structures
- Avoid unnecessary database queries
- Implement caching where appropriate
- Optimize resource usage

### 5.4 Documentation

- Use Javadoc for classes and methods
- Document public APIs thoroughly
- Include meaningful comments for complex logic

---

## 6. Testing

### 6.1 Test Coverage

- Aim for at least 80% test coverage
- Test both positive and negative scenarios
- Test edge cases

### 6.2 Test Structure

- Organize tests by feature
- Use descriptive test method names
- Follow the Arrange-Act-Assert pattern

---

## 7. Code Review

### 7.1 Review Checklist

- [ ] Code follows naming conventions
- [ ] Code is well-documented
- [ ] Tests are present and passing
- [ ] Security best practices are followed
- [ ] Performance considerations are addressed

### 7.2 Review Process

- All code changes must be reviewed
- Use pull requests for code review
- Address all review comments before merging

---

## 8. Related Documents

- [Documentation Standards](documentation_standards.md)
- [Git Standards](git_standards.md)

---

*Last updated: 2026-02-28*