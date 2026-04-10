# OMC Mode Selection

Use remote `claude -p` when one server worker can finish the task without coordination.

Use `omc team` when all of these are true:

- the work naturally splits into two or more subtasks
- each subtask can stay inside its own remote worktree
- the subtasks do not need to co-edit the same files
- Codex can review each result independently and then close the parent task

Do not force `omc team` onto tightly coupled work. If two remote workers would have to negotiate ownership of the same file set, keep it as one Claude task or keep it in Codex.
