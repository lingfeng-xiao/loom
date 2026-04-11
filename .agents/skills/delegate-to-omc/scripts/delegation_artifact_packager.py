#!/usr/bin/env python3
"""Fail-closed artifact packager for delegate-to-omc v2 sessions."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import stat
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_utf8_no_bom(path: Path) -> None:
    data = path.read_bytes()
    if data.startswith(b"\xef\xbb\xbf"):
        raise ValueError(f"UTF-8 BOM is not allowed: {path}")
    data.decode("utf-8")


def shell_quote(value: str) -> str:
    return "'" + value.replace("'", "'\\''") + "'"


def as_path(repo: Path, value: str) -> Path:
    path = Path(value)
    return path if path.is_absolute() else repo / path


def collect_sources(repo: Path, manifest_path: Path, manifest: dict[str, Any]) -> list[tuple[str, Path, Path]]:
    sources: list[tuple[str, Path, Path]] = [("manifest", manifest_path, Path("session-manifest.json"))]
    if manifest.get("brief_path"):
        sources.append(("brief", as_path(repo, str(manifest["brief_path"])), Path("brief.md")))
    if manifest.get("prompt_input_path"):
        sources.append(("prompt", as_path(repo, str(manifest["prompt_input_path"])), Path("prompt-input.txt")))
    for task in manifest.get("tasks", []) or []:
        if not isinstance(task, dict):
            continue
        tid = str(task.get("task_id") or task.get("id") or "task")
        if task.get("brief_path"):
            sources.append((f"task:{tid}:brief", as_path(repo, str(task["brief_path"])), Path("tasks") / tid / "brief.md"))
        if task.get("prompt_input_path"):
            sources.append((f"task:{tid}:prompt", as_path(repo, str(task["prompt_input_path"])), Path("tasks") / tid / "prompt-input.txt"))
    return sources


def package(args: argparse.Namespace) -> int:
    repo = args.repo_root.resolve()
    manifest_path = args.manifest.resolve()
    errors: list[dict[str, str]] = []
    copied: list[dict[str, str]] = []
    session_dir: Path | None = None
    session_id = ""
    try:
        validate_utf8_no_bom(manifest_path)
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if not isinstance(manifest, dict):
            raise ValueError("Manifest must be a JSON object")
        session_id = str(manifest.get("session_id") or manifest_path.stem)
        if not session_id.strip():
            raise ValueError("session_id is required")
        session_dir = (args.delegation_root if args.delegation_root.is_absolute() else repo / args.delegation_root) / session_id
        session_dir.mkdir(parents=True, exist_ok=True)
        probe = session_dir / ".write-probe"
        probe.write_text("ok\n", encoding="utf-8")
        probe.unlink()
        sources = collect_sources(repo, manifest_path, manifest)
        if not any(label.endswith("brief") or label == "brief" for label, _src, _dest in sources):
            raise ValueError("At least one brief_path is required")
        expected = {str(key): str(value) for key, value in (manifest.get("expected_checksums") or {}).items()}
        for label, source, rel_dest in sources:
            try:
                if not source.exists() or not source.is_file():
                    raise FileNotFoundError(str(source))
                validate_utf8_no_bom(source)
                actual = sha256(source)
                keys = {str(source)}
                try:
                    keys.add(str(source.relative_to(repo)))
                except ValueError:
                    pass
                for key in keys:
                    if key in expected and expected[key] != actual:
                        raise ValueError(f"checksum mismatch for {key}")
                dest = session_dir / rel_dest
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, dest)
                copied_hash = sha256(dest)
                if copied_hash != actual:
                    raise ValueError(f"copy checksum mismatch for {source}")
                copied.append({"label": label, "source": str(source), "destination": str(dest), "sha256": copied_hash})
            except Exception as exc:
                errors.append({"label": label, "source": str(source), "error": str(exc)})
        runner_script = session_dir / "uploaded-runner.sh"
        runner_script.write_text("\n".join([
            "#!/usr/bin/env bash",
            "set -euo pipefail",
            f"cd {shell_quote(str(repo))}",
            f"exec {shell_quote(str(repo / '.agents/skills/delegate-to-omc/scripts/run-delegation-session.sh'))} --manifest {shell_quote(str(session_dir / 'session-manifest.json'))} --session-root {shell_quote(str(session_dir))} --dry-run",
            "",
        ]), encoding="utf-8")
        runner_script.chmod(runner_script.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP)
        copied.append({"label": "uploaded_runner_script", "source": "generated", "destination": str(runner_script), "sha256": sha256(runner_script)})
    except Exception as exc:
        errors.append({"label": "package", "source": str(manifest_path), "error": str(exc)})

    payload = {
        "status": "FAIL" if errors else "PASS",
        "session_id": session_id,
        "session_dir": str(session_dir or ""),
        "copied": copied,
        "errors": errors,
        "generated_at": utc_now(),
        "fail_closed": bool(errors),
    }
    output = args.output or ((session_dir or args.delegation_root) / "package-result.json")
    write_json(output if output.is_absolute() else repo / output, payload)
    print(json.dumps(payload, indent=2, sort_keys=True))
    return 2 if payload["status"] != "PASS" else 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Package delegation artifacts without inline SSH/JSON")
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--delegation-root", type=Path, default=Path(".delegations"))
    parser.add_argument("--output", type=Path)
    return package(parser.parse_args())


if __name__ == "__main__":
    raise SystemExit(main())
