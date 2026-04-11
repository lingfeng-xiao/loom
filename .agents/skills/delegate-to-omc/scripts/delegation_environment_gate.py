#!/usr/bin/env python3
"""Phase 0 environment gate for delegate-to-omc v2."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pwd
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

RUNNER_VERSION = "2.0"
CACHE_DIR_NAMES = {"__pycache__", ".pytest_cache"}


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def run(cmd: list[str], cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, cwd=str(cwd) if cwd else None, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def add_issue(issues: list[dict[str, Any]], code: str, summary: str, severity: str = "FAIL", **extra: Any) -> None:
    row = {"code": code, "summary": summary, "severity": severity}
    row.update({key: value for key, value in extra.items() if value is not None})
    issues.append(row)


def append_issue_event(path: Path | None, event: dict[str, Any]) -> None:
    if not path:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(event, sort_keys=True) + "\n")


def git_value(repo: Path, *args: str) -> str:
    result = run(["git", *args], cwd=repo)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip() or f"git {' '.join(args)} failed")
    return result.stdout.strip()


def clean_test_caches(repo: Path) -> list[str]:
    recovered: list[str] = []
    for root, dirs, _files in os.walk(repo):
        root_path = Path(root)
        if ".git" in root_path.parts:
            dirs[:] = []
            continue
        for dirname in list(dirs):
            if dirname in CACHE_DIR_NAMES:
                target = root_path / dirname
                shutil.rmtree(target)
                recovered.append(str(target.relative_to(repo)))
                dirs.remove(dirname)
    return sorted(recovered)


def dir_checksum(root: Path) -> str:
    digest = hashlib.sha256()
    if not root.exists():
        return ""
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = path.relative_to(root).as_posix()
        digest.update(rel.encode("utf-8"))
        digest.update(b"\0")
        digest.update(hashlib.sha256(path.read_bytes()).hexdigest().encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest()


def default_shell() -> str:
    try:
        return pwd.getpwuid(os.getuid()).pw_shell or os.environ.get("SHELL", "")
    except Exception:
        return os.environ.get("SHELL", "")


def runner_version(repo: Path) -> str:
    runner = repo / ".agents/skills/delegate-to-omc/scripts/delegation_session_runner.py"
    if not runner.exists():
        return ""
    for line in runner.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("RUNNER_VERSION"):
            return line.split("=", 1)[1].strip().strip("\"'")
    return ""


def check_baseline(repo: Path, baseline_ref: str, allow_missing: bool, issues: list[dict[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {"baseline_ref": baseline_ref, "status": "skipped" if not baseline_ref else "unknown"}
    if not baseline_ref:
        return result
    rev = run(["git", "rev-parse", "--verify", baseline_ref], cwd=repo)
    if rev.returncode != 0:
        result["status"] = "missing"
        if not allow_missing:
            add_issue(issues, "baseline_stale", f"Baseline ref is missing: {baseline_ref}")
        return result
    baseline_head = rev.stdout.strip()
    current_head = git_value(repo, "rev-parse", "HEAD")
    result.update({"baseline_head": baseline_head, "current_head": current_head})
    if baseline_head == current_head:
        result["status"] = "current"
        return result
    ancestor = run(["git", "merge-base", "--is-ancestor", baseline_head, current_head], cwd=repo)
    result["status"] = "stale" if ancestor.returncode == 0 else "diverged"
    add_issue(issues, "baseline_stale", f"Baseline {baseline_ref} is {result['status']} relative to HEAD.", baseline_head=baseline_head, current_head=current_head)
    return result


def smoke_claude(mode: str, repo: Path, issues: list[dict[str, Any]]) -> dict[str, Any]:
    payload: dict[str, Any] = {"mode": mode, "used_for_development": False}
    if mode in {"off", "mock", "dry-run"}:
        payload.update({"status": "mocked" if mode != "off" else "skipped", "contract_output": {"RESULT": "SUCCESS", "SUMMARY": "mock smoke only"}})
        return payload
    claude = shutil.which("claude")
    if not claude:
        payload["status"] = "missing"
        add_issue(issues, "claude_invocation_error", "Claude binary is unavailable for optional live smoke.", severity="WARN")
        return payload
    proc = run([claude, "-p", "Print RESULT: SUCCESS and SUMMARY: smoke only. Do not edit repository files."], cwd=repo)
    payload.update({"status": "pass" if proc.returncode == 0 and "RESULT:" in proc.stdout else "fail", "returncode": proc.returncode, "stdout_preview": proc.stdout[:500], "stderr_preview": proc.stderr[:500]})
    if payload["status"] != "pass":
        add_issue(issues, "claude_invocation_error", "Optional Claude smoke failed; live dispatch remains disabled.", severity="WARN")
    return payload


def run_gate(args: argparse.Namespace) -> int:
    repo = args.repo_root.resolve()
    output_dir = args.output_dir.resolve()
    issues: list[dict[str, Any]] = []
    auto_recovered: list[dict[str, Any]] = []
    checks: dict[str, Any] = {"generated_at": utc_now(), "runner_version": RUNNER_VERSION}

    recovered = clean_test_caches(repo) if args.auto_clean_caches else []
    if recovered:
        auto_recovered.append({"code": "test_artifact_pollution", "summary": "Removed test cache directories.", "paths": recovered, "auto_recovered": True})

    branch = git_value(repo, "branch", "--show-current")
    head = git_value(repo, "rev-parse", "HEAD")
    status_text = git_value(repo, "status", "--porcelain")
    clean = status_text == ""
    checks["server_repo"] = {"path": str(repo), "branch": branch, "head": head, "git_clean": clean, "status_text": status_text}
    if args.expected_branch and branch != args.expected_branch:
        add_issue(issues, "server_branch_mismatch", f"Expected branch {args.expected_branch}, got {branch}.")
    if args.expected_head and head != args.expected_head:
        add_issue(issues, "server_head_mismatch", "Server HEAD does not match expected HEAD.", expected=args.expected_head, actual=head)
    if not clean:
        add_issue(issues, "test_artifact_pollution", "Server repo is not git clean after cache cleanup.", status_text=status_text)

    checks["baseline"] = check_baseline(repo, args.baseline_ref, args.allow_missing_baseline, issues)
    actual_runner_version = runner_version(repo)
    checks["runner_version_check"] = {"expected": args.expected_runner_version, "actual": actual_runner_version, "status": "pass" if actual_runner_version == args.expected_runner_version else "fail"}
    if actual_runner_version != args.expected_runner_version:
        add_issue(issues, "baseline_stale", "Runner version does not match the expected environment-gate version.", expected=args.expected_runner_version, actual=actual_runner_version)

    server_skill_checksum = dir_checksum(repo / ".agents/skills/delegate-to-omc")
    checks["local_mirror"] = {"status": "skipped"}
    if args.local_mirror_root:
        local_root = args.local_mirror_root.resolve()
        local_head = git_value(local_root, "rev-parse", "HEAD") if (local_root / ".git").exists() else ""
        local_skill_checksum = dir_checksum(local_root / ".agents/skills/delegate-to-omc")
        checks["local_mirror"] = {"status": "checked", "path": str(local_root), "head": local_head, "server_head": head, "skill_checksum": local_skill_checksum, "server_skill_checksum": server_skill_checksum}
        if local_head != head:
            add_issue(issues, "mirror_mismatch", "Local mirror HEAD differs from server HEAD.", local_head=local_head, server_head=head)
        if local_skill_checksum != server_skill_checksum:
            add_issue(issues, "skill_drift", "Local mirror skill differs from server skill.")
    elif args.local_head or args.local_skill_checksum:
        checks["local_mirror"] = {"status": "checked", "head": args.local_head, "server_head": head, "skill_checksum": args.local_skill_checksum, "server_skill_checksum": server_skill_checksum}
        if args.local_head and args.local_head != head:
            add_issue(issues, "mirror_mismatch", "Local mirror HEAD differs from server HEAD.", local_head=args.local_head, server_head=head)
        if args.local_skill_checksum and args.local_skill_checksum != server_skill_checksum:
            add_issue(issues, "skill_drift", "Local mirror skill differs from server skill.")

    worktree_root = args.worktree_root.resolve()
    try:
        probe = worktree_root / ".delegations" / "_write_probe"
        probe.parent.mkdir(parents=True, exist_ok=True)
        probe.write_text("ok\n", encoding="utf-8")
        probe.unlink()
        checks["worktree_root"] = {"path": str(worktree_root), "writable": True}
    except Exception as exc:
        checks["worktree_root"] = {"path": str(worktree_root), "writable": False, "error": str(exc)}
        add_issue(issues, "worktree_not_writable", f"Worktree root is not writable: {exc}")

    locks_root = args.locks_root.resolve()
    lock_paths = [locks_root / "release.lock", repo / ".release/release.lock", repo / "release.lock", locks_root / "worktrees" / f"{worktree_root.name}.lock"]
    present_locks = [str(path) for path in lock_paths if path.exists()]
    checks["locks"] = {"locks_root": str(locks_root), "present": present_locks}
    for path in present_locks:
        add_issue(issues, "release_lock" if path.endswith("release.lock") else "worktree_lock", f"Lock is present: {path}")

    shell = default_shell()
    checks["default_shell"] = {"value": shell, "zsh_glob_risk": "zsh" in shell}
    if "zsh" in shell:
        add_issue(issues, "shell_environment_error", "Default shell is zsh; upload Bash scripts for complex remote execution.", severity="WARN")

    checks["claude_smoke"] = smoke_claude(args.claude_smoke, repo, issues)
    checks["auto_recovered"] = auto_recovered
    fatal = [issue for issue in issues if issue.get("severity", "FAIL") == "FAIL"]
    payload = {"status": "FAIL" if fatal else ("WARN" if issues else "PASS"), "issues": issues, "checks": checks, "auto_recovered": auto_recovered}
    write_json(output_dir / "env-gate.json", payload)
    lines = ["# Delegation Environment Gate", "", f"- Status: `{payload['status']}`", f"- Server HEAD: `{head}`", f"- Branch: `{branch}`", f"- Git clean: `{clean}`", f"- Claude smoke: `{checks['claude_smoke']['status']}`", "", "## Issues", ""]
    lines.extend([f"- `{item['code']}` {item['summary']}" for item in issues] or ["- None recorded"])
    lines.extend(["", "## Auto Recovery", ""])
    lines.extend([f"- `{item['code']}` {item['summary']} Paths: `{', '.join(item.get('paths', []))}`" for item in auto_recovered] or ["- None recorded"])
    (output_dir / "env-gate.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    append_issue_event(args.workflow_issue_events, {"event_type": "environment_gate", "status": payload["status"], "issues": issues, "occurred_at": utc_now()})
    return 2 if fatal else 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Run delegate-to-omc v2 environment gate")
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--output-dir", type=Path, default=Path(".delegations/_environment"))
    parser.add_argument("--expected-head", default="")
    parser.add_argument("--expected-branch", default="")
    parser.add_argument("--baseline-ref", default="workflow-runner-baseline")
    parser.add_argument("--allow-missing-baseline", action="store_true")
    parser.add_argument("--expected-runner-version", default=RUNNER_VERSION)
    parser.add_argument("--local-mirror-root", type=Path)
    parser.add_argument("--local-head", default="")
    parser.add_argument("--local-skill-checksum", default="")
    parser.add_argument("--worktree-root", type=Path, default=Path.cwd())
    parser.add_argument("--locks-root", type=Path, default=Path(".delegations/locks"))
    parser.add_argument("--workflow-issue-events", type=Path)
    parser.add_argument("--claude-smoke", choices=["off", "mock", "dry-run", "live"], default="mock")
    parser.add_argument("--no-auto-clean-caches", dest="auto_clean_caches", action="store_false")
    parser.set_defaults(auto_clean_caches=True)
    return run_gate(parser.parse_args())


if __name__ == "__main__":
    raise SystemExit(main())
