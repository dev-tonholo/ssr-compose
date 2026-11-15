#!/usr/bin/env python3
"""Merge a new role template into an existing crew role and print the result.

Keeps the role's tier and effort (when valid) and everything under its "## Project notes" heading;
everything else comes from the template, with <project> replaced.

Usage: python3 merge-role.py <template> <current-role> <project>
Exit codes: 0 ok, 2 a file cannot be read or an argument is missing.
"""
from __future__ import annotations

import sys
from pathlib import Path

PROJECT_NOTES = "## Project notes"
PROJECT_PLACEHOLDER = "<project>"
ALLOWED = {"tier": ("max", "top", "mid", "small"), "effort": ("low", "medium", "high")}
USAGE = "usage: merge-role.py <template> <current-role> <project>"


def read_lines(path: Path) -> list[str]:
    return path.read_text(errors="replace").replace("\r\n", "\n").replace("\r", "\n").splitlines()


def frontmatter_value(lines: list[str], key: str) -> str:
    if not lines or lines[0] != "---":
        return ""
    for line in lines[1:]:
        if line == "---":
            break
        if line.startswith(f"{key}:"):
            return line[len(key) + 1:].lstrip(" ")
    return ""


def kept_values(current: list[str]) -> dict[str, str]:
    values = {key: frontmatter_value(current, key) for key in ALLOWED}
    return {key: value for key, value in values.items() if value in ALLOWED[key]}


def notes_body(lines: list[str]) -> list[str] | None:
    if PROJECT_NOTES not in lines:
        return None
    return lines[lines.index(PROJECT_NOTES) + 1:]


def template_head(template: list[str], kept: dict[str, str], project: str) -> list[str]:
    head, closed = [], False
    for index, line in enumerate(template):
        if index > 0 and line == "---":
            closed = True
        key = line.partition(":")[0]
        if not closed and key in kept:
            line = f"{key}: {kept[key]}"
        head.append(line.replace(PROJECT_PLACEHOLDER, project))
        if line == PROJECT_NOTES:
            break
    return head


def main(arguments: list[str]) -> int:
    if len(arguments) != 3:
        print(USAGE, file=sys.stderr)
        return 2
    template_path, current_path, project = Path(arguments[0]), Path(arguments[1]), arguments[2]
    for path in (template_path, current_path):
        if not path.is_file():
            print(f"cannot read {path}", file=sys.stderr)
            return 2
    template, current = read_lines(template_path), read_lines(current_path)
    notes = notes_body(current)
    merged = template_head(template, kept_values(current), project) + (notes if notes is not None else notes_body(template) or [])
    print("\n".join(merged))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
