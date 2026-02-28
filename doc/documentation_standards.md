# Documentation Standards

This document defines the standards for all documentation files in the `doc` folder.

---

## 1. File Naming Conventions

### 1.1 General Rules
- All file names must be in **English**
- Use **lowercase letters**, numbers, and hyphens (`-`) only
- Use **snake_case** for multi-word file names
- File extensions: `.md` for Markdown documents

### 1.2 Naming Examples
| Incorrect | Correct |
|-----------|---------|
| `git规范.md` | `git_standards.md` |
| `API文档.md` | `api_documentation.md` |
| `开发指南.md` | `development_guide.md` |
| `README.zh.md` | `readme_zh.md` |

### 1.3 Standard File Names
- `readme.md` - Main documentation entry point
- `contributing.md` - Contribution guidelines
- `changelog.md` - Version change history
- `documentation_standards.md` - This file

---

## 2. Document Structure

### 2.1 Header Format
Every document must start with a level 1 heading:
```markdown
# Document Title
```

### 2.2 Section Hierarchy
Use consistent heading levels:
- `#` - Document title (only one per file)
- `##` - Main sections
- `###` - Subsections
- `####` - Detailed subsections

### 2.3 Required Sections
All technical documents should include:
1. **Overview** - Brief description of the document's purpose
2. **Content Sections** - Organized by topic
3. **References** - Links to related documents (if applicable)

---

## 3. Writing Style

### 3.1 Language
- Primary language: **English**
- Use clear and concise language
- Avoid jargon; explain technical terms when necessary

### 3.2 Formatting
- Use **bold** for emphasis on important terms
- Use `code blocks` for commands, file names, and code snippets
- Use bullet points for lists
- Use numbered lists for sequential steps

### 3.3 Code Blocks
Specify the language for syntax highlighting:
```markdown
```bash
# Shell commands
```

```python
# Python code
```
```

---

## 4. File Organization

### 4.1 Directory Structure
```
doc/
├── readme.md                    # Main documentation
├── documentation_standards.md   # This standards document
├── architecture.md              # System architecture
├── api/
│   ├── readme.md
│   └── endpoints.md
├── guides/
│   ├── setup.md
│   └── deployment.md
└── standards/
    ├── git_standards.md
    └── coding_standards.md
```

### 4.2 Folder Naming
- Use **lowercase** for category folders (e.g., `api/`, `guides/`)
- Use descriptive, single-word names when possible

---

## 5. Version Control

### 5.1 Document Updates
- Update the document date when making significant changes
- Use Git commit messages following the project's commit standards
- Major changes should be reviewed via Pull Request

### 5.2 Document Metadata
Optional header for complex documents:
```markdown
---
last_updated: 2026-02-28
author: Your Name
version: 1.0
---
```

---

## 6. Review Checklist

Before submitting documentation changes:
- [ ] File name follows English naming convention
- [ ] Document has a clear title and structure
- [ ] Content is accurate and up-to-date
- [ ] Code examples are tested and working
- [ ] Links are valid and accessible
- [ ] No spelling or grammar errors

---

## 7. Related Documents

- [Git Standards](git_standards.md)
- [Main README](README.md)

---

*Last updated: 2026-02-28*
