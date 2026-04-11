#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./summarize-delegation-session.sh --session-id <id> [--delegation-root <path>]"
}

session_id=""
delegation_root=".delegations"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --session-id) session_id="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$session_id" ]]; then
  usage
  exit 1
fi

repo_root="$(pwd -P)"
if [[ "$delegation_root" = /* ]]; then
  delegation_root_abs="$delegation_root"
else
  delegation_root_abs="$repo_root/$delegation_root"
fi
session_dir="$delegation_root_abs/_sessions/$session_id"
events_file="$session_dir/events.jsonl"
summary_json="$session_dir/delegation-session-summary.json"
summary_md="$session_dir/delegation-session-summary.md"
mkdir -p "$session_dir"
touch "$events_file"

python3 - "$events_file" "$summary_json" "$summary_md" "$session_id" <<'PY'
import json
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

events_file = Path(sys.argv[1])
summary_json = Path(sys.argv[2])
summary_md = Path(sys.argv[3])
session_id = sys.argv[4]

events = []
for line in events_file.read_text(encoding="utf-8").splitlines():
    if not line.strip():
        continue
    events.append(json.loads(line))

tasks = {event.get("task_id") for event in events if event.get("task_id")}
statuses = Counter(event.get("status", "") for event in events)
event_types = Counter(event.get("event_type", "") for event in events)
codex_takeover_count = sum(1 for event in events if event.get("codex_takeover"))
worker_success_count = statuses.get("SUCCESS", 0) + statuses.get("PASS", 0)
worker_failure_count = statuses.get("FAILED", 0) + statuses.get("NEEDS_FIX", 0) + statuses.get("BLOCKED", 0)
contract_failure_count = sum(1 for event in events if "contract" in event.get("summary", "").lower())
fix_pass_count = event_types.get("fix_pass", 0)
review_reject_count = event_types.get("review_rejected", 0) + event_types.get("review.rejected", 0)
prompt_tokens = sum(int(event.get("estimated_prompt_tokens", 0) or 0) for event in events)
response_tokens = sum(int(event.get("estimated_response_tokens", 0) or 0) for event in events)

estimated_codex_overhead_tokens = (len(events) * 80) + (codex_takeover_count * 600) + (review_reject_count * 250)
if not events:
    verdict = "uncertain"
    confidence = "low"
elif worker_failure_count > worker_success_count or codex_takeover_count > 0:
    verdict = "no"
    confidence = "medium"
elif worker_success_count > 0 and worker_failure_count == 0:
    verdict = "yes"
    confidence = "medium"
else:
    verdict = "uncertain"
    confidence = "low"

quality_gate_results = []
for task_id in sorted(tasks):
    task_events = [event for event in events if event.get("task_id") == task_id]
    task_statuses = [event.get("status", "") for event in task_events if event.get("status")]
    failed = any(status in {"FAILED", "NEEDS_FIX", "BLOCKED"} for status in task_statuses)
    passed = any(status in {"PASS", "SUCCESS"} for status in task_statuses)
    quality_gate_results.append({
        "task_id": task_id,
        "verdict": "failed" if failed else ("passed" if passed else "unknown"),
        "evidence": [event.get("evidence_file", "") for event in task_events if event.get("evidence_file")],
    })

root_cause_hypotheses = []
if contract_failure_count:
    root_cause_hypotheses.append({
        "hypothesis": "Worker output did not satisfy the delegation contract.",
        "evidence": [event.get("summary", "") for event in events if "contract" in event.get("summary", "").lower()],
        "counter_evidence": ["At least one worker passed review."] if worker_success_count else [],
        "confidence": "medium",
        "repairability": "prompt_or_review_gate",
    })
if review_reject_count:
    root_cause_hypotheses.append({
        "hypothesis": "Task quality required Codex review intervention.",
        "evidence": [event.get("summary", "") for event in events if event.get("event_type") in {"review_rejected", "review.rejected"}],
        "counter_evidence": ["Rejection was captured before release, so quality gate worked."],
        "confidence": "medium",
        "repairability": "prompt_task_splitting_or_worker_selection",
    })
if codex_takeover_count:
    root_cause_hypotheses.append({
        "hypothesis": "Claude work increased Codex overhead enough to threaten the token-saving goal.",
        "evidence": ["Codex takeover was recorded."],
        "counter_evidence": ["Token values are proxy estimates unless real usage is available."],
        "confidence": "medium",
        "repairability": "process_stop_loss_or_resplit",
    })
if not root_cause_hypotheses:
    root_cause_hypotheses.append({
        "hypothesis": "No failure pattern was detected from recorded events.",
        "evidence": ["No failure, review reject, contract failure, or Codex takeover event was recorded."],
        "counter_evidence": ["Missing instrumentation could hide failures."],
        "confidence": "low" if not events else "medium",
        "repairability": "monitoring",
    })

candidate_lessons = []
if verdict == "no":
    candidate_lessons.append({
        "candidate_lesson": "Reduce delegation breadth or stop retrying when Codex takeover is required.",
        "confidence": "medium",
        "review_after": "next delegation session",
    })
elif verdict == "yes":
    candidate_lessons.append({
        "candidate_lesson": "The current task size and gate strictness look compatible with token-saving delegation.",
        "confidence": "medium",
        "review_after": "after three similar sessions",
    })
else:
    candidate_lessons.append({
        "candidate_lesson": "Improve usage instrumentation before changing delegation policy.",
        "confidence": "low",
        "review_after": "after real usage or richer proxy metrics are available",
    })

payload = {
    "session_id": session_id,
    "delegated_task_count": len(tasks),
    "worker_success_count": worker_success_count,
    "worker_failure_count": worker_failure_count,
    "contract_failure_count": contract_failure_count,
    "codex_takeover_count": codex_takeover_count,
    "fix_pass_count": fix_pass_count,
    "review_reject_count": review_reject_count,
    "elapsed_time_by_phase": {},
    "estimated_claude_prompt_tokens": prompt_tokens,
    "estimated_claude_response_tokens": response_tokens,
    "estimated_codex_overhead_tokens": estimated_codex_overhead_tokens,
    "token_savings_verdict": verdict,
    "confidence": confidence,
    "events_by_type": dict(event_types),
    "statuses": dict(statuses),
    "quality_gate_results": quality_gate_results,
    "root_cause_hypotheses": root_cause_hypotheses,
    "candidate_lessons": candidate_lessons,
    "token_metrics_source": "estimate",
    "generated_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
}
summary_json.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

md = [
    "# Delegation Session Summary",
    "",
    f"- Session ID: `{session_id}`",
    f"- Delegated tasks: `{payload['delegated_task_count']}`",
    f"- Worker successes: `{worker_success_count}`",
    f"- Worker failures: `{worker_failure_count}`",
    f"- Codex takeovers: `{codex_takeover_count}`",
    f"- Estimated Claude prompt tokens: `{prompt_tokens}`",
    f"- Estimated Claude response tokens: `{response_tokens}`",
    f"- Estimated Codex overhead tokens: `{estimated_codex_overhead_tokens}`",
    f"- Token savings verdict: `{verdict}`",
    f"- Confidence: `{confidence}`",
    "",
    "## Reflection",
    "",
    "This summary is generated after task execution from recorded evidence. Treat token values as estimates unless upstream usage data is available.",
    "",
    "## Root Cause Hypotheses",
    "",
]
for item in root_cause_hypotheses:
    md.extend([
        f"- Hypothesis: {item['hypothesis']}",
        f"- Confidence: `{item['confidence']}`",
        f"- Repairability: `{item['repairability']}`",
        f"- Evidence: {'; '.join(item['evidence']) if item['evidence'] else 'None recorded'}",
        f"- Counter-evidence: {'; '.join(item['counter_evidence']) if item['counter_evidence'] else 'None recorded'}",
        "",
    ])
md.extend([
    "## Candidate Lessons",
    "",
])
for item in candidate_lessons:
    md.extend([
        f"- Candidate lesson: {item['candidate_lesson']}",
        f"- Confidence: `{item['confidence']}`",
        f"- Review after: {item['review_after']}",
        "",
    ])
summary_md.write_text("\n".join(md) + "\n", encoding="utf-8")
PY

cat "$summary_md"
