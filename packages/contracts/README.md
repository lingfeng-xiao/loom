# `@template/contracts`

Shared contracts for loom Phase 1.

Current focus:

- shell bootstrap surface used by the current workbench shell
- frozen Phase 1 REST read/write contracts for project, conversation, message, context, trace, and settings
- frozen Phase 1 SSE event contracts for conversation streaming and trace visibility

The package name is still template-shaped for now, but the exported contract surface is loom-oriented.

Source of truth:

- `docs/loom-phase1-contract-freeze.md`
- `packages/contracts/src/index.ts`
