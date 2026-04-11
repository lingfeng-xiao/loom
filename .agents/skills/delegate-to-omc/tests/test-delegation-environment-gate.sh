#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT
export PYTHONPYCACHEPREFIX="$tmp_root/pycache"

python3 -m py_compile "$scripts_dir/delegation_environment_gate.py"

source_repo="$tmp_root/source repo"
mirror_repo="$tmp_root/mirror repo"
git init -q "$source_repo"
cd "$source_repo"
git config user.email test@example.invalid
git config user.name Test
mkdir -p .agents/skills/delegate-to-omc/scripts .delegations/locks/worktrees
cat > .agents/skills/delegate-to-omc/scripts/delegation_session_runner.py <<'PY'
RUNNER_VERSION = "2.0"
PY
git add .
git commit -q -m init
git branch workflow-runner-baseline
cat > file.txt <<'TXT'
new
TXT
git add file.txt
git commit -q -m newer
mkdir -p .agents/skills/delegate-to-omc/scripts/__pycache__ .pytest_cache
touch .delegations/locks/release.lock ".delegations/locks/worktrees/source repo.lock"

git clone -q "$source_repo" "$mirror_repo"
cd "$mirror_repo"
git checkout -q HEAD~1

gate_dir="$tmp_root/gate out"
set +e
python3 "$scripts_dir/delegation_environment_gate.py" \
  --repo-root "$source_repo" \
  --output-dir "$gate_dir" \
  --worktree-root "$source_repo" \
  --locks-root "$source_repo/.delegations/locks" \
  --local-mirror-root "$mirror_repo" \
  --claude-smoke mock >"$tmp_root/out.txt" 2>"$tmp_root/err.txt"
rc=$?
set -e
if [[ "$rc" -eq 0 ]]; then
  echo "environment gate unexpectedly passed" >&2
  cat "$gate_dir/env-gate.json" >&2
  exit 1
fi
python3 -m json.tool "$gate_dir/env-gate.json" >/dev/null
grep -q 'baseline_stale' "$gate_dir/env-gate.json"
grep -q 'mirror_mismatch' "$gate_dir/env-gate.json"
grep -q 'release_lock' "$gate_dir/env-gate.json"
grep -q 'test_artifact_pollution' "$gate_dir/env-gate.json"
test ! -d "$source_repo/.pytest_cache"
test ! -d "$source_repo/.agents/skills/delegate-to-omc/scripts/__pycache__"

echo "delegation environment gate tests passed"
