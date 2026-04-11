#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

make_repo() {
  local repo="$1"
  git init -q -b main "$repo"
  git -C "$repo" config user.email "delegation-test@example.com"
  git -C "$repo" config user.name "Delegation Test"
  printf '# Probe\n' > "$repo/README.md"
  git -C "$repo" add README.md
  git -C "$repo" commit -q -m 'init'
}

make_brief() {
  local brief="$1"
  cat > "$brief" <<'BRIEF'
# Task Brief

- Task ID: `stdin-probe`

## Goal
Create `worker-output.txt` in the assigned worktree.

## Done when
- `worker-output.txt` exists.

## Validation
- Fake validation is reported.
BRIEF
}

repo="$tmp_root/repo"
mkdir -p "$repo"
make_repo "$repo"
brief="$tmp_root/brief.md"
make_brief "$brief"
export HOME="$tmp_root/home"
fake_bin="$HOME/.npm-global/bin"
mkdir -p "$fake_bin"

cat > "$fake_bin/claude" <<'FAKE'
#!/usr/bin/env bash
prompt="$(cat)"
if grep -q 'CLAUDE_P_OK' <<<"$prompt"; then
  printf 'CLAUDE_P_OK\n'
  exit 0
fi
printf '%s\n' "$prompt" > "$FAKE_CLAUDE_STDIN_CAPTURE"
printf '%s\n' "$*" > "$FAKE_CLAUDE_ARGS_CAPTURE"
printf 'worker touched file\n' >> worker-output.txt
cat <<'CONTRACT'
RESULT: SUCCESS
SUMMARY: fake worker created the probe file
CHANGED_FILES: worker-output.txt
TESTS_RUN: fake validation
RISKS: None
BLOCKERS: None
NEXT_ACTIONS: None
CONTRACT
FAKE
chmod +x "$fake_bin/claude"

export PATH="$fake_bin:$PATH"
export FAKE_CLAUDE_STDIN_CAPTURE="$tmp_root/stdin-capture.txt"
export FAKE_CLAUDE_ARGS_CAPTURE="$tmp_root/args-capture.txt"

bash "$scripts_dir/server-delegate-to-claude.sh" \
  --task-id stdin-probe \
  --task-file "$brief" \
  --repo-root "$repo" \
  --base-ref main \
  --worktree-root "$tmp_root/worktrees" \
  --delegation-root "$tmp_root/delegations" \
  --timeout-seconds 30 \
  --idle-timeout-seconds 10 > "$tmp_root/stdin-run.log"

grep -q 'Task Brief' "$FAKE_CLAUDE_STDIN_CAPTURE"
grep -q 'Execution Handoff' "$FAKE_CLAUDE_STDIN_CAPTURE"
grep -q 'assigned worktree above is authoritative' "$FAKE_CLAUDE_STDIN_CAPTURE"
grep -q 'Use shell commands for repository inspection and edits' "$FAKE_CLAUDE_STDIN_CAPTURE"
grep -q -- '--tools Bash' "$FAKE_CLAUDE_ARGS_CAPTURE"
grep -q 'worker_status.*SUCCESS' "$tmp_root/delegations/stdin-probe/result.json"
grep -q 'worker-output.txt' "$tmp_root/delegations/stdin-probe/git.status.txt"

cat > "$fake_bin/claude" <<'FAKE'
#!/usr/bin/env bash
prompt="$(cat)"
if grep -q 'CLAUDE_P_OK' <<<"$prompt"; then
  printf 'CLAUDE_P_OK\n'
  exit 0
fi
printf '%s\n' "$*" > "$FAKE_CLAUDE_ARGS_CAPTURE"
printf 'Execution error\n'
exit 0
FAKE
chmod +x "$fake_bin/claude"

bash "$scripts_dir/server-delegate-to-claude.sh" \
  --task-id execution-error-probe \
  --task-file "$brief" \
  --repo-root "$repo" \
  --base-ref main \
  --worktree-root "$tmp_root/worktrees" \
  --delegation-root "$tmp_root/delegations" \
  --timeout-seconds 30 \
  --idle-timeout-seconds 10 > "$tmp_root/execution-error-run.log"

python3 - <<'PY' "$tmp_root/delegations/execution-error-probe/result.json"
import json
import sys
payload = json.load(open(sys.argv[1], encoding="utf-8"))
assert payload["worker_status"] == "FAILED", payload
assert payload["claude_invocation_failed"] is True, payload
assert payload["exit_code"] == 1, payload
PY

echo "server Claude invocation tests passed"
