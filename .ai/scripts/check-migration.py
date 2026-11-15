#!/usr/bin/env python3
"""Prove a crew migration ledger accounts for every non-blank line of the old agent files.

Usage: python3 check-migration.py <ledger> <snapshot-dir>
  <ledger>        .crew/migration.md, a table: | File | Lines | Kind | Destination |
  <snapshot-dir>  the copies made by find-agents.py --snapshot, at their repo-relative paths
Exit codes: 0 ok, 1 problems found, 2 a missing input.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

KINDS = ("frontmatter", "role-covered", "role-kept", "role-upstream", "fact", "knowledge", "copy", "unknown")
OWNER_KIND = "unknown"
COPY_KIND = "copy"
RANGE = re.compile(r"^(\d+)(?:-(\d+))?$")
COPY_DESTINATION = re.compile(r"^([^:]+):(\d+)$")
CELL_PADDING = " \t`"
LEDGER_CELLS = 6
MAX_SNAPSHOT_FILE_BYTES = 1024 * 1024
REPOSITORY_MARKER = ".git"


def is_file(path: Path) -> bool:
    try:
        return path.is_file()
    except OSError:
        return False


def file_lines(path: Path) -> list[str]:
    text = path.read_bytes().decode("utf-8", errors="replace")
    lines = text.split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    return [line.rstrip("\r") for line in lines]


def ledger_rows(ledger: Path) -> list[tuple[str, str, str, str]]:
    rows = []
    for line in file_lines(ledger):
        if not line.startswith("| ") or line.startswith("| File |") or line.startswith("|---"):
            continue
        cells = [cell.strip(CELL_PADDING) for cell in line.split("|")]
        cells += [""] * (LEDGER_CELLS - len(cells))
        rows.append((cells[1], cells[2], cells[3], cells[4]))
    return rows


class Ledger:
    def __init__(self, snapshot: Path) -> None:
        self.snapshot = snapshot
        self.problems: list[str] = []
        self.covered: set[tuple[str, int]] = set()

    def problem(self, message: str) -> None:
        self.problems.append(message)

    def lines_of(self, relative: str) -> list[str]:
        path = self.snapshot / relative
        if is_file(path) and path.stat().st_size > MAX_SNAPSHOT_FILE_BYTES:
            message = f"{relative} is larger than 1 MiB, so it is not an agent file"
            if message not in self.problems:
                self.problem(message)
            return []
        return file_lines(path)

    def parse_range(self, file: str, text: str) -> tuple[int, int] | None:
        match = RANGE.match(text)
        if not match:
            self.problem(f"{file}: bad line range '{text}'")
            return None
        first = int(match.group(1))
        return first, int(match.group(2) or first)

    def record_range(self, file: str, text: str) -> None:
        bounds = self.parse_range(file, text)
        if bounds is None:
            return
        first, last = bounds
        count = len(self.lines_of(file))
        if first < 1 or last < first or last > count:
            self.problem(f"{file} has {count} lines, row says {text}")
            return
        self.covered.update((file, line) for line in range(first, last + 1))

    def check_copy(self, file: str, text: str, destination: str) -> None:
        match = COPY_DESTINATION.match(destination)
        if not match:
            self.problem(f"{file} {text}: copy destination must be <file>:<line>")
            return
        source, start = match.group(1), int(match.group(2))
        if not is_file(self.snapshot / source):
            self.problem(f"{file} {text}: copy source {source} is not in the snapshot")
            return
        bounds = RANGE.match(text)
        if not bounds:
            return
        first = int(bounds.group(1))
        last = int(bounds.group(2) or first)
        copy, original = self.lines_of(file), self.lines_of(source)
        for offset in range(last - first + 1):
            line, source_line = first + offset, start + offset
            copied = copy[line - 1] if line <= len(copy) else None
            expected = original[source_line - 1] if source_line <= len(original) else None
            if copied != expected:
                self.problem(f"{file}:{line} is not a copy of {source}:{source_line}")

    def check_row(self, file: str, text: str, kind: str, destination: str) -> None:
        if kind not in KINDS:
            self.problem(f"{file} {text}: bad kind '{kind}'")
        if kind == OWNER_KIND:
            self.problem(f"{file} {text}: kind unknown needs an owner decision")
        if not destination:
            self.problem(f"{file} {text}: no destination")
        if not is_file(self.snapshot / file):
            self.problem(f"{file} is not in the snapshot")
            return
        self.record_range(file, text)
        if kind == COPY_KIND:
            self.check_copy(file, text, destination)

    def check_coverage(self) -> None:
        for path in sorted(path for path in self.snapshot.rglob("*") if path.is_file()):
            relative = path.relative_to(self.snapshot).as_posix()
            for number, line in enumerate(self.lines_of(relative), 1):
                if line.strip() and (relative, number) not in self.covered:
                    self.problem(f"{relative}:{number} is not in the ledger")


def main(arguments: list[str]) -> int:
    ledger_path = Path(arguments[0]) if arguments else Path("")
    snapshot = Path(arguments[1]) if len(arguments) > 1 else Path("")
    if not arguments or not ledger_path.is_file():
        print(f"Cannot read ledger '{arguments[0] if arguments else ''}'")
        return 2
    if len(arguments) < 2 or not snapshot.is_dir():
        print(f"Cannot open snapshot '{arguments[1] if len(arguments) > 1 else ''}'")
        return 2
    if (snapshot / REPOSITORY_MARKER).exists():
        print(f"Snapshot '{snapshot}' looks like a repository; pass the folder find-agents.py --snapshot wrote")
        return 2
    ledger = Ledger(snapshot)
    for row in ledger_rows(ledger_path):
        ledger.check_row(*row)
    ledger.check_coverage()
    for problem in ledger.problems:
        print(problem)
    if not ledger.problems:
        print("ok")
    return 1 if ledger.problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
