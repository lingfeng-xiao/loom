#!/usr/bin/env python3
"""Admission, machine gate, and stop-loss helper for delegate-to-omc v2."""

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


def text_blob(*values: Any) -> str:
    return "\n".join(str(value or "") for value in values)


def changed_files(status_text: str) -> list[str]:
    changed: list[str] = []
    for line in status_text.splitlines():
        parts = line.strip().split(maxsplit=1)
        if len(parts) == 2:
            changed.append(parts[1])
    return changed


def classify_gate(result: dict[str, Any], preflight: dict[str, Any], review: dict[str, Any], status_text: str, relevant_files: list[str], history: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    failed: list[str] = []
    classifications: list[str] = []
    recommended_action = "none"
    repair_size = "none"
    auto_retry = False
    stop_loss = False
    history = history or []
    diff_present = bool(result.get("diff_present") or changed_files(status_text))
    declared = str(result.get("declared_result", "")).upper()
    blob = text_blob(result.get("worker_status"), result.get("error"), result.get("stderr"), result.get("stdout"), result.get("summary"))

    for issue in preflight.get("issues", []) if isinstance(preflight.get("issues"), list) else []:
        code = str(issue.get("code") or issue.get("gate") or "")
        if code in {"baseline_stale", "skill_drift", "shell_environment_error", "test_artifact_pollution", "release_wait_timeout", "healthcheck_warmup_retry", "artifact_packaging_error"}:
            failed.append(code)
            classifications.append(code)
    if preflight.get("status") not in {None, "PASS", "WARN"}:
        failed.append("preflight")
        recommended_action = "fix_infrastructure"
        repair_size = "not_repairable_in_current_task"
    if result.get("artifact_packaging_error"):
        failed.append("artifact_packaging_error")
        classifications.append("artifact_packaging_error")
        recommended_action = "block_release"
    if "Execution error" in blob:
        failed.append("execution_error")
        if diff_present:
            classifications.append("artifact_needs_review")
            recommended_action = "codex_review"
            repair_size = "medium"
        else:
            classifications.append("claude_invocation_error")
            recommended_action = "codex_takeover"
            repair_size = "not_repairable_in_current_task"
    if result.get("timed_out") or result.get("idle_timed_out"):
        if result.get("timed_out"):
            failed.append("timeout")
        if result.get("idle_timed_out"):
            failed.append("idle_timeout")
        recommended_action = "resplit_task"
        repair_size = "large"
        classifications.append("timeout_or_idle_timeout")
    missing_fields = result.get("missing_contract_fields") or []
    if result.get("contract_complete") is False or missing_fields:
        failed.append("contract")
        classifications.append("contract_incomplete")
        recommended_action = "tiny_fix" if len(missing_fields) <= 2 else "small_fix"
        repair_size = "tiny" if len(missing_fields) <= 2 else "small"
        auto_retry = True
    if declared == "SUCCESS" and not diff_present:
        failed.append("diff")
        classifications.append("success_without_diff")
        recommended_action = "block_release"
        repair_size = "not_repairable_in_current_task"
        auto_retry = False
    if result.get("validation_reported") is False:
        failed.append("validation")
        recommended_action = "small_fix"
        repair_size = "small"
        auto_retry = True

    changed = changed_files(status_text)
    if relevant_files and changed:
        allowed = tuple(item.rstrip("/") for item in relevant_files)
        out_of_scope = [path for path in changed if not any(path == item or path.startswith(f"{item}/") for item in allowed)]
        if out_of_scope:
            failed.append("scope")
            classifications.append("scope_drift")
            recommended_action = "codex_takeover" if len(out_of_scope) <= 3 else "block_release"
            repair_size = "large"
            auto_retry = False
    if review.get("review_result") and review.get("review_result") != "PASS":
        failed.append("review")

    primary = "claude_invocation_error" if "claude_invocation_error" in classifications else (classifications[0] if classifications else "passed")
    same_exec_errors = sum(1 for row in history if row.get("classification") == "claude_invocation_error")
    if primary == "claude_invocation_error" and same_exec_errors >= 1:
        stop_loss = True
        recommended_action = "stop_loss"
        auto_retry = False
    quality = "FAIL" if failed else "PASS"
    retry = {"classification": primary, "auto_retry": bool(auto_retry and not stop_loss), "stop_loss": stop_loss, "recommended_action": recommended_action, "repair_size": repair_size}
    return {
        "quality_gate_result": quality,
        "classification": primary,
        "classifications": sorted(set(classifications)) or (["passed"] if quality == "PASS" else []),
        "failed_gates": sorted(set(failed)),
        "repair_size": repair_size,
        "recommended_action": recommended_action,
        "retry_decision": retry,
        "evidence": {
            "worker_status": result.get("worker_status", ""),
            "declared_result": result.get("declared_result", ""),
            "preflight_status": preflight.get("status", result.get("preflight_status", "")),
            "review_result": review.get("review_result", ""),
            "diff_present": diff_present,
        },
        "compact_summary": "Machine gate passed." if quality == "PASS" else f"Machine gate failed: {', '.join(sorted(set(failed)))}.",
    }


def run_admission(manifest_path: Path, locks_root: Path, output_path: Path, allow_release_lock: bool) -> int:
    manifest = read_json(manifest_path, {})
    issues: list[dict[str, str]] = []
    seen_files: dict[str, str] = {}
    for task in manifest.get("tasks", []):
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


def run_gate(task_dir: Path, output_path: Path, relevant_files: list[str], history_path: Path | None, retry_output: Path | None) -> int:
    result = read_json(task_dir / "result.json", {})
    preflight = read_json(task_dir / "preflight.json", {})
    review = read_json(task_dir / "review-result.json", {})
    status_text = (task_dir / "git.status.txt").read_text(encoding="utf-8", errors="replace") if (task_dir / "git.status.txt").exists() else ""
    history = read_json(history_path or (task_dir / "gate-history.json"), [])
    if not isinstance(history, list):
        history = []
    payload = classify_gate(result, preflight, review, status_text, relevant_files, history)
    write_json(output_path, payload)
    write_json(retry_output or (output_path.parent / "retry-decision.json"), payload["retry_decision"])
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
    gate.add_argument("--retry-output", type=Path)
    gate.add_argument("--history", type=Path)
    gate.add_argument("--relevant-file", action="append", default=[])
    args = parser.parse_args()
    if args.command == "admission":
        return run_admission(args.manifest, args.locks_root, args.output, args.allow_release_lock)
    return run_gate(args.task_dir, args.output, args.relevant_file, args.history, args.retry_output)


if __name__ == "__main__":
    raise SystemExit(main())
