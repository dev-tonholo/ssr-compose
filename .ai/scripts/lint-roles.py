#!/usr/bin/env python3
"""Check crew role files: frontmatter values, the opening limit, the report field, the Project notes section,
Scope sections for specialists, and no host commands or tool paths.

Usage: python3 lint-roles.py <roles-folder>   (for example .ai/agents or the skill's assets/roles)
Exit codes: 0 ok, 1 problems found, 2 the folder cannot be opened.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

MANAGER = "engineering-manager"
ADVISOR = "advisor"
CORE_ROLES = "engineering-manager worker reviewer qa advisor designer".split()
LIMIT = "You never act as the engineering-manager and never dispatch crew roles. You may consult the advisor."
ADVISOR_LIMIT = "You never act as the engineering-manager and never dispatch crew roles. You never dispatch anyone."
REPORT_FIELD = "what slowed me down or went wrong"
PROJECT_NOTES = "## Project notes"
SPECIALIST_SECTIONS = ("## Scope", "## Out of scope")
HOST_COMMANDS = ("gh", "glab", "tea")
TOOL_PATHS = (".claude/worktrees", ".codex/", "~/.claude")
MAX_TIER = "max"
ALLOWED = {"tier": ("max", "top", "mid", "small"), "effort": ("low", "medium", "high"), "access": ("write", "read-only")}


def frontmatter(lines: list[str]) -> dict[str, str]:
    values: dict[str, str] = {}
    if not lines or lines[0] != "---":
        return values
    for line in lines[1:]:
        if line == "---":
            break
        key, separator, value = line.partition(":")
        if separator and key not in values:
            values[key] = value.lstrip(" ")
    return values


def lint_role(path: Path) -> list[str]:
    name = path.stem
    text = path.read_text(errors="replace").replace("\r\n", "\n")
    lines = text.splitlines()
    values = frontmatter(lines)
    problems = [f"{path.name}: bad {key}" for key, allowed in ALLOWED.items() if values.get(key, "") not in allowed]
    if values.get("tier") == MAX_TIER and name != ADVISOR:
        problems.append(f"{name}.md: tier {MAX_TIER} is for the {ADVISOR} only")
    if name == MANAGER and values.get("dispatchable") != "false":
        problems.append(f"{name}.md: {MANAGER} must have dispatchable: false")
    if name != MANAGER:
        expected = ADVISOR_LIMIT if name == ADVISOR else LIMIT
        if expected not in lines:
            problems.append(f"{name}.md: missing opening limit")
        if REPORT_FIELD not in text.lower():
            problems.append(f"{name}.md: missing report field '{REPORT_FIELD}'")
    if PROJECT_NOTES not in lines:
        problems.append(f"{name}.md: missing Project notes section")
    if name not in CORE_ROLES:
        problems += [f"{name}.md: missing {section[3:]} section" for section in SPECIALIST_SECTIONS if section not in lines]
    problems += [f"{name}.md: hard-coded host command '{command}'" for command in HOST_COMMANDS
                 if re.search(rf"(^|`){command} ", text, re.MULTILINE)]
    problems += [f"{name}.md: tool-specific path '{tool_path}'" for tool_path in TOOL_PATHS if tool_path in text]
    return problems


def main(arguments: list[str]) -> int:
    folder = Path(arguments[0]) if arguments else Path("")
    if not arguments or not folder.is_dir():
        print(f"Cannot open roles folder '{arguments[0] if arguments else ''}'")
        return 2
    problems = [problem for path in sorted(folder.glob("*.md")) if path.is_file() for problem in lint_role(path)]
    for problem in problems:
        print(problem)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
