#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./generate-workflow-report.sh --task-id <task-id> [--delegation-root <path>] [--release-id <id>] [--rollback-ref <ref>]"
}

task_id=""
delegation_root=".delegations"
release_id=""
rollback_ref=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --release-id) release_id="$2"; shift 2 ;;
    --rollback-ref) rollback_ref="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" ]]; then
  usage
  exit 1
fi

task_dir="$(cd "$delegation_root" && pwd -P)/$task_id"
python3 - "$task_dir" "$task_id" "$release_id" "$rollback_ref" <<'PY'
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

task_dir = Path(sys.argv[1])
task_id = sys.argv[2]
release_id = sys.argv[3]
rollback_ref = sys.argv[4]

def read_json(name, default):
    path = task_dir / name
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        return {"_read_error": str(exc)}

def read_text(name):
    path = task_dir / name
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="replace")

def review_result_from_notes(text):
    match = re.search(r"^REVIEW_RESULT:\s*(\S+)", text, re.MULTILINE)
    return match.group(1).strip().upper() if match else "UNKNOWN"

result = read_json("result.json", {})
preflight = read_json("preflight.json", {})
review_data = read_json("review-result.json", {})
closeout = read_json("closeout.json", {})
triage = read_json("failure-triage.json", {})
review_notes = read_text("review-notes.md")

review_result = str(closeout.get("review_result") or review_data.get("review_result") or review_result_from_notes(review_notes))
worker_status = str(closeout.get("worker_status") or result.get("worker_status") or "UNKNOWN")
preflight_status = str(closeout.get("preflight_status") or preflight.get("status") or "UNKNOWN")
closeable = bool(closeout.get("closeable", review_result == "PASS" and worker_status == "SUCCESS" and preflight_status == "PASS"))
closed = bool(closeout.get("closed", closeable))
final_state = str(closeout.get("final_state") or ("CLOSED" if closeable else "BLOCKED"))

issues = []
for issue in review_data.get("issues", []) or []:
    issues.append(str(issue))
if preflight_status != "PASS":
    reason = preflight.get("failure_reason") or "preflight did not pass"
    issues.append(f"Preflight status is {preflight_status}: {reason}")
if worker_status != "SUCCESS":
    issues.append(f"Worker status is {worker_status}.")
if review_result != "PASS":
    issues.append(f"Review result is {review_result}.")
if result.get("timed_out"):
    issues.append("Worker hit overall timeout.")
if result.get("idle_timed_out"):
    issues.append("Worker hit idle-output timeout.")

expectation_mismatches = []
if result.get("declared_result") == "SUCCESS" and worker_status != "SUCCESS":
    expectation_mismatches.append("Worker declared SUCCESS but closeout checks did not accept it.")
if result.get("contract_complete") is False:
    expectation_mismatches.append("Worker result contract was incomplete.")
if result.get("diff_present") is False:
    expectation_mismatches.append("No real diff was detected.")
if result.get("validation_reported") is False:
    expectation_mismatches.append("Validation was not meaningfully reported.")

next_actions = review_data.get("recommended_corrections") or review_data.get("minimal_fix_list") or []
if not next_actions and not closeable:
    next_actions = ["Fix the blocking issues and rerun closeout."]
if closeable:
    next_actions = ["No follow-up required."]

evidence_files = {
    "brief": str(task_dir / "brief.md"),
    "result": str(task_dir / "result.json"),
    "preflight": str(task_dir / "preflight.json"),
    "review_notes": str(task_dir / "review-notes.md"),
    "review_result": str(task_dir / "review-result.json"),
    "closeout": str(task_dir / "closeout.json"),
    "workflow_report_json": str(task_dir / "workflow-report.json"),
    "workflow_report_md": str(task_dir / "workflow-report.md"),
}

payload = {
    "task_id": task_id,
    "final_state": final_state,
    "closed": closed,
    "closeable": closeable,
    "review_result": review_result,
    "worker_status": worker_status,
    "preflight_status": preflight_status,
    "release_id": release_id,
    "rollback_ref": rollback_ref,
    "issues": sorted(set(issues)),
    "expectation_mismatches": expectation_mismatches,
    "auto_recovered": len(issues) > 0 and closeable,
    "residual_risk": "None recorded." if closeable else "Closeout is blocked; inspect issues and evidence.",
    "next_actions": [str(item) for item in next_actions],
    "failure_triage": triage,
    "repair_size": triage.get("repair_size", ""),
    "recommended_action": triage.get("recommended_action", ""),
    "evidence_files": evidence_files,
    "generated_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
}

(task_dir / "workflow-report.json").write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

def bullet(items):
    if not items:
        return "- None\n"
    return "".join(f"- {item}\n" for item in items)

md = [
    "# Workflow Report",
    "",
    f"- Task ID: `{task_id}`",
    f"- Final state: `{final_state}`",
    f"- Closed: `{str(closed).lower()}`",
    f"- Review result: `{review_result}`",
    f"- Worker status: `{worker_status}`",
    f"- Preflight status: `{preflight_status}`",
    f"- Release ID: `{release_id}`",
    f"- Rollback ref: `{rollback_ref}`",
    "",
    "## Issues",
    "",
    bullet(payload["issues"]).rstrip(),
    "",
    "## Expectation Mismatches",
    "",
    bullet(expectation_mismatches).rstrip(),
    "",
    "## Residual Risk",
    "",
    payload["residual_risk"],
    "",
    "## Next Actions",
    "",
    bullet(payload["next_actions"]).rstrip(),
    "",
    "## Failure Triage",
    "",
    f"- Repair size: `{payload['repair_size']}`",
    f"- Recommended action: `{payload['recommended_action']}`",
    f"- Confidence: `{triage.get('confidence', '')}`",
    "",
    "## Evidence",
    "",
    bullet([f"{key}: `{value}`" for key, value in evidence_files.items()]).rstrip(),
    "",
]
(task_dir / "workflow-report.md").write_text("\n".join(md) + "\n", encoding="utf-8")
PY

echo "Workflow report written to $task_dir/workflow-report.md and $task_dir/workflow-report.json"
