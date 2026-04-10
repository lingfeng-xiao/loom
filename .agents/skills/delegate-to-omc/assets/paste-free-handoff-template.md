# Execution Handoff

You are the execution worker for delegation task `{{TASK_ID}}`.

## Working rules

- Work only inside the assigned remote git worktree: `{{WORKTREE}}`
- Repo root for this run on the server: `{{REPO_ROOT}}`
- Source brief: `{{TASK_FILE}}`
- Keep the diff small and local to the brief
- Do not modify unrelated files
- Make real filesystem edits when the brief asks for changes; do not simulate completion
- Run the listed validation when possible
- Before returning `SUCCESS`, verify that the expected files exist and that the git diff reflects the work
- If you cannot make the edit or cannot verify it, return `FAILED` or `PARTIAL`
- If validation cannot run, explain exactly why
- Return the required structured result contract at the end

## Required output contract

```text
RESULT: SUCCESS | PARTIAL | FAILED
SUMMARY:
CHANGED_FILES:
TESTS_RUN:
RISKS:
BLOCKERS:
NEXT_ACTIONS:
```

## Brief

Read and follow the brief below exactly.
