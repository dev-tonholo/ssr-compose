#!/usr/bin/env python3
"""Prove a crew works by checking a Claude Code stream-json transcript of one small task.

Usage:
  python3 trial-run.py <repo-path> --transcript <file> [--main-before <sha>] [--question]   evaluate a transcript
  python3 trial-run.py <repo-path> --run                                                     ask, then run the change trial
  python3 trial-run.py <repo-path> --run-question                                            ask, then run the question trial
  python3 trial-run.py <repo-path> --prompt                                                  print the change trial prompt
The change trial must start the crew; the question trial must not.
Prints one "PASS|FAIL|SKIP <check>" line per check and "tokens <role> <n>" lines.
Exit codes: 0 no check failed, 1 a check failed, 2 bad usage or the repo cannot be opened.
"""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

MANAGER = "engineering-manager"
READ_ONLY_ROLES = ("reviewer", "qa", "advisor", "designer")
EDIT_TOOLS = ("Edit", "Write", "NotebookEdit", "MultiEdit")
AGENT_TOOLS = ("Agent", "Task")
ADVISOR = "advisor"
WORKER_BRIEF = "Crew role: worker"
UNKNOWN = "unknown"
CLAUDE = "claude"
PUSH_PATTERN = re.compile(r"git push|gh pr create|glab mr create")
SHELL_WRITE = re.compile(r" ran (.*[;&|] *)?(git (commit|checkout|reset|apply|stash|restore|rm|mv|add|push)|sed -i|rm |mv |tee )")
REVIEWER_VERDICT = re.compile(r"PASS|CHANGES REQUESTED", re.MULTILINE)
QA_VERDICT = re.compile(r"(^|[^A-Z-])(GO|NO-GO|INCOMPLETE)([^A-Z]|$)", re.MULTILINE)
TRIAL_FILE = "docs/crew-trial.md"
TRIAL_PROMPT = (
    f'Fix the typo "teh" in {TRIAL_FILE}. For this trial, keep the work item and review in .crew/items/ and .crew/reviews/ '
    "using .crew/templates/, and do not use the host. Run in the driver seat for this trial; nobody will answer."
)
QUESTION_PROMPT = "What does the README say this project is for?"
ALLOWED_TOOLS = "Bash(git *),Bash(python3 .ai/scripts/*)"
DISALLOWED_TOOLS = "Bash(git push *)"
USAGE = "Usage: trial-run.py <repo-path> --transcript <file> [--main-before <sha>] [--question] | --run | --run-question | --prompt"
GUIDELINES = Path(".ai/guidelines.md")
WORKSPACES_LINE = re.compile(r"^- Workspaces: `([^`<>]+)(?:<[^`>]*>)?`", re.MULTILINE)
TOKEN_FIELDS = ("input_tokens", "cache_creation_input_tokens", "cache_read_input_tokens", "output_tokens")


@dataclass
class ToolUse:
    role: str
    id: str
    name: str
    input: dict

    def file_path(self) -> str:
        value = self.input.get("file_path")
        return value if isinstance(value, str) else ""

    def is_edit(self) -> bool:
        return self.name in EDIT_TOOLS

    def is_dispatch(self) -> bool:
        return self.name in AGENT_TOOLS

    def subagent(self) -> str:
        return str(self.input.get("subagent_type") or UNKNOWN)


