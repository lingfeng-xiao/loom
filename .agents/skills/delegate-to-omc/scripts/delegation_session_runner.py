#!/usr/bin/env python3
"""Deterministic session runner for delegate-to-omc v2."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

RUNNER_VERSION = "2.0"
PHASES = (
    "environment_checked",
    "packaged",
    "admitted",
    "dispatch_ready",
    "dispatched",
    "artifact_pulled",
    "machine_gated",
    "retry_decided",
    "validated",
    "reported",
)


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def read_json(path: Path, default: Any | None = None) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def load_seen_events(path: Path) -> set[tuple[str, str, str]]:
    seen: set[tuple[str, str, str]] = set()
    if not path.exists():
        return seen
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        seen.add((str(row.get("task_id", "")), str(row.get("event_type", "")), str(row.get("phase", ""))))
    return seen


def append_event(path: Path, event: dict[str, Any], seen: set[tuple[str, str, str]]) -> None:
    event.setdefault("occurred_at", utc_now())
    key = (str(event.get("task_id", "")), str(event.get("event_type", "")), str(event.get("phase", "")))
    if key in seen:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(event, sort_keys=True) + "\n")
    seen.add(key)


def task_id(task: dict[str, Any]) -> str:
    value = task.get("task_id") or task.get("id")
    if not value:
        raise ValueError("Each task must include task_id or id")
    return str(value)


def manifest_tasks(manifest: dict[str, Any]) -> list[dict[str, Any]]:
    tasks = manifest.get("tasks", [])
    if not isinstance(tasks, list) or not tasks:
        raise ValueError("Manifest must include a non-empty tasks list")
    return [task for task in tasks if isinstance(task, dict)]


def default_session_root(manifest_path: Path, manifest: dict[str, Any]) -> Path:
    root = manifest.get("session_root")
    if root:
        return Path(str(root))
    return manifest_path.parent / str(manifest.get("session_id", manifest_path.stem))


def initial_task(task: dict[str, Any]) -> dict[str, Any]:
    tid = task_id(task)
    return {
        "task_id": tid,
        "state": "planned",
        "attempt": int(task.get("attempt", 1)),
        "dispatch_count": 0,
        "adapter": str(task.get("adapter", "fake_worker")),
        "fake_result": str(task.get("fake_result", "success")),
        "worktree": str(task.get("worktree", "")),
        "relevant_files": task.get("relevant_files", []),
        "updated_at": utc_now(),
    }


def load_or_initialize_state(manifest: dict[str, Any], state_path: Path) -> dict[str, Any]:
    state = read_json(state_path)
    if state:
        return state
    return {
        "session_id": str(manifest.get("session_id") or "delegation-session"),
        "status": "planned",
        "runner_version": RUNNER_VERSION,
        "live_dispatch_enabled": bool(manifest.get("live_dispatch_enabled", False)),
        "claude_worker_enabled": bool(manifest.get("claude_worker_enabled", False)),
        "completed_phases": [],
        "created_at": utc_now(),
        "updated_at": utc_now(),
        "tasks": {task_id(task): initial_task(task) for task in manifest_tasks(manifest)},
    }


def complete_phase(state: dict[str, Any], events_path: Path, seen: set[tuple[str, str, str]], phase: str) -> None:
    if phase in state.setdefault("completed_phases", []):
        return
    state["completed_phases"].append(phase)
    state["status"] = phase
    state["updated_at"] = utc_now()
    append_event(events_path, {"event_type": f"phase.{phase}", "phase": phase, "task_id": "", "status": phase}, seen)


def set_task_state(state: dict[str, Any], events_path: Path, seen: set[tuple[str, str, str]], tid: str, new_state: str, phase: str) -> None:
    task = state["tasks"][tid]
    if task.get("state") == new_state:
        return
    task["state"] = new_state
    task["updated_at"] = utc_now()
    append_event(events_path, {"event_type": f"task.{new_state}", "phase": phase, "task_id": tid, "state": new_state}, seen)


def ensure_report(session_root: Path, state: dict[str, Any]) -> None:
    report = session_root / "user-report.md"
    if report.exists() and report.stat().st_size > 0:
        return
    report.write_text("\n".join([
        "# Delegation User Report",
        "",
        f"- Session ID: `{state.get('session_id', 'unknown')}`",
        "- Execution result: `runner-placeholder`",
        "- Claude worker: `disabled`",
        "- Codex takeover: `not required for fake-worker validation`",
        "- Token/ROI verdict: `uncertain` Confidence: `low`",
        "",
    ]), encoding="utf-8")


def apply_phase(phase: str, manifest: dict[str, Any], state: dict[str, Any], session_root: Path, events_path: Path, seen: set[tuple[str, str, str]]) -> None:
    tasks = manifest_tasks(manifest)
    if phase == "dispatched":
        for task in tasks:
            tid = task_id(task)
            current = state["tasks"].setdefault(tid, initial_task(task))
            if current.get("dispatched_at"):
                continue
            if state.get("live_dispatch_enabled") or state.get("claude_worker_enabled"):
                current["adapter"] = "placeholder_live_dispatch_disabled"
            current["dispatch_count"] = int(current.get("dispatch_count", 0)) + 1
            current["dispatched_at"] = utc_now()
            set_task_state(state, events_path, seen, tid, "dispatched", phase)
        return
    if phase == "artifact_pulled":
        for task in tasks:
            tid = task_id(task)
            if state["tasks"][tid].get("state") == "dispatched":
                set_task_state(state, events_path, seen, tid, "artifact_pulled", phase)
        return
    if phase == "machine_gated":
        for task in tasks:
            tid = task_id(task)
            fake_result = str(task.get("fake_result", state["tasks"][tid].get("fake_result", "success"))).lower()
            set_task_state(state, events_path, seen, tid, "gate_passed" if fake_result in {"success", "pass", "closed"} else "gate_failed", phase)
        return
    if phase == "retry_decided":
        for task in tasks:
            tid = task_id(task)
            current = state["tasks"][tid]
            current["retry_decision"] = "none" if current.get("state") == "gate_passed" else "blocked"
            current["updated_at"] = utc_now()
            append_event(events_path, {"event_type": "task.retry_decided", "phase": phase, "task_id": tid, "retry_decision": current["retry_decision"]}, seen)
        return
    if phase == "validated":
        for task in tasks:
            tid = task_id(task)
            if state["tasks"][tid].get("state") == "gate_passed":
                set_task_state(state, events_path, seen, tid, "validated", phase)
        return
    if phase == "reported":
        ensure_report(session_root, state)
        return
    for task in tasks:
        tid = task_id(task)
        if state["tasks"].get(tid, {}).get("state") not in {"validated", "gate_failed", "blocked"}:
            set_task_state(state, events_path, seen, tid, phase, phase)


def terminal_status(state: dict[str, Any], session_root: Path) -> str:
    task_states = [task.get("state") for task in state.get("tasks", {}).values()]
    if task_states and all(value in {"validated", "closed"} for value in task_states) and (session_root / "user-report.md").exists():
        return "closed"
    return "blocked"


def run_session(manifest_path: Path, session_root: Path | None, dry_run: bool) -> dict[str, Any]:
    manifest = read_json(manifest_path)
    if not isinstance(manifest, dict):
        raise ValueError("Manifest must be a JSON object")
    root = session_root or default_session_root(manifest_path, manifest)
    root.mkdir(parents=True, exist_ok=True)
    state_path = root / "session-state.json"
    events_path = root / "session-events.jsonl"
    state = load_or_initialize_state(manifest, state_path)
    if state.get("status") == "blocked" and "blocked" in state.get("completed_phases", []):
        return state
    state["dry_run"] = dry_run
    state["updated_at"] = utc_now()
    seen = load_seen_events(events_path)
    append_event(events_path, {"event_type": "session.started", "phase": "", "task_id": "", "dry_run": dry_run, "runner_version": RUNNER_VERSION}, seen)
    for phase in PHASES:
        if phase in state.get("completed_phases", []):
            continue
        apply_phase(phase, manifest, state, root, events_path, seen)
        complete_phase(state, events_path, seen, phase)
        write_json(state_path, state)
    final = terminal_status(state, root)
    state["status"] = final
    if final not in state.setdefault("completed_phases", []):
        state["completed_phases"].append(final)
    for tid, task in state.get("tasks", {}).items():
        if final == "closed" and task.get("state") == "validated":
            set_task_state(state, events_path, seen, tid, "closed", final)
        elif final == "blocked" and task.get("state") in {"gate_failed", "blocked"}:
            set_task_state(state, events_path, seen, tid, "blocked", final)
    state["updated_at"] = utc_now()
    write_json(state_path, state)
    append_event(events_path, {"event_type": f"session.{final}", "phase": final, "task_id": "", "dry_run": dry_run}, seen)
    return state


def main() -> int:
    parser = argparse.ArgumentParser(description="Run a deterministic delegation session")
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--session-root", type=Path)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    state = run_session(args.manifest, args.session_root, args.dry_run)
    print(json.dumps({"session_id": state["session_id"], "status": state["status"], "tasks": state["tasks"]}, indent=2, sort_keys=True))
    return 2 if state["status"] == "blocked" else 0


if __name__ == "__main__":
    raise SystemExit(main())
