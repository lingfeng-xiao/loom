# Frontend Baseline

The default web shell is intentionally small. It should:

- load `/api/bootstrap`
- load `/api/nodes`
- present release metadata, setup tasks, extension points, and node inventory
- degrade gracefully when the API is unavailable

## Customization Guidelines

- Replace the placeholder dashboard before shipping a product.
- Keep `/api/bootstrap` aligned with what the UI expects to render.
- Prefer plain, dependency-light patterns unless your product genuinely needs more.
- Preserve mobile readability and a fast first load.