class Transcript:
    def __init__(self, path: Path) -> None:
        self.events = []
        for line in path.read_text(errors="replace").splitlines():
            try:
                event = json.loads(line)
            except ValueError:
                continue
            if isinstance(event, dict):
                self.events.append(event)
        self.uses_by_parent = [(parent, ToolUse("", str(use.get("id") or ""), str(use.get("name") or ""), tool_input(use)))
                               for parent, use in self.raw_uses()]
        self.roles = {use.id: use.subagent() for _, use in self.uses_by_parent if use.is_dispatch() and use.id}
        self.uses = [ToolUse(self.role_of(parent), use.id, use.name, use.input) for parent, use in self.uses_by_parent]

    def raw_uses(self) -> list[tuple[str | None, dict]]:
        uses = []
        for event in self.events:
            content = message_of(event).get("content")
            if event.get("type") != "assistant" or not isinstance(content, list):
                continue
            uses += [(event.get("parent_tool_use_id"), item) for item in content if isinstance(item, dict) and item.get("type") == "tool_use"]
        return uses

    def role_of(self, parent: str | None) -> str:
        return MANAGER if parent is None else self.roles.get(parent, UNKNOWN)

    def dispatches(self, role: str) -> list[ToolUse]:
        return [use for use in self.uses if use.is_dispatch() and use.subagent() == role]

    def results_of(self, role: str) -> str:
        ids = {use.id for use in self.dispatches(role)}
        texts = []
        for event in self.events:
            content = message_of(event).get("content")
            if event.get("type") != "user" or not isinstance(content, list):
                continue
            for item in content:
                if isinstance(item, dict) and item.get("type") == "tool_result" and item.get("tool_use_id") in ids:
                    texts.append(result_text(item.get("content")))
        return "\n".join(texts)

    def denials(self) -> list[dict]:
        found = []
        for event in self.events:
            listed = event.get("permission_denials") if event.get("type") == "result" else None
            found += [denial for denial in listed or [] if isinstance(denial, dict)]
        return found

    def role_of_use(self, use_id: str) -> str:
        return next((use.role for use in self.uses if use.id == use_id), UNKNOWN)

    def token_lines(self) -> list[str]:
        seen, totals = set(), {}
        for index, event in enumerate(self.events):
            message = message_of(event)
            usage = message.get("usage")
            if event.get("type") != "assistant" or not isinstance(usage, dict):
                continue
            key = message.get("id") or f"line-{index}"
            if key in seen:
                continue
            seen.add(key)
            role = self.role_of(event.get("parent_tool_use_id"))
            totals[role] = totals.get(role, 0) + sum(int(usage.get(field) or 0) for field in TOKEN_FIELDS)
        return [f"tokens {role} {total}" for role, total in sorted(totals.items())]


def tool_input(use: dict) -> dict:
    value = use.get("input", use.get("tool_input"))
    return value if isinstance(value, dict) else {}


def message_of(event: dict) -> dict:
    message = event.get("message")
    return message if isinstance(message, dict) else {}


def result_text(content) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return " ".join(str(item.get("text") or "") for item in content if isinstance(item, dict))
    return ""


def first_matching_line(lines: list[str], pattern: re.Pattern) -> str:
    return next((line for text in lines for line in text.splitlines() if pattern.search(line)), "")


class Report:
    def __init__(self) -> None:
        self.failures = 0

    def add(self, status: str, check: str, detail: str = "") -> None:
        print(f"{status} {check}" + (f" ({detail})" if detail else ""))
        if status == "FAIL":
            self.failures += 1

    def verdict(self, passed: bool, check: str, detail: str = "") -> None:
        self.add("PASS" if passed else "FAIL", check, "" if passed else detail)


def shell_writes(transcript: Transcript, roles: tuple[str, ...]) -> str:
    lines = [f"{use.role} ran {use.input.get('command', '')}" for use in transcript.uses if use.role in roles and use.name == "Bash"]
    return first_matching_line(lines, SHELL_WRITE)


def check_take_over(report: Report, transcript: Transcript) -> None:
    manager = [use for use in transcript.uses if use.role == MANAGER]
    edited = next((use.file_path() for use in manager if use.is_edit() and use.file_path() and ".crew/" not in use.file_path()), "")
    check = f"{MANAGER} took over"
    if edited:
        report.add("FAIL", check, f"main session edited {edited}")
    elif not any(use.is_dispatch() for use in manager):
        report.add("FAIL", check, "no crew role dispatched")
    else:
        report.add("PASS", check)


def check_verdict(report: Report, transcript: Transcript, role: str, pattern: re.Pattern) -> None:
    if role == "qa" and not transcript.dispatches(role):
        report.add("SKIP", f"{role} verdict", "qa not dispatched")
        return
    report.verdict(bool(pattern.search(transcript.results_of(role))), f"{role} verdict")


