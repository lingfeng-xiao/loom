# Frontend Coding Standards

This document defines the coding standards for all frontend code in the project.

---

## 1. Overview

This document establishes the coding standards and best practices for frontend development in the project. It covers file structure, naming conventions, code style, and other guidelines to ensure consistency and maintainability across the codebase.

---

## 2. File Structure

### 2.1 Directory Organization

```
frontend/
├── src/
│   ├── api/           # API service modules
│   ├── assets/        # Static assets (images, fonts, etc.)
│   ├── components/    # Reusable components
│   ├── router/        # Vue Router configuration
│   ├── store/         # State management (Pinia/Vuex)
│   ├── utils/         # Utility functions
│   ├── views/         # Page components
│   ├── App.vue        # Root component
│   └── main.js        # Entry point
├── public/            # Public static files
├── index.html         # HTML template
├── package.json       # Project configuration
└── vite.config.js     # Vite configuration
```

### 2.2 File Naming Conventions

- **Component files**: Use PascalCase
  - Example: `UserProfile.vue`, `LoginForm.vue`

- **JavaScript/TypeScript files**: Use camelCase
  - Example: `apiService.js`, `utils.js`

- **View files**: Use PascalCase
  - Example: `HomePage.vue`, `Dashboard.vue`

- **Asset files**: Use kebab-case
  - Example: `background-image.jpg`, `logo-icon.svg`

---

## 3. Code Style

### 3.1 JavaScript/TypeScript

- **Indentation**: 2 spaces
- **Semicolons**: Use semicolons at the end of statements
- **Quotes**: Use single quotes for strings
- **Braces**: Opening brace on the same line
- **Variables**: Use `const` for constants, `let` for variables
- **Arrow functions**: Use arrow functions when possible
- **Template literals**: Use backticks for multi-line strings

```javascript
// Good
const handleClick = () => {
  console.log('Button clicked');
};

// Bad
function handleClick() {
  console.log("Button clicked")
}
```

### 3.2 Vue Components

- **Script setup**: Use `<script setup>` for component composition
- **Props**: Define props with type validation
- **Emits**: Define custom events explicitly
- **Computed properties**: Use computed for derived state
- **Watchers**: Use watchers sparingly

```vue
<template>
  <div class="user-profile">
    <h2>{{ userName }}</h2>
    <button @click="handleUpdate">Update Profile</button>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['update']);

const userName = computed(() => props.user.name);

const handleUpdate = () => {
  emit('update');
};
</script>

<style scoped>
.user-profile {
  padding: 20px;
  border: 1px solid #ccc;
}
</style>
```

### 3.3 CSS/SCSS

- **Naming convention**: Use BEM (Block, Element, Modifier)
- **Indentation**: 2 spaces
- **Selector specificity**: Keep selectors simple
- **Variables**: Use CSS variables or SCSS variables for consistent styling
- **Responsive design**: Use media queries for different screen sizes

```css
/* Good */
.user-profile {
  padding: 20px;
  
  &__name {
    font-size: 18px;
  }
  
  &--active {
    border-color: #007bff;
  }
}

/* Bad */
#userProfile .name {
  font-size: 18px;
}
```

---

## 4. Naming Conventions

### 4.1 Variables

- **CamelCase** for variables and functions
- **UPPER_SNAKE_CASE** for constants
- **Descriptive names** that explain the purpose

```javascript
// Good
const userName = 'John Doe';
const MAX_RETRY_COUNT = 3;

// Bad
const un = 'John Doe';
const mrc = 3;
```

### 4.2 Components

- **PascalCase** for component names
- **Descriptive names** that reflect the component's purpose
- **Avoid abbreviations** unless they are widely understood

```vue
<!-- Good -->
<template>
  <UserProfile :user="currentUser" />
</template>

<!-- Bad -->
<template>
  <UP :u="currentUser" />
</template>
```

### 4.3 Props and Emits

- **camelCase** for prop names
- **kebab-case** for HTML attributes
- **Descriptive names** for events

```vue
<!-- Good -->
<template>
  <UserProfile :user-name="currentUser.name" @profile-updated="handleProfileUpdate" />
</template>

<!-- Bad -->
<template>
  <UserProfile :un="currentUser.name" @upd="handleProfileUpdate" />
</template>
```

---

## 5. Best Practices

### 5.1 Performance

- **Lazy loading**: Use dynamic imports for components
- **Code splitting**: Split code by route
- **Memoization**: Use memo for expensive computations
- **Debouncing**: Use debounce for search inputs
- **Throttling**: Use throttle for scroll events

### 5.2 Security

- **XSS protection**: Use v-html sparingly and sanitize input
- **CSRF protection**: Follow best practices for API calls
- **Input validation**: Validate user input on both client and server
- **Sensitive data**: Never store sensitive data in localStorage

### 5.3 Accessibility

- **Semantic HTML**: Use proper HTML elements
- **ARIA attributes**: Use ARIA attributes when needed
- **Keyboard navigation**: Ensure all interactive elements are keyboard accessible
- **Screen reader support**: Test with screen readers
- **Color contrast**: Ensure sufficient color contrast

### 5.4 Testing

- **Unit tests**: Test individual components and functions
- **Integration tests**: Test component interactions
- **End-to-end tests**: Test complete user flows
- **Test coverage**: Aim for high test coverage

---

## 6. Linting and Formatting

### 6.1 ESLint

- Use ESLint for code quality
- Follow the project's ESLint configuration
- Run ESLint before committing code

### 6.2 Prettier

- Use Prettier for code formatting
- Follow the project's Prettier configuration
- Run Prettier before committing code

### 6.3 Editor Configuration

- Use .editorconfig for consistent editor settings
- Configure IDE/editor to use project's linting and formatting rules

---

## 7. Version Control

### 7.1 Git Commit Messages

- **Conventional commits**: Use conventional commit format
- **Descriptive messages**: Explain what and why, not how
- **Subject line**: Keep subject line under 50 characters
- **Body**: Use body for more detailed explanations

### 7.2 Branching Strategy

- **Main branch**: Production-ready code
- **Develop branch**: Integration branch for features
- **Feature branches**: For new features and bug fixes
- **Hotfix branches**: For critical production fixes

---

## 8. References

- [Vue.js Style Guide](https://vuejs.org/style-guide/)
- [JavaScript Standard Style](https://standardjs.com/)
- [BEM Methodology](http://getbem.com/)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

*Last updated: 2026-02-28*