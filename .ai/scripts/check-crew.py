#!/usr/bin/env python3
"""Check the crew's local work items and reviews in .crew/, that .crew/worktrees/ is ignored by git,
that every specialist in the guidelines has a role file and a knowledge skill,
and that each .crew/seats line is <role>:<driver|passenger> for a real role.

Usage: python3 check-crew.py [repo-path]   (default: two folders above this script)
Exit codes: 0 ok, 1 problems found, 2 the repo cannot be opened.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

ITEMS_DIR = Path(".crew/items")
REVIEWS_DIR = Path(".crew/reviews")
WORKTREES_PROBE = ".crew/worktrees/probe"
REQUIRED_FIELDS = ("status", "blocked-by", "branch", "owner", "size")
STATUSES = ("open", "in-progress", "blocked", "in-review", "done")
SIZES = ("trivial", "small", "medium", "complex")
CHANGES_REQUESTED = "Verdict: CHANGES REQUESTED"
GUIDELINES = Path(".ai/guidelines.md")
AGENTS_DIR = Path(".ai/agents")
SPECIALISTS_HEADING = "### Specialists"
CORE_ROLES = "engineering-manager worker reviewer qa advisor designer".split()
SEATS = ("driver", "passenger")
SEATS_FILE = Path(".crew/seats")
SEAT_LINE = re.compile(r"^([a-z0-9-]+):([a-z]+)$")
REVIEW_ROUND = re.compile(r"-(\d+)\.md$")
SPECIALIST_COLUMNS = 5


def is_folder(path: Path) -> bool:
    try:
        return path.is_dir()
    except OSError:
        return False


def read_lines(path: Path) -> list[str]:
    return path.read_text(errors="replace").replace("\r\n", "\n").splitlines()


def header(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in read_lines(path)[1:]:
        if line == "---":
            break
        key, separator, value = line.partition(":")
        if separator and key not in values:
            values[key] = value.lstrip(" ")
    return values


class Checker:
    def __init__(self) -> None:
        self.problems: list[str] = []

    def problem(self, message: str) -> None:
        print(message)
        self.problems.append(message)

    def check_item(self, item: Path) -> None:
        values = header(item)
        for field in REQUIRED_FIELDS:
            if field not in values:
                self.problem(f"{item}: missing {field}")
        for field, allowed in (("status", STATUSES), ("size", SIZES)):
            if field in values and values[field] not in allowed:
                self.problem(f"{item}: bad {field} '{values[field]}'")
        for blocker in values.get("blocked-by", "").replace(",", " ").split():
            if not any(ITEMS_DIR.glob(f"{blocker}-*.md")):
                self.problem(f"{item}: blocked-by {blocker} has no item")
        if values.get("status") == "done":
            self.check_done_review(item)

    def check_done_review(self, item: Path) -> None:
        item_id = item.name.split("-", 1)[0]
        rounds = [(int(match.group(1)), path) for path in REVIEWS_DIR.glob(f"{item_id}-*.md")
                  if (match := REVIEW_ROUND.search(path.name))] if REVIEWS_DIR.is_dir() else []
        if rounds and CHANGES_REQUESTED in max(rounds)[1].read_text(errors="replace"):
            self.problem(f"{item}: done but last review says CHANGES REQUESTED")

    def check_worktrees_ignored(self) -> None:
        ignored = subprocess.run(["git", "check-ignore", "-q", WORKTREES_PROBE], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if ignored.returncode != 0:
            self.problem(".crew/worktrees is not ignored by git")

    def specialist_rows(self) -> list[tuple[str, str]]:
        rows, inside = [], False
        for line in read_lines(GUIDELINES):
            if line == SPECIALISTS_HEADING:
                inside = True
            elif inside and line.startswith("#"):
                break
            elif inside and line.startswith("| ") and not line.startswith("| Role |"):
                cells = [cell.strip().strip("`").strip() for cell in line.split("|")]
                cells += [""] * (SPECIALIST_COLUMNS - len(cells))
                if cells[1]:
                    rows.append((cells[1], cells[3]))
        return rows

    def check_specialists(self) -> None:
        if not GUIDELINES.is_file():
            return
        sections = read_lines(GUIDELINES).count(SPECIALISTS_HEADING)
        if sections == 0:
            return
        if sections > 1:
            self.problem(f"{GUIDELINES} has {sections} Specialists sections; keep one")
        listed = []
        for role, skill in self.specialist_rows():
            listed.append(role)
            if role in CORE_ROLES:
                self.problem(f"specialist {role}: name is a core role")
                continue
            if not (AGENTS_DIR / f"{role}.md").is_file():
                self.problem(f"specialist {role} has no {AGENTS_DIR}/{role}.md")
            if not skill or not is_folder(Path(skill)):
                self.problem(f"specialist {role}: knowledge skill {skill} not found")
        for path in sorted(AGENTS_DIR.glob("*.md")):
            if path.name.startswith("."):
                continue
            if path.stem not in CORE_ROLES and path.stem not in listed:
                self.problem(f"{path.name} is not listed under Specialists")

    def check_seats(self) -> None:
        if not SEATS_FILE.is_file():
            return
        seen = []
        for number, raw in enumerate(read_lines(SEATS_FILE), 1):
            line = "".join(raw.split())
            if not line:
                continue
            match = SEAT_LINE.match(line)
            if not match:
                self.problem(f"{SEATS_FILE} line {number}: expected <role>:<driver|passenger>")
                continue
            role, seat = match.groups()
            if seat not in SEATS:
                self.problem(f"{SEATS_FILE} line {number}: bad seat '{seat}'")
            if not (AGENTS_DIR / f"{role}.md").is_file():
                self.problem(f"{SEATS_FILE} line {number}: no role {AGENTS_DIR}/{role}.md")
            if role in seen:
                self.problem(f"{SEATS_FILE} line {number}: {role} is listed twice")
            seen.append(role)


def main(arguments: list[str]) -> int:
    repo = Path(arguments[0]) if arguments else Path(os.path.abspath(__file__)).parent.parent.parent
    if not repo.is_dir():
        print(f"Cannot open {repo}")
        return 2
    os.chdir(repo)
    checker = Checker()
    for item in sorted(ITEMS_DIR.glob("*.md")):
        checker.check_item(item)
    checker.check_worktrees_ignored()
    checker.check_specialists()
    checker.check_seats()
    return 1 if checker.problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
