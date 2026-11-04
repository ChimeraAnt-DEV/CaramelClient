#!/usr/bin/env python3
"""Pull the sneak peeks the team posts in the Discord channel into the app's feed.

The in-app changelog shows a "Sneak peeks" section. Rather than hand-copy images, this reads the
channel's recent messages with a bot token and downloads each image attachment the maintainer
posted, then rewrites `resources/launcher/changelog_feed.json`'s `sneakPeeks` list. The images land
in `docs/assets/sneakpeeks/`, which `app/build.gradle` copies into the APK.

Usage:
    DISCORD_BOT_TOKEN=... python3 scripts/fetch_discord_sneak_peeks.py [--channel ID] [--limit N]

The bot must be a member of the guild with **View Channel** and **Read Message History** on the
channel. A webhook cannot do this: it is write-only.

Nothing here is destructive beyond the two paths it owns (the images directory and the feed's
`sneakPeeks` array); a failure leaves the existing feed in place.
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
FEED = ROOT / "resources" / "launcher" / "changelog_feed.json"
IMAGES = ROOT / "docs" / "assets" / "sneakpeeks"
API = "https://discord.com/api/v10"
# A desktop UA: Discord rejects some default library agents with a 403.
UA = "GlowberryClient/1.0 (+https://github.com/ChimeraAnt-DEV/GlowberryClient)"
DEFAULT_CHANNEL = "1553866091670012005"
# Only the maintainer's posts are peeks; a random member's screenshot is not.
AUTHOR = "chimera.ant.king"
MAX_PEEKS = 10
MIN_WIDTH = 400


def get_json(url: str, token: str):
    req = urllib.request.Request(url, headers={
        "Authorization": f"Bot {token}", "User-Agent": UA,
    })
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def download(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def clean_caption(text: str) -> str:
    text = re.sub(r"<@[^>]+>", "", text or "")
    text = text.replace("@everyone", "").replace("@here", "")
    text = re.sub(r"\*\*|```|__|[*_`]", "", text)
    return re.sub(r"\s+", " ", text).strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--channel", default=DEFAULT_CHANNEL)
    ap.add_argument("--limit", type=int, default=50, help="messages to scan")
    ap.add_argument("--max-peeks", type=int, default=MAX_PEEKS)
    args = ap.parse_args()

    token = os.environ.get("DISCORD_BOT_TOKEN", "").strip()
    if not token:
        print("DISCORD_BOT_TOKEN is not set", file=sys.stderr)
        return 2

    messages = get_json(f"{API}/channels/{args.channel}/messages?limit={args.limit}", token)
    if not isinstance(messages, list):
        print(f"Discord refused the read: {messages}", file=sys.stderr)
        return 1

    IMAGES.mkdir(parents=True, exist_ok=True)
    peeks = []
    for message in messages:                      # newest first
        if message.get("author", {}).get("username") != AUTHOR:
            continue
        caption = clean_caption(message.get("content", ""))
        for att in message.get("attachments", []):
            content_type = att.get("content_type") or ""
            # Static images only: the feed renders a still, and a GIF would need its own view.
            if not content_type.startswith("image/") or content_type == "image/gif":
                continue
            if (att.get("width") or 0) < MIN_WIDTH:
                continue
            name = f"peek_{len(peeks) + 1:02d}.png"
            try:
                IMAGES.joinpath(name).write_bytes(download(att["url"]))
            except Exception as error:            # noqa: BLE001 - report and keep the rest
                print(f"skip {name}: {error}", file=sys.stderr)
                continue
            peeks.append({
                "caption": caption[:90] or att.get("filename", name),
                "image": name,
                "source": "discord",
                "messageId": message.get("id", ""),
            })
            if len(peeks) >= args.max_peeks:
                break
        if len(peeks) >= args.max_peeks:
            break

    if not peeks:
        print("no peeks found; the feed is left unchanged", file=sys.stderr)
        return 1

    feed = json.loads(FEED.read_text(encoding="utf-8"))
    feed["sneakPeeks"] = peeks
    FEED.write_text(json.dumps(feed, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"wrote {len(peeks)} sneak peeks to {FEED.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
