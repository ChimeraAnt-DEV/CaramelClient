#!/usr/bin/env python3
"""Publish one emergency-news post into resources/launcher/emergency_news.json.

This is the half of the emergency channel that a workflow calls: a maintainer types a message,
this prepends it to the document the client already fetches at startup, and the commit publishes it
to every install on its next launch. No build, no release.

Three rules, each of which is a real failure mode:

  * the id is derived from the message, not from the time, so re-running the same announcement
    updates the existing post instead of stacking a second copy of it in front of the first;
  * a post with no title gets one derived from its body, because the client drops a post whose title
    is empty (and a dropped post is indistinguishable from a broken channel);
  * the document keeps its `$comment` and its schema, and `posts` never grows past MAX_POSTS —
    the client only ever shows the first three, so anything beyond that is dead weight in the repo.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCUMENT = ROOT / "resources/launcher/emergency_news.json"

SCHEMA_VERSION = 1
MAX_POSTS = 5
SEVERITIES = ("emergency", "urgent")

COMMENT = (
    "Announcements that ship without a build. Edit, commit to main, and every install shows them "
    "on the next launch. id must be stable (that is what de-duplicates a post across launches and "
    "keeps a dismissed-looking post from reappearing under a new id). expiresAt is ISO-8601 and "
    "optional; an empty string means no expiry. url is optional and opens in the launcher's own "
    "browser. severity is 'emergency' (something is broken now) or 'urgent' (read it soon); the "
    "client badges both and defaults an unknown value to urgent."
)


def post_id(severity: str, title: str, body: str) -> str:
    """A stable id for one announcement: same message, same id, however often it is published."""
    digest = hashlib.sha1(f"{severity}\n{title}\n{body}".encode("utf-8")).hexdigest()
    return f"{severity}-{digest[:12]}"


def derive_title(body: str) -> str:
    """The first sentence (or first 60 characters) of the body, for a post published without one."""
    first = body.strip().split(". ")[0].strip()
    if not first:
        first = body.strip()
    if len(first) > 60:
        first = first[:57].rstrip() + "..."
    return first


def make_post(severity: str, message: str, title: str = "", expires_hours: float = 24.0,
              banner: str = "", now: dt.datetime | None = None) -> dict:
    """One post in the shape the client parses."""
    if severity not in SEVERITIES:
        raise ValueError(f"severity must be one of {SEVERITIES}, not {severity!r}")
    body = (message or "").strip()
    if not body:
        raise ValueError("a post needs a message")
    headline = (title or "").strip() or derive_title(body)
    stamp = (now or dt.datetime.now(dt.timezone.utc)).astimezone(dt.timezone.utc)
    if expires_hours and expires_hours > 0:
        expires = (stamp + dt.timedelta(hours=expires_hours)).replace(microsecond=0)
        expires_at = expires.isoformat().replace("+00:00", "Z")
    else:
        expires_at = ""
    post = {
        "id": post_id(severity, headline, body),
        "severity": severity,
        "title": headline,
        "body": body,
        "meta": f"{stamp.day} {stamp.strftime('%b %Y')}",
        "expiresAt": expires_at,
        "banner": (banner or "").strip(),
    }
    return post


def load(path: pathlib.Path = DOCUMENT) -> dict:
    if not path.exists():
        return {"$comment": COMMENT, "schemaVersion": SCHEMA_VERSION, "posts": []}
    document = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(document, dict):
        raise ValueError("the emergency news document is not an object")
    document.setdefault("$comment", COMMENT)
    document["schemaVersion"] = SCHEMA_VERSION
    posts = document.get("posts")
    document["posts"] = posts if isinstance(posts, list) else []
    return document


def publish(post: dict, path: pathlib.Path = DOCUMENT) -> dict:
    """Prepend `post`, replacing any post with the same id, and trim to MAX_POSTS."""
    document = load(path)
    kept = [p for p in document["posts"] if isinstance(p, dict) and p.get("id") != post["id"]]
    document["posts"] = [post] + kept
    document["posts"] = document["posts"][:MAX_POSTS]
    return document


def write(document: dict, path: pathlib.Path = DOCUMENT) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    # The key order is the one a maintainer reads: the comment, the schema, then the posts.
    ordered = {"$comment": document["$comment"], "schemaVersion": document["schemaVersion"],
               "posts": document["posts"]}
    path.write_text(json.dumps(ordered, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Publish an emergency-news post.")
    parser.add_argument("--severity", required=True, choices=SEVERITIES)
    parser.add_argument("--message", required=True)
    parser.add_argument("--title", default="")
    parser.add_argument("--expires-hours", type=float, default=24.0)
    parser.add_argument("--banner", default="")
    parser.add_argument("--file", default=str(DOCUMENT))
    args = parser.parse_args(argv)

    post = make_post(args.severity, args.message, args.title, args.expires_hours, args.banner)
    path = pathlib.Path(args.file)
    document = publish(post, path)
    write(document, path)
    print(f"Published {post['id']} ({post['severity']}): {post['title']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