def check_loop(report: Report, transcript: Transcript) -> None:
    nested = next((f"{use.role} started {use.subagent()}" for use in transcript.uses
                   if use.role != MANAGER and use.is_dispatch() and use.subagent() != ADVISOR), "")
    report.verdict(not nested, "no crew loop", nested)


def check_read_only(report: Report, transcript: Transcript) -> None:
    edited = next((f"{use.role} edited {use.file_path()}" for use in transcript.uses if use.role in READ_ONLY_ROLES and use.is_edit()), "")
    edited = edited or shell_writes(transcript, READ_ONLY_ROLES)
    report.verdict(not edited, "read-only roles changed nothing", edited)


def check_push(report: Report, transcript: Transcript) -> None:
    pushed = first_matching_line([str(use.input.get("command", "")) for use in transcript.uses if use.name == "Bash"], PUSH_PATTERN)
    report.verdict(not pushed, "nothing pushed", pushed)


def check_first_try(report: Report, transcript: Transcript) -> None:
    count = len(transcript.dispatches("worker"))
    report.verdict(count <= 1, "worker done on the first try", f"{count} worker dispatches")


def check_denials(report: Report, transcript: Transcript) -> None:
    denials = transcript.denials()
    if not denials:
        report.add("PASS", "no permission denials")
        return
    first = denials[0]
    target = tool_input(first).get("file_path") or tool_input(first).get("command") or ""
    role = transcript.role_of_use(str(first.get("tool_use_id") or ""))
    report.add("FAIL", "no permission denials", f"{len(denials)} denied, first: {role} {first.get('tool_name') or UNKNOWN} {target}".rstrip())


def main_ref() -> str:
    for branch in ("main", "master"):
        found = subprocess.run(["git", "rev-parse", "--verify", "-q", branch], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True)
        if found.returncode == 0:
            return found.stdout.strip()
    return ""


def check_main(report: Report, before: str) -> None:
    if not before:
        report.add("SKIP", "main untouched", "no --main-before")
    else:
        report.verdict(main_ref() == before, "main untouched", "main moved")


def evaluate(path: Path, main_before: str) -> int:
    transcript, report = Transcript(path), Report()
    uses = transcript.uses
    check_take_over(report, transcript)
    briefed = [use for use in transcript.dispatches("worker") if str(use.input.get("prompt", "")).startswith(WORKER_BRIEF)]
    report.verdict(bool(briefed), "worker dispatched")
    report.verdict(any(use.role == "worker" and use.is_edit() and use.file_path().endswith(TRIAL_FILE) for use in uses), "worker made the change")
    check_verdict(report, transcript, "reviewer", REVIEWER_VERDICT)
    check_verdict(report, transcript, "qa", QA_VERDICT)
    report.verdict(any(use.is_edit() and ".crew/items/" in use.file_path() for use in uses), "work item updated")
    check_loop(report, transcript)
    check_read_only(report, transcript)
    check_main(report, main_before)
    check_push(report, transcript)
    check_first_try(report, transcript)
    check_denials(report, transcript)
    print("\n".join(transcript.token_lines()))
    return 1 if report.failures else 0


def evaluate_question(path: Path) -> int:
    transcript, report = Transcript(path), Report()
    started = next((use.subagent() for use in transcript.uses if use.is_dispatch()), "")
    report.verdict(not started, "no crew role dispatched", started)
    changed = next((f"{use.role} edited {use.file_path()}" for use in transcript.uses if use.is_edit()), "")
    changed = changed or shell_writes(transcript, (MANAGER,))
    report.verdict(not changed, "no files changed", changed)
    print("\n".join(transcript.token_lines()))
    return 1 if report.failures else 0


def confirm_paid_run() -> bool:
    try:
        answer = input(f"This starts a real Claude Code session in {os.getcwd()} and uses your plan's usage. Continue? [y/N] ")
    except EOFError:
        answer = ""
    if answer.strip().lower() != "y":
        print("Cancelled.")
        return False
    return True


