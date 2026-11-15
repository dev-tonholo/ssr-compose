#!/usr/bin/env python3
"""Check that every relative Markdown link in .ai/, AGENTS.md and the ADR folder points to an existing file.

The ADR folder is docs/adr/, or else the first folder named adr, one or two levels below the repo root,
that holds 0000-template.md.

Usage: python3 .ai/scripts/check-links.py   (run from anywhere; the repo is two folders above this script)
Exit codes: 0 all links resolve, 1 broken links found, 2 bad arguments.
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

SCANNED_FOLDER = ".ai"
DEFAULT_ADR_FOLDER = "docs/adr"
ADR_TEMPLATE_PATTERNS = ("*/adr/0000-template.md", "*/*/adr/0000-template.md")
SCANNED_FILES = ("AGENTS.md",)
LINK = re.compile(r"\]\(([^)]+)\)")
SKIPPED_PREFIXES = ("http:", "https:", "mailto:", "#")


def path_exists(path: Path) -> bool:
    try:
        return path.exists()
    except OSError:
        return False


def repo_root() -> Path:
    return Path(os.path.abspath(__file__)).parent.parent.parent


def adr_folder(root: Path) -> str:
    """Return the repo's ADR folder relative to root; docs/adr when none is found."""
    if (root / DEFAULT_ADR_FOLDER).is_dir():
        return DEFAULT_ADR_FOLDER
    templates = sorted(path for pattern in ADR_TEMPLATE_PATTERNS for path in root.glob(pattern))
    visible = [path for path in templates if not any(part.startswith(".") for part in path.relative_to(root).parts)]
    return visible[0].parent.relative_to(root).as_posix() if visible else DEFAULT_ADR_FOLDER


def markdown_files(root: Path) -> list[Path]:
    files = [path for folder in (SCANNED_FOLDER, adr_folder(root)) for path in sorted((root / folder).rglob("*.md"))]
    return files + [root / name for name in SCANNED_FILES if (root / name).exists()]


def relative_targets(text: str) -> list[str]:
    targets = (match.split("#", 1)[0] for match in LINK.findall(text) if not match.startswith(SKIPPED_PREFIXES))
    return [target for target in targets if target]


def main(arguments: list[str]) -> int:
    if arguments:
        print(f"Unknown option '{arguments[0]}'")
        return 2
    root = repo_root()
    checked = broken = 0
    for file in markdown_files(root):
        for target in relative_targets(file.read_text(errors="replace")):
            checked += 1
            if not path_exists(file.parent / target):
                print(f"BROKEN: {file.relative_to(root)} -> {target}")
                broken += 1
    print(f"Checked {checked} relative links, {broken} broken.")
    return 1 if broken else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
