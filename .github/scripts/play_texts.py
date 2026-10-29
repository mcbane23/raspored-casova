#!/usr/bin/env python3
"""Builds the Google Play texts file attached to every GitHub release.

Prints plain text to stdout: the store listing (app name, short and full
description from docs/play-listing/sr/) followed by the "What's new" note for
this version in the <sr>/<en-US> form Play Console accepts. The note comes from
the merged PR's "Release notes" section, read from PR_TITLE and PR_BODY like
release_notes.py does. VERSION_NAME names the version in the header.
Limits are only warned about (GitHub Actions ::warning::), never enforced.
"""
import os
import sys
from pathlib import Path

from release_notes import parse

LISTING = Path(__file__).resolve().parents[2] / "docs" / "play-listing" / "sr"
# (file, heading, Play Console character limit)
FIELDS = (
    ("title.txt", "Naziv aplikacije", 30),
    ("short-description.txt", "Kratak opis", 80),
    ("full-description.txt", "Pun opis", 4000),
)
WHATS_NEW_LIMIT = 500


def warn(message):
    print(f"::warning::{message}", file=sys.stderr)


def whats_new(title, body):
    try:
        notes = parse(body)
    except Exception:  # never break the release over a malformed body
        notes = {"SR": [], "EN": []}
    fallback = (title or "").strip() or "Nova verzija / New version"
    out = []
    for lang, tag in (("SR", "sr"), ("EN", "en-US")):
        text = "\n".join(f"• {line}" for line in notes[lang] or [fallback])
        if len(text) > WHATS_NEW_LIMIT:
            warn(f"Play 'What's new' ({tag}) is {len(text)} characters, the limit is {WHATS_NEW_LIMIT}.")
        out += [f"<{tag}>", text, f"</{tag}>"]
    return "\n".join(out)


def render(version, title, body):
    header = " ".join(filter(None, ["Raspored časova", version]))
    out = [f"{header} – tekstovi za Google Play", ""]
    for name, heading, limit in FIELDS:
        text = (LISTING / name).read_text(encoding="utf-8").strip()
        if len(text) > limit:
            warn(f"Play listing {name} is {len(text)} characters, the limit is {limit}.")
        out += [f"=== {heading} (najviše {limit} znakova, ovde {len(text)}) ===", text, ""]
    out += [
        f"=== Šta je novo u ovoj verziji (najviše {WHATS_NEW_LIMIT} znakova po jeziku) ===",
        whats_new(title, body),
        "",
    ]
    return "\n".join(out)


if __name__ == "__main__":
    print(render(os.environ.get("VERSION_NAME", ""), os.environ.get("PR_TITLE", ""), os.environ.get("PR_BODY", "")), end="")
