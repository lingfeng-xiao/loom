#!/usr/bin/env python3
"""Admission and machine gate helper for delegate-to-omc v2."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def read_json(path: Path, default: Any | None = None) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def task_id(task: dict[str, Any]) -> str:
    return str(task.get("task_id") or task.get("id") or "")


def normalize_files(task: dict[str, Any]) -> set[str]:
    values = task.get("relevant_files") or task.get("files") or []
    return {str(value).rstrip("/") for value in values if str(value).strip()}


def classify_gate(result: dict[str, Any], preflight: dict[str, Any], review: dict[str, Any], status_text: str, relevant_files: list[str]) -> dict[str, Any]:
    failed: list[str] = []
    recommended_action = "none"
    repair_size = "none"
    quality = "PASS"

    if preflight.get("status") not in {None, "PASS"}:
        failed.append("preflight")
        recommended_action = "fix_infrastructure"
        repair_size = "not_repairable_in_current_task"
    if result.get("timed_out"):
        failed.append("timeout")
        recommended_action = "resplit_task"
        repair_size = "large"
    if result.get("idle_timed_out"):
        failed.append("idle_timeout")
        recommended_action = "resplit_task"
        repair_size = "large"
    if result.get("contract_complete") is False:
        failed.append("contract")
        recommended_action = "tiny_fix"
        repair_size = "tiny"
    if result.get("declared_result") == "SUCCESS" and result.get("diff_present") is False:
        failed.append("diff")
        recommended_action = "medium_fix"
        repair_size = "medium"
    if result.get("validation_reported") is False:
        failed.append("validation")
        recommended_action = "small_fix"
        repair_size = "small"

    changed = []
    for line in status_text.splitlines():
        parts = line.strip().split(maxsplit=1)
        if len(parts) == 2:
            changed.append(parts[1])
    if relevant_files and changed:
        allowed = tuple(item.rstrip("/") for item in relevant_files)
        out_of_scope = [path for path in changed if not any(path == item or path.startswith(f"{item}/") for item in allowed)]
        if out_of_scope:
            failed.append("scope")
            recommended_action = "codex_takeover"
            repair_size = "large"

    if review.get("review_result") and review.get("review_result") != "PASS":
        failed.append("review")

    if failed:
        quality = "FAIL"
    return {
        "quality_gate_result": quality,
        "failed_gates": sorted(set(failed)),
        "repair_size": repair_size,
        "recommended_action": recommended_action,
        "evidence": {
            "worker_status": result.get("worker_status", ""),
            "declared_result": result.get("declared_result", ""),
            "preflight_status": preflight.get("status", result.get("preflight_status", "")),
            "review_result": review.get("review_result", ""),
        },
        "compact_summary": "Machine gate passed." if quality == "PASS" else f"Machine gate failed: {', '.join(sorted(set(failed)))}.",
    }


def run_admission(manifest_path: Path, locks_root: Path, output_path: Path, allow_release_lock: bool) -> int:
    manifest = read_json(manifest_path, {})
    tasks = manifest.get("tasks", [])
    issues: list[dict[str, str]] = []
    seen_files: dict[str, str] = {}
    for task in tasks:
        tid = task_id(task)
        files = normalize_files(task)
        if not tid:
            issues.append({"task_id": "", "gate": "task_id", "summary": "Task is missing task_id/id."})
        if not files:
            issues.append({"task_id": tid, "gate": "relevant_files", "summary": "Task is missing relevant_files."})
        if not task.get("done_when"):
            issues.append({"task_id": tid, "gate": "done_when", "summary": "Task is missing done_when."})
        if not task.get("validation"):
            issues.append({"task_id": tid, "gate": "validation", "summary": "Task is missing validation."})
        for key in ("timeout_seconds", "idle_timeout_seconds"):
            if int(task.get(key, manifest.get(key, 0)) or 0) <= 0:
                issues.append({"task_id": tid, "gate": key, "summary": f"{key} must be positive."})
        for file_path in files:
            owner = seen_files.get(file_path)
            if owner and owner != tid:
                issues.append({"task_id": tid, "gate": "file_overlap", "summary": f"{file_path} overlaps with {owner}."})
            seen_files[file_path] = tid
        worktree = task.get("worktree")
        if worktree and (locks_root / "worktrees" / f"{Path(str(worktree)).name}.lock").exists():
            issues.append({"task_id": tid, "gate": "worktree_lock", "summary": f"Worktree is locked: {worktree}."})
    if (locks_root / "release.lock").exists() and not allow_release_lock:
        issues.append({"task_id": "", "gate": "release_lock", "summary": "Release lock is present."})
    payload = {"admission_result": "FAIL" if issues else "PASS", "issues": issues}
    write_json(output_path, payload)
    return 2 if issues else 0


def run_gate(task_dir: Path, output_path: Path, relevant_files: list[str]) -> int:
    result = read_json(task_dir / "result.json", {})
    preflight = read_json(task_dir / "preflight.json", {})
    review = read_json(task_dir / "review-result.json", {})
    status_text = (task_dir / "git.status.txt").read_text(encoding="utf-8", errors="replace") if (task_dir / "git.status.txt").exists() else ""
    payload = classify_gate(result, preflight, review, status_text, relevant_files)
    write_json(output_path, payload)
    return 2 if payload["quality_gate_result"] != "PASS" else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    admission = sub.add_parser("admission")
    admission.add_argument("--manifest", required=True, type=Path)
    admission.add_argument("--locks-root", required=True, type=Path)
    admission.add_argument("--output", required=True, type=Path)
    admission.add_argument("--allow-release-lock", action="store_true")
    gate = sub.add_parser("gate")
    gate.add_argument("--task-dir", required=True, type=Path)
    gate.add_argument("--output", required=True, type=Path)
    gate.add_argument("--relevant-file", action="append", default=[])
    args = parser.parse_args()
    if args.command == "admission":
        return run_admission(args.manifest, args.locks_root, args.output, args.allow_release_lock)
    return run_gate(args.task_dir, args.output, args.relevant_file)


if __name__ == "__main__":
    raise SystemExit(main())
