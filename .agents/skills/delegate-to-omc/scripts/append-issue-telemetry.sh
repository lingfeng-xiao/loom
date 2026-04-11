#!/usr/bin/env bash
# Usage: ./append-issue-telemetry.sh --task-id <task-id> [--delegation-root <path>] \
#   --event-id <uuid> --workflow-id <id> --attempt <n> --phase <name> \
#   --category <category> --severity <level> --summary <text> \
#   [--evidence-file <path>] [--auto-recovered] [--follow-up-required] \
#   [--actor <name>]
#
# Categories: runner_error, claude_invocation_error, shell_quote_error,
#   artifact_sync_error, mirror_head_mismatch, preflight_failure, timeout,
#   idle_timeout, review_rejected, validation_failed, deploy_failed,
#   release_ahead_warning
#
# Severities: critical, high, medium, low, info

set -euo pipefail

usage() {
  echo "Usage: $0 --task-id <task-id> --event-id <uuid> --workflow-id <id>"
  echo "  --attempt <n> --phase <name> --category <category> --severity <level>"
  echo "  --summary <text> [--delegation-root <path>] [--evidence-file <path>]"
  echo "  [--auto-recovered] [--follow-up-required] [--actor <name>]"
  echo ""
  echo "Categories: runner_error, claude_invocation_error, shell_quote_error,"
  echo "  artifact_sync_error, mirror_head_mismatch, preflight_failure, timeout,"
  echo "  idle_timeout, review_rejected, validation_failed, deploy_failed,"
  echo "  release_ahead_warning"
  echo "Severities: critical, high, medium, low, info"
}

VALID_CATEGORIES="runner_error|claude_invocation_error|shell_quote_error|artifact_sync_error|mirror_head_mismatch|preflight_failure|timeout|idle_timeout|review_rejected|validation_failed|deploy_failed|release_ahead_warning"
VALID_SEVERITIES="critical|high|medium|low|info"

task_id=""
delegation_root=".delegations"
event_id=""
workflow_id=""
attempt=""
phase=""
category=""
severity=""
summary=""
evidence_file=""
auto_recovered="false"
follow_up_required="false"
actor=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --event-id) event_id="$2"; shift 2 ;;
    --workflow-id) workflow_id="$2"; shift 2 ;;
    --attempt) attempt="$2"; shift 2 ;;
    --phase) phase="$2"; shift 2 ;;
    --category) category="$2"; shift 2 ;;
    --severity) severity="$2"; shift 2 ;;
    --summary) summary="$2"; shift 2 ;;
    --evidence-file) evidence_file="$2"; shift 2 ;;
    --auto-recovered) auto_recovered="true"; shift 1 ;;
    --follow-up-required) follow_up_required="true"; shift 1 ;;
    --actor) actor="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || -z "$event_id" || -z "$workflow_id" || -z "$attempt" || \
      -z "$phase" || -z "$category" || -z "$severity" || -z "$summary" ]]; then
  usage
  exit 1
fi

if ! echo "$category" | grep -qxE "$VALID_CATEGORIES"; then
  echo "ERROR: invalid category '$category'" >&2
  echo "Valid categories: $(echo "$VALID_CATEGORIES" | tr '|' ' ')" >&2
  exit 1
fi

if ! echo "$severity" | grep -qxE "$VALID_SEVERITIES"; then
  echo "ERROR: invalid severity '$severity'" >&2
  echo "Valid severities: $(echo "$VALID_SEVERITIES" | tr '|' ' ')" >&2
  exit 1
fi

repo_root="$(pwd -P)"
delegation_root_abs="$(cd "$repo_root" && mkdir -p "$delegation_root" && cd "$delegation_root" && pwd -P)"
telemetry_dir="$delegation_root_abs/$task_id/telemetry"
mkdir -p "$telemetry_dir"

occurred_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Build JSON using python3 to safely escape all string values
json_line="$(python3 - "$event_id" "$workflow_id" "$task_id" "$attempt" "$phase" "$category" "$severity" "$summary" "${evidence_file:-NONE}" "$auto_recovered" "$follow_up_required" "${actor:-NONE}" "$occurred_at" <<'PYEOF'
import json
import sys

data = {
    "event_id": sys.argv[1],
    "event_type": "workflow.issue.detected",
    "workflow_id": sys.argv[2],
    "task_id": sys.argv[3],
    "attempt": int(sys.argv[4]),
    "phase": sys.argv[5],
    "category": sys.argv[6],
    "severity": sys.argv[7],
    "summary": sys.argv[8],
    "evidence_file": sys.argv[9] if sys.argv[9] != "NONE" else None,
    "auto_recovered": sys.argv[10] == "true",
    "follow_up_required": sys.argv[11] == "true",
    "actor": sys.argv[12] if sys.argv[12] != "NONE" else None,
    "occurred_at": sys.argv[13]
}

print(json.dumps(data))
PYEOF
)"

printf '%s\n' "$json_line" >> "$telemetry_dir/events.jsonl"

echo "Appended issue telemetry to $telemetry_dir/events.jsonl"
