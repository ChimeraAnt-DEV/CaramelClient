## Emergency news: publishing a notice without shipping a build

The channel is one document on the same repository the client already reads its rule set from, so a
notice about a broken release reaches every install on its next launch instead of waiting for the
next APK.

- **Document:** `resources/launcher/emergency_news.json` (`schemaVersion`, `posts[]`), fetched from
  `EmergencyNews.SOURCE_URL` and cached in the app's `files/` so a failed fetch keeps the last good
  post rather than emptying the tab.
- **Severity is published, never guessed:** `"severity": "emergency" | "urgent"`. `NewsSeverity`
  reads it case-insensitively and treats a missing or unknown value as `urgent` — under-reporting an
  outage is worse than an extra badge. The client draws the badge red for emergency, amber for urgent,
  on the Home card and in the News modal (`CaramelNewsInfo.severityLabel` / `.emergency`). Strings are
  `news_severity_emergency` / `news_severity_urgent`, translated in all 10 locales.
- **Two dispatchable workflows, both writing that one document:**
  - `.github/workflows/emergency-news.yml` — severity `emergency`, default expiry 24h.
  - `.github/workflows/urgent-news.yml` — severity `urgent`, default expiry 72h.
  Both take a `message` (required), optional `title`, `expires_hours` (0 = no expiry) and `banner`
  (reserved; the client falls back to its own gradient, because a remote document cannot ship a
  drawable). They commit to `main` with `GITHUB_TOKEN`, so no build runs and no release is cut.
- **The writer is `scripts/publish_emergency_news.py`, and it is the rule, not a convenience:**
  - the id is `sha1(severity + title + body)[:12]`, so re-running the same announcement updates the
    post instead of stacking a duplicate in front of it;
  - a post with no title derives one from its first sentence, because the client silently drops a
    post whose title is empty;
  - `posts` is capped at `MAX_POSTS` (5) — the client shows the first three, so anything beyond is
    dead weight in the repository.
  `scripts/tests/publish_emergency_news_test.py` pins all three, and runs in `android.yml`.
- **Client-side parse rules** (pinned by `EmergencyNewsTest`): a post needs a title and a body, an
  expired post is dropped at read time, a repeated id is shown once, and a document that is not JSON
  yields no posts rather than an exception in the News tab. The text is displayed, never executed, and
  a post's only URL opens in the launcher's own browser.
- **What is still manual:** the banner asset. The `banner` field is carried through the document and
  ignored by the client until a drawable-shipping route exists; today a post without art keeps the
  generated gradient rather than showing a hole.
