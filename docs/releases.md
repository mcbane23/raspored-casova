# Releases

Every merge to `main` publishes a GitHub release (see README → Automatic releases) with the debug APK, the signed
AAB and a `raspored-casova-<version>-google-play.txt` file. The release page itself only shows GitHub's
automatically generated list of changes.

## Google Play texts

The .txt file holds everything Play Console asks for in plain text, Serbian first:

- the app name, short description and full description from [play-listing/sr/](play-listing/sr/) (each with its
  character count and Play's limit),
- "What's new" for this version, in the form Play Console accepts (max 500 characters per language):

```
<sr>
• ...
</sr>
<en-US>
• ...
</en-US>
```

The file is built by `.github/scripts/play_texts.py` in the "Write Google Play texts" workflow step. Texts over a
Play limit only produce a workflow warning. That step never fails the release; if it cannot run, the release is
published without the .txt file.

## Where "What's new" comes from

It is taken from the **Release notes** section of the merged pull request's description. The PR template
(`.github/pull_request_template.md`) already contains it:

```markdown
## Release notes
- SR: Podsetnik za torbu sada stiže tačno u minut.
- EN: The pack-the-bag reminder now arrives at the exact minute.
```

- Lines starting with `SR:` go into `<sr>`, lines starting with `EN:` into `<en-US>`, one bullet per line.
- Several lines per language are allowed; one short line per user-visible change is best.
- The section ends at the next heading. HTML comments are ignored.
- If the section is missing or a language has no lines, the PR title is used instead.

A direct push to `main` (no PR) falls back the same way. A manual workflow run reuses the notes of the PR that the
current `main` commit came from. The text for the first Play release is in
[google-play.md](google-play.md#release-notes-first-release).
