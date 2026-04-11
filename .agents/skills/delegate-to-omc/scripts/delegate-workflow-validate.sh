#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tests_dir="$repo_root/.agents/skills/delegate-to-omc/tests"
report_dir="${1:-$repo_root/.delegations/_workflow-validation}"
mkdir -p "$report_dir"
tmp_root="$(mktemp -d)"
pycache_root="$tmp_root/pycache"
export PYTHONPYCACHEPREFIX="$pycache_root"
trap 'rm -rf "$tmp_root"' EXIT

json_report="$report_dir/workflow-validation-report.json"
md_report="$report_dir/workflow-validation-report.md"
steps=()
failures=()

record_step() {
  local name="$1" status="$2" detail="${3:-}"
  steps+=("$name|$status|$detail")
  if [[ "$status" != "PASS" ]]; then
    failures+=("$name: $detail")
  fi
}

clean_caches() {
  find "$repo_root" \( -type d -name __pycache__ -o -type d -name .pytest_cache \) -prune -exec rm -rf {} + >/dev/null
}

run_or_record() {
  local name="$1"; shift
  local log="$tmp_root/${name//[^A-Za-z0-9_.-]/_}.log"
  if "$@" >"$log" 2>&1; then
    record_step "$name" "PASS" "$(tail -n 1 "$log" | tr -d '\r')"
  else
    record_step "$name" "FAIL" "$(tail -n 20 "$log" | tr '\n' ';' | tr -d '\r')"
  fi
}

cd "$repo_root"
clean_caches || { echo "failed to clean test caches" >&2; exit 2; }

run_or_record "static-python" python3 -m py_compile \
  "$scripts_dir/delegation_environment_gate.py" \
  "$scripts_dir/delegation_artifact_packager.py" \
  "$scripts_dir/delegation_session_runner.py" \
  "$scripts_dir/delegation_admission_gate.py" \
  "$scripts_dir/generate-delegation-user-report.py"

while IFS= read -r script; do
  run_or_record "static-bash:$(basename "$script")" bash -n "$script"
done < <(find "$scripts_dir" "$tests_dir" -type f -name '*.sh' | sort)

for test_script in \
  "$tests_dir/test-delegation-environment-gate.sh" \
  "$tests_dir/test-delegation-artifact-packager.sh" \
  "$tests_dir/test-delegation-session-runner.sh" \
  "$tests_dir/test-server-claude-invocation.sh" \
  "$tests_dir/test-delegation-admission-gate.sh" \
  "$tests_dir/test-delegation-telemetry-report.sh" \
  "$tests_dir/test-session-reflection.sh" \
  "$tests_dir/test-workflow-reporting.sh"; do
  run_or_record "test:$(basename "$test_script")" bash "$test_script"
done

if clean_caches; then
  record_step "release-preflight-cache-clean" "PASS" "test cache cleanup complete"
else
  record_step "release-preflight-cache-clean" "FAIL" "could not remove test caches"
fi

status_text="$(git status --porcelain)"
if [[ -z "$status_text" ]]; then
  record_step "release-preflight-git-clean" "PASS" "git status clean"
else
  record_step "release-preflight-git-clean" "FAIL" "$status_text"
fi

if [[ "${#failures[@]}" -eq 0 ]]; then
  overall="PASS"
else
  overall="FAIL"
fi

python3 - "$json_report" "$overall" "${steps[@]}" <<'PY'
import json
import sys
from datetime import datetime, timezone

out = sys.argv[1]
overall = sys.argv[2]
steps = []
for row in sys.argv[3:]:
    name, status, detail = (row.split("|", 2) + ["", ""])[:3]
    steps.append({"name": name, "status": status, "detail": detail})
payload = {"status": overall, "generated_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"), "steps": steps}
open(out, "w", encoding="utf-8").write(json.dumps(payload, indent=2, sort_keys=True) + "\n")
PY

{
  echo "# Delegate Workflow Validation"
  echo
  echo "- Status: \`$overall\`"
  echo
  echo "## Steps"
  echo
  for row in "${steps[@]}"; do
    IFS='|' read -r name status detail <<<"$row"
    echo "- \`$status\` $name $detail"
  done
} > "$md_report"

cat "$md_report"
[[ "$overall" == "PASS" ]]
