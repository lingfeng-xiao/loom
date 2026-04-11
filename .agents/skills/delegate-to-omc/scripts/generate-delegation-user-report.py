#!/usr/bin/env python3
"""Generate a user-visible delegation report from v2 session artifacts."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def read_json(path: Path, default: Any | None = None) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def read_events(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def bullets(items: list[str]) -> list[str]:
    return [f"- {item}" for item in items] if items else ["- None recorded"]


def generate_report(session_root: Path, output: Path) -> None:
    state = read_json(session_root / "session-state.json", {})
    summary = read_json(session_root / "delegation-session-summary.json", {})
    gate = read_json(session_root / "gate-summary.json", {})
    events = read_events(session_root / "session-events.jsonl")
    tasks = state.get("tasks", {})
    failed_tasks = [task_id for task_id, task in tasks.items() if task.get("state") not in {"closed", "validated", "gate_passed"}]
    hypotheses = summary.get("root_cause_hypotheses", [])
    recommendations = summary.get("candidate_lessons", [])
    lines = [
        "# Delegation User Report",
        "",
        f"- Session ID: `{state.get('session_id', summary.get('session_id', 'unknown'))}`",
        f"- Execution result: `{state.get('status', 'unknown')}`",
        f"- Quality gate: `{gate.get('quality_gate_result', 'unknown')}`",
        f"- Token savings verdict: `{summary.get('token_savings_verdict', 'uncertain')}`",
        f"- Token metric source: `{summary.get('token_metrics_source', 'estimate')}`",
        f"- Events recorded: `{len(events)}`",
        "",
        "## Deviations",
        "",
    ]
    lines.extend(bullets([gate.get("compact_summary", "")] if gate.get("compact_summary") else []))
    lines.extend(["", "## Failed Or Unclosed Tasks", ""])
    lines.extend(bullets(failed_tasks))
    lines.extend(["", "## Root Cause Hypotheses", ""])
    if hypotheses:
        for item in hypotheses:
            lines.append(f"- {item.get('hypothesis', 'unknown')} Confidence: `{item.get('confidence', 'unknown')}` Repairability: `{item.get('repairability', 'unknown')}`")
    else:
        lines.append("- No root-cause hypotheses recorded.")
    lines.extend(["", "## Next Recommendations", ""])
    if recommendations:
        for item in recommendations:
            lines.append(f"- {item.get('candidate_lesson', 'Review the recorded evidence before changing workflow rules.')}")
    else:
        lines.append("- Review gate summaries and session events before the next delegation.")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--session-root", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    output = args.output or (args.session_root / "user-report.md")
    generate_report(args.session_root, output)
    print(f"User report written to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
