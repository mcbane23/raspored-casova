# Release notes

Every merge to `main` publishes a GitHub release (see README → Automatic releases). Each release starts with a short
note in Serbian and then English, followed by GitHub's automatically generated list of changes.

## Where the note comes from

The note is taken from the **Release notes** section of the merged pull request's description. The PR template
(`.github/pull_request_template.md`) already contains it:

```markdown
## Release notes
- SR: Podsetnik za torbu sada stiže tačno u minut.
- EN: The pack-the-bag reminder now arrives at the exact minute.
```

- Lines starting with `SR:` go under **Srpski**, lines starting with `EN:` under **English**; the release shows
  Serbian first.
- Several lines per language are allowed; one short line per user-visible change is best.
- The section ends at the next heading. HTML comments are ignored.
- If the section is missing or a language has no lines, the PR title is used instead, so a release is never empty.

The note is built by `.github/scripts/release_notes.py` in the "Write release note" workflow step. That step never
fails the release: if the PR cannot be found or parsed, the release still gets a generic note.

Releases from a direct push to `main` (no PR) or from a manual workflow run fall back the same way.

## Google Play "What's new"

Play Console asks for its own release notes (max 500 characters per language) when a new AAB is uploaded. The
`SR:`/`EN:` lines can be pasted there in this form:

```
<sr>
• ...
</sr>
<en-US>
• ...
</en-US>
```

The text for the first Play release is in [google-play.md](google-play.md#release-notes-first-release).
