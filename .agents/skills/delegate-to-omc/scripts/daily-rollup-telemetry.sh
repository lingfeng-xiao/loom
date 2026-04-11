#!/usr/bin/env bash
# Usage: ./daily-rollup-telemetry.sh [--delegation-root <path>] \
#   [--observability-root <path>] [--date <YYYY-MM-DD>]
#
# Reads all .delegations/<task-id>/telemetry/events.jsonl files and produces
# a daily rollup at .workflow-observability/daily/YYYY-MM-DD.json

set -euo pipefail

usage() {
  echo "Usage: $0 [--delegation-root <path>] [--observability-root <path>] [--date <YYYY-MM-DD>]"
}

delegation_root=".delegations"
observability_root=".workflow-observability"
rollup_date="$(date -u +%Y-%m-%d)"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --observability-root) observability_root="$2"; shift 2 ;;
    --date) rollup_date="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

repo_root="$(pwd -P)"
if [[ "$delegation_root" = /* ]]; then
  delegation_root_abs="$delegation_root"
else
  delegation_root_abs="$repo_root/$delegation_root"
fi

if [[ "$observability_root" = /* ]]; then
  observability_dir="$observability_root/daily"
else
  observability_dir="$repo_root/$observability_root/daily"
fi

mkdir -p "$observability_dir"
rollup_file="$observability_dir/${rollup_date}.json"

# Use Python to parse JSONL and produce rollup
python3 - "$delegation_root_abs" "$rollup_date" "$rollup_file" <<'PYEOF'
import json
import sys
from collections import defaultdict
from datetime import datetime
import os
import glob

delegation_root = sys.argv[1] if len(sys.argv) > 1 else ".delegations"
rollup_date = sys.argv[2] if len(sys.argv) > 2 else datetime.utcnow().strftime("%Y-%m-%d")
rollup_file = sys.argv[3] if len(sys.argv) > 3 else "/dev/stdout"

issue_total_by_category = defaultdict(int)
issue_total_by_severity = defaultdict(int)
auto_recovered_total = 0
follow_up_required_total = 0
issue_summaries = []

pattern = os.path.join(delegation_root, "*/telemetry/events.jsonl")
for filepath in glob.glob(pattern):
    if not os.path.isfile(filepath):
        continue
    try:
        with open(filepath, "r") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    event = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if event.get("event_type") != "workflow.issue.detected":
                    continue
                occurred = event.get("occurred_at", "")
                if not occurred.startswith(rollup_date):
                    continue

                category = event.get("category", "unknown")
                severity = event.get("severity", "unknown")
                issue_total_by_category[category] += 1
                issue_total_by_severity[severity] += 1

                if event.get("auto_recovered"):
                    auto_recovered_total += 1
                if event.get("follow_up_required"):
                    follow_up_required_total += 1

                issue_summaries.append({
                    "task_id": event.get("task_id", ""),
                    "category": category,
                    "severity": severity,
                    "summary": event.get("summary", ""),
                    "occurred_at": occurred
                })
    except (IOError, OSError):
        continue

severity_order = {"critical": 0, "high": 1, "medium": 2, "low": 3, "info": 4}
sorted_summaries = sorted(
    issue_summaries,
    key=lambda x: (severity_order.get(x["severity"], 5), x["category"])
)

rollup = {
    "date": rollup_date,
    "issue_total_by_category": dict(issue_total_by_category),
    "issue_total_by_severity": dict(issue_total_by_severity),
    "auto_recovered_total": auto_recovered_total,
    "follow_up_required_total": follow_up_required_total,
    "top_issue_summaries": sorted_summaries[:50]
}

with open(rollup_file, "w") as out:
    json.dump(rollup, out, indent=2)
    out.write("\n")
PYEOF

echo "Wrote rollup to $rollup_file"
