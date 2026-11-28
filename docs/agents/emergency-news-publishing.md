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
- **A post names its banner, the client supplies the picture:** `"banner"` takes one of a fixed
  set of keys — `emergency`, `urgent`, `christmas`, `caramel_plus` (also `caramel+` / `plus`),
  `berryapi` — and `NewsBanners.drawableFor` maps it to a bundled drawable. A document that is
  fetched cannot name a resource id (that would be an instruction, not data), so the keys are the
  whole vocabulary; an unknown key falls back to the art its severity implies and a post with
  neither keeps the card's gradient. The changelog feed's sections name keys the same way.
- **Published notices come first.** `MainActivity.buildNews()` starts the list with the announcements
  and appends the changelog and bundled rows, so the News tab's featured card is an outage notice
  when there is one, and the Home strip's first cards are too. The strip shows the first three rows.
- **The document also ships with the build.** `resources/launcher/emergency_news.json` is an assets
  root, so a fresh install with no cache yet (first launch, or a fetch that failed before any post
  was cached) reads the packaged copy — the channel works before the network does, and a notice can
  be baked into a release as well as published to `main`.
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
- **What is still manual:** a *new* banner picture. The published document names one of the bundled
  keys, so shipping a new picture means adding a drawable and a key to `NewsBanners` in a build; the
  art itself is never fetched.
- **Verified on device (Mobilebox, `caramel-test`, arm64):** with a post per key in the packaged
  document, the News tab opened on the EMERGENCY hero, the URGENT post sat under it, and the
  Christmas and Caramel+ posts drew their thumbs in the modal; the Home strip led with the emergency
  and urgent cards. The card art is drawn fit-centre over a blurred fill, so a wide picture is shown
  whole instead of cropped at phone width.
