#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./record-delegation-event.sh --session-id <id> --event-type <type> [--task-id <id>] [--delegation-root <path>] [--phase <phase>] [--status <status>] [--evidence-file <path>] [--summary <text>] [--prompt-file <path>] [--response-file <path>] [--codex-takeover] [--worker <name>] [--worktree <path>]"
}

session_id=""
event_type=""
task_id=""
delegation_root=".delegations"
phase=""
status=""
evidence_file=""
summary=""
prompt_file=""
response_file=""
worker=""
worktree=""
codex_takeover=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --session-id) session_id="$2"; shift 2 ;;
    --event-type) event_type="$2"; shift 2 ;;
    --task-id) task_id="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --phase) phase="$2"; shift 2 ;;
    --status) status="$2"; shift 2 ;;
    --evidence-file) evidence_file="$2"; shift 2 ;;
    --summary) summary="$2"; shift 2 ;;
    --prompt-file) prompt_file="$2"; shift 2 ;;
    --response-file) response_file="$2"; shift 2 ;;
    --worker) worker="$2"; shift 2 ;;
    --worktree) worktree="$2"; shift 2 ;;
    --codex-takeover) codex_takeover=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$session_id" || -z "$event_type" ]]; then
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
mkdir -p "$session_dir"
event_file="$session_dir/events.jsonl"
occurred_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

prompt_chars=0
response_chars=0
if [[ -n "$prompt_file" && -f "$prompt_file" ]]; then
  prompt_chars="$(wc -c < "$prompt_file" | tr -d ' ')"
fi
if [[ -n "$response_file" && -f "$response_file" ]]; then
  response_chars="$(wc -c < "$response_file" | tr -d ' ')"
fi

python3 - "$event_file" "$session_id" "$event_type" "$task_id" "$phase" "$status" "$evidence_file" "$summary" "$worker" "$worktree" "$codex_takeover" "$prompt_chars" "$response_chars" "$occurred_at" <<'PY'
import json
import sys
from pathlib import Path

event_file = Path(sys.argv[1])
payload = {
    "session_id": sys.argv[2],
    "event_type": sys.argv[3],
    "task_id": sys.argv[4],
    "phase": sys.argv[5],
    "status": sys.argv[6],
    "evidence_file": sys.argv[7],
    "summary": sys.argv[8],
    "worker": sys.argv[9],
    "worktree": sys.argv[10],
    "codex_takeover": sys.argv[11] == "true",
    "prompt_chars": int(sys.argv[12] or "0"),
    "response_chars": int(sys.argv[13] or "0"),
    "estimated_prompt_tokens": int((int(sys.argv[12] or "0") + 3) / 4),
    "estimated_response_tokens": int((int(sys.argv[13] or "0") + 3) / 4),
    "occurred_at": sys.argv[14],
}
with event_file.open("a", encoding="utf-8") as fh:
    fh.write(json.dumps(payload) + "\n")
PY

echo "Recorded delegation event to $event_file"
