#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT
export PYTHONPYCACHEPREFIX="$tmp_root/pycache"

python3 -m py_compile "$scripts_dir/delegation_artifact_packager.py"

work="$tmp_root/repo with spaces"
mkdir -p "$work/briefs" "$work/prompts"
cat > "$work/briefs/task brief.md" <<'MD'
Task ID: a
Goal: package artifacts
MD
cat > "$work/prompts/prompt input.txt" <<'TXT'
hello
TXT
brief_hash="$(sha256sum "$work/briefs/task brief.md" | awk '{print $1}')"
cat > "$work/session-manifest.json" <<JSON
{
  "session_id": "pkg-test",
  "brief_path": "briefs/task brief.md",
  "prompt_input_path": "prompts/prompt input.txt",
  "expected_checksums": {"briefs/task brief.md": "$brief_hash"},
  "tasks": [{"task_id":"a","brief_path":"briefs/task brief.md","relevant_files":["x"],"done_when":["done"],"validation":["test"]}]
}
JSON
python3 "$scripts_dir/delegation_artifact_packager.py" --repo-root "$work" --manifest "$work/session-manifest.json" --delegation-root "$work/.delegations" >/dev/null
python3 -m json.tool "$work/.delegations/pkg-test/package-result.json" >/dev/null
grep -q '"status": "PASS"' "$work/.delegations/pkg-test/package-result.json"
test -x "$work/.delegations/pkg-test/uploaded-runner.sh"

printf '\357\273\277bad\n' > "$work/briefs/bom.md"
cat > "$work/bad-manifest.json" <<'JSON'
{"session_id":"bad","brief_path":"briefs/bom.md","tasks":[{"task_id":"a"}]}
JSON
set +e
python3 "$scripts_dir/delegation_artifact_packager.py" --repo-root "$work" --manifest "$work/bad-manifest.json" --delegation-root "$work/.delegations" >/dev/null 2>&1
rc=$?
set -e
if [[ "$rc" -eq 0 ]]; then
  echo "BOM package unexpectedly passed" >&2
  exit 1
fi
grep -q 'BOM' "$work/.delegations/bad/package-result.json"

echo "delegation artifact packager tests passed"
