#!/usr/bin/env python3
"""Builds the bilingual (Serbian, then English) note for a GitHub release.

Reads the merged pull request's title and body from the PR_TITLE and PR_BODY
environment variables and prints Markdown to stdout. The note comes from the
"Release notes" section of the PR body, where each line starts with "SR:" or
"EN:". Missing languages fall back to the PR title, so the note is never empty.
This script must never fail the release: any problem falls back to the title.
"""
import os
import re

HEADING = re.compile(r"^\s*#{1,6}\s*(.*?)\s*#*\s*$")
ITEM = re.compile(r"^\s*(?:[-*+]\s+)?(SR|EN)\s*:\s*(.*\S)?\s*$", re.IGNORECASE)


def parse(body):
    """Returns {"SR": [...], "EN": [...]} from the "Release notes" section."""
    notes = {"SR": [], "EN": []}
    body = re.sub(r"<!--.*?-->", "", body or "", flags=re.DOTALL)
    in_section = False
    for line in body.splitlines():
        heading = HEADING.match(line)
        if heading:
            in_section = heading.group(1).strip().lower() == "release notes"
            continue
        if not in_section:
            continue
        item = ITEM.match(line)
        if item and item.group(2):
            notes[item.group(1).upper()].append(item.group(2))
    return notes


def render(title, body):
    try:
        notes = parse(body)
    except Exception:  # never break the release over a malformed body
        notes = {"SR": [], "EN": []}
    fallback = (title or "").strip() or "Nova verzija / New version"
    out = []
    for lang, label in (("SR", "Srpski"), ("EN", "English")):
        lines = notes[lang] or [fallback]
        out.append(f"**{label}**")
        out.extend(f"- {line}" for line in lines)
        out.append("")
    return "\n".join(out)


if __name__ == "__main__":
    print(render(os.environ.get("PR_TITLE", ""), os.environ.get("PR_BODY", "")))
