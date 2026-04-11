#!/usr/bin/env python3
"""Deterministic session runner for delegate-to-omc v2.

This runner intentionally keeps v1 implementation small: it checkpoints session
state, appends machine-readable events, supports dry-run execution, and leaves
live Claude dispatch behind a future adapter boundary.
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


TASK_STATES = (
    "planned",
    "admitted",
    "dispatched",
    "running",
    "artifact_pulled",
    "gate_passed",
    "gate_failed",
    "retry_decided",
    "validated",
    "closed",
    "blocked",
)

DEFAULT_TRANSITIONS = (
    "admitted",
    "dispatched",
    "running",
    "artifact_pulled",
    "gate_passed",
    "retry_decided",
    "validated",
    "closed",
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


def append_event(path: Path, event: dict[str, Any], seen: set[tuple[str, str]]) -> None:
    key = (str(event.get("task_id", "")), str(event.get("event_type", "")))
    if key in seen:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(event, sort_keys=True) + "\n")
    seen.add(key)


def load_seen_events(path: Path) -> set[tuple[str, str]]:
    seen: set[tuple[str, str]] = set()
    if not path.exists():
        return seen
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        seen.add((str(row.get("task_id", "")), str(row.get("event_type", ""))))
    return seen


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
    session_id = str(manifest.get("session_id", manifest_path.stem))
    return manifest_path.parent / session_id


def load_or_initialize_state(manifest: dict[str, Any], state_path: Path) -> dict[str, Any]:
    state = read_json(state_path)
    if state:
        return state
    session_id = str(manifest.get("session_id") or "delegation-session")
    tasks = {
        task_id(task): {
            "task_id": task_id(task),
            "state": "planned",
            "attempt": int(task.get("attempt", 1)),
            "worktree": str(task.get("worktree", "")),
            "relevant_files": task.get("relevant_files", []),
            "updated_at": utc_now(),
        }
        for task in manifest_tasks(manifest)
    }
    return {
        "session_id": session_id,
        "status": "planned",
        "dry_run": False,
        "tasks": tasks,
        "created_at": utc_now(),
        "updated_at": utc_now(),
        "version": 1,
    }


def transition_task(state: dict[str, Any], events_path: Path, seen: set[tuple[str, str]], task: dict[str, Any], new_state: str, dry_run: bool) -> None:
    tid = task_id(task)
    current = state["tasks"].setdefault(tid, {"task_id": tid, "state": "planned"})
    if current.get("state") == new_state:
        return
    current["state"] = new_state
    current["updated_at"] = utc_now()
    append_event(
        events_path,
        {
            "event_type": f"task.{new_state}",
            "task_id": tid,
            "state": new_state,
            "dry_run": dry_run,
            "occurred_at": utc_now(),
        },
        seen,
    )


def run_session(manifest_path: Path, session_root: Path | None, dry_run: bool) -> dict[str, Any]:
    manifest = read_json(manifest_path)
    if not isinstance(manifest, dict):
        raise ValueError("Manifest must be a JSON object")
    root = session_root or default_session_root(manifest_path, manifest)
    root.mkdir(parents=True, exist_ok=True)
    state_path = root / "session-state.json"
    events_path = root / "session-events.jsonl"
    state = load_or_initialize_state(manifest, state_path)
    state["dry_run"] = dry_run
    state["updated_at"] = utc_now()
    seen = load_seen_events(events_path)
    append_event(events_path, {"event_type": "session.started", "task_id": "", "dry_run": dry_run, "occurred_at": utc_now()}, seen)

    for task in manifest_tasks(manifest):
        tid = task_id(task)
        task_state = state["tasks"].setdefault(tid, {"task_id": tid, "state": "planned"})
        if task_state.get("state") in {"closed", "blocked"}:
            continue
        transitions = task.get("dry_run_transitions") or DEFAULT_TRANSITIONS
        for new_state in transitions:
            if new_state not in TASK_STATES:
                raise ValueError(f"Unsupported task state: {new_state}")
            transition_task(state, events_path, seen, task, str(new_state), dry_run)
        write_json(state_path, state)

    task_states = [task.get("state") for task in state["tasks"].values()]
    state["status"] = "closed" if task_states and all(value == "closed" for value in task_states) else "blocked"
    state["updated_at"] = utc_now()
    write_json(state_path, state)
    append_event(events_path, {"event_type": f"session.{state['status']}", "task_id": "", "dry_run": dry_run, "occurred_at": utc_now()}, seen)
    return state


def main() -> int:
    parser = argparse.ArgumentParser(description="Run a deterministic delegation session")
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--session-root", type=Path)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    state = run_session(args.manifest, args.session_root, args.dry_run)
    print(json.dumps({"session_id": state["session_id"], "status": state["status"], "tasks": state["tasks"]}, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