def outside_workspaces() -> list[str]:
    """Return ["--add-dir", <folder>] when the guidelines put crew workspaces outside the repo, so the trial may edit there."""
    found = WORKSPACES_LINE.search(GUIDELINES.read_text(errors="replace")) if GUIDELINES.is_file() else None
    if not found:
        return []
    folder = Path(found.group(1)).resolve()
    if folder == Path.cwd().resolve() or Path.cwd().resolve() in folder.parents:
        return []
    folder.mkdir(parents=True, exist_ok=True)
    return ["--add-dir", str(folder)]


def run_session(prompt: str) -> Path:
    handle, name = tempfile.mkstemp(prefix="crew-trial.", dir=os.environ.get("TMPDIR") or "/tmp")
    with os.fdopen(handle, "w") as output:
        subprocess.run([CLAUDE, "-p", prompt, "--output-format", "stream-json", "--verbose", "--permission-mode", "acceptEdits",
                        "--allowedTools", ALLOWED_TOOLS, "--disallowedTools", DISALLOWED_TOOLS, *outside_workspaces()], stdout=output)
    print(f"Transcript: {name}")
    return Path(name)


def git(*arguments: str) -> int:
    return subprocess.run(["git", *arguments]).returncode


def prepare_trial_branch() -> str:
    status = subprocess.run(["git", "status", "--porcelain"], stdout=subprocess.PIPE, text=True).stdout
    if status.strip():
        return "The repo has uncommitted changes. Commit or stash them first."
    if not Path(".crew/templates").is_dir():
        return "Missing .crew/templates/. Run the onboard-crew setup first."
    Path(".crew/items").mkdir(parents=True, exist_ok=True)
    Path(".crew/reviews").mkdir(parents=True, exist_ok=True)
    branch = f"crew-trial-{datetime.now():%Y%m%d%H%M%S}"
    if git("switch", "-q", "-c", branch) != 0:
        return f"Cannot create the branch {branch}."
    Path(TRIAL_FILE).parent.mkdir(parents=True, exist_ok=True)
    Path(TRIAL_FILE).write_text("This is teh crew trial file.\n")
    if git("add", TRIAL_FILE) != 0 or git("commit", "-q", "-m", "chore: add crew trial file") != 0:
        return "Cannot commit the trial file."
    print(f"Trial branch: {branch}")
    return ""


def claude_missing() -> bool:
    if shutil.which(CLAUDE) is None:
        print(f"{CLAUDE} not found on PATH. Install Claude Code first.")
        return True
    return False


def run_change_trial() -> int:
    if not confirm_paid_run() or claude_missing():
        return 2
    main_before = main_ref()
    problem = prepare_trial_branch()
    if problem:
        print(problem)
        return 2
    return evaluate(run_session(TRIAL_PROMPT), main_before)


def run_question_trial() -> int:
    if not confirm_paid_run() or claude_missing():
        return 2
    return evaluate_question(run_session(QUESTION_PROMPT))


def evaluate_given(arguments: list[str]) -> int:
    if not arguments or not Path(arguments[0]).is_file():
        print("Missing transcript file")
        return 2
    path, rest = Path(arguments[0]), arguments[1:]
    main_before, question = "", False
    while rest:
        option = rest.pop(0)
        if option == "--main-before":
            main_before = rest.pop(0) if rest else ""
        elif option == "--question":
            question = True
    return evaluate_question(path) if question else evaluate(path, main_before)


def main(arguments: list[str]) -> int:
    repo = arguments[0] if arguments else ""
    mode = arguments[1] if len(arguments) > 1 else ""
    if not repo or not Path(repo).is_dir():
        print(f"Cannot open repo '{repo}'")
        return 2
    rest = [os.path.abspath(arguments[2])] + arguments[3:] if mode == "--transcript" and len(arguments) > 2 else arguments[2:]
    os.chdir(repo)
    if mode == "--transcript":
        return evaluate_given(rest)
    if mode == "--run":
        return run_change_trial()
    if mode == "--run-question":
        return run_question_trial()
    if mode == "--prompt":
        print(TRIAL_PROMPT)
        return 0
    print(USAGE)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
