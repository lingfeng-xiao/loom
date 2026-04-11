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
    return [f"- {item}" for item in items if item] or ["- None recorded"]


def task_gate_lines(session_root: Path, state: dict[str, Any]) -> list[str]:
    lines: list[str] = []
    tasks = state.get("tasks", {}) if isinstance(state.get("tasks"), dict) else {}
    for task_id, task in sorted(tasks.items()):
        task_dir_gate = read_json(session_root / "tasks" / task_id / "gate-summary.json", {})
        verdict = task_dir_gate.get("quality_gate_result", task.get("state", "unknown"))
        classification = task_dir_gate.get("classification", task.get("retry_decision", "unknown"))
        lines.append(f"- `{task_id}` Gate: `{verdict}` Classification: `{classification}` State: `{task.get('state', 'unknown')}`")
    return lines or ["- None recorded"]


def generate_report(session_root: Path, output: Path) -> None:
    state = read_json(session_root / "session-state.json", {})
    summary = read_json(session_root / "delegation-session-summary.json", {})
    gate = read_json(session_root / "gate-summary.json", {})
    env_gate = read_json(session_root / "env-gate.json", read_json(session_root / "environment" / "env-gate.json", {}))
    package_result = read_json(session_root / "package-result.json", {})
    validation = read_json(session_root / "workflow-validation-report.json", {})
    release = read_json(session_root / "release-result.json", {})
    events = read_events(session_root / "session-events.jsonl")
    tasks = state.get("tasks", {}) if isinstance(state.get("tasks"), dict) else {}
    failed_tasks = [task_id for task_id, task in tasks.items() if task.get("state") not in {"closed", "validated", "gate_passed"}]
    env_auto = env_gate.get("auto_recovered", []) if isinstance(env_gate, dict) else []
    package_errors = package_result.get("errors", []) if isinstance(package_result, dict) else []
    claude_enabled = bool(state.get("claude_worker_enabled") or state.get("live_dispatch_enabled"))
    codex_takeover = any("codex" in str(event).lower() and "takeover" in str(event).lower() for event in events) or gate.get("recommended_action") == "codex_takeover"
    roi_verdict = summary.get("token_savings_verdict") or ("no" if not claude_enabled else "uncertain")
    roi_confidence = summary.get("confidence") or ("medium" if not claude_enabled else "low")
    lines = [
        "# Delegation User Report",
        "",
        f"- Session ID: `{state.get('session_id', summary.get('session_id', 'unknown'))}`",
        f"- Execution result: `{state.get('status', 'unknown')}`",
        f"- Environment gate: `{env_gate.get('status', 'unknown')}`",
        f"- Artifact packaging: `{package_result.get('status', 'unknown')}`",
        f"- Quality gate: `{gate.get('quality_gate_result', 'unknown')}`",
        f"- Claude worker: `{'enabled' if claude_enabled else 'disabled'}`",
        f"- Stop-loss: `{gate.get('retry_decision', {}).get('stop_loss', False)}`",
        f"- Codex takeover: `{codex_takeover}`",
        f"- Token/ROI verdict: `{roi_verdict}` Confidence: `{roi_confidence}`",
        f"- Events recorded: `{len(events)}`",
        "",
        "## Environment Gate",
        "",
    ]
    lines.extend(bullets([f"`{issue.get('code')}` {issue.get('summary')}" for issue in env_gate.get("issues", [])] if isinstance(env_gate, dict) else []))
    lines.extend(["", "## Artifact Packaging", ""])
    if package_errors:
        lines.extend(bullets([f"`{item.get('label')}` {item.get('error')}" for item in package_errors]))
    else:
        lines.append(f"- Status: `{package_result.get('status', 'unknown')}`")
    lines.extend(["", "## Machine Gates", ""])
    lines.extend(task_gate_lines(session_root, state))
    lines.extend(["", "## Worker And Takeover", ""])
    lines.append(f"- Claude worker was `{'enabled' if claude_enabled else 'disabled'}` for this session.")
    lines.append(f"- Codex takeover reason: `{gate.get('recommended_action', 'none')}`")
    lines.extend(["", "## Auto Recovery", ""])
    lines.extend(bullets([f"`{item.get('code')}` {item.get('summary')}" for item in env_auto]))
    lines.extend(["", "## Unrecovered Risks", ""])
    risks = [f"Task `{task_id}` ended as `{tasks[task_id].get('state')}`" for task_id in failed_tasks]
    risks.extend([f"Packaging error: {item.get('error')}" for item in package_errors])
    lines.extend(bullets(risks))
    lines.extend(["", "## Tests And Release", ""])
    lines.append(f"- Workflow validation: `{validation.get('status', 'not_recorded')}`")
    lines.append(f"- Release result: `{release.get('status', 'not_recorded')}`")
    if release.get("release_id"):
        lines.append(f"- Release ID: `{release.get('release_id')}`")
    if release.get("rollback_ref"):
        lines.append(f"- Rollback ref: `{release.get('rollback_ref')}`")
    lines.extend(["", "## ROI Verdict", ""])
    lines.append(f"- Verdict: `{roi_verdict}`")
    lines.append(f"- Confidence: `{roi_confidence}`")
    lines.append("- Evidence: Claude live dispatch remains disabled until live smoke is stable, so this pass hardens reliability rather than claiming token savings.")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--session-root", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    output = args.output or (args.session_root / "user-report.md")
    generate_report(args.session_root, output)
    if not output.exists() or output.stat().st_size == 0:
        raise SystemExit("user-report.md was not generated")
    print(f"User report written to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
