#!/usr/bin/env python3
"""Tests for scripts/publish_emergency_news.py (the live emergency-news channel)."""

import datetime as dt
import json
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import publish_emergency_news as news

NOW = dt.datetime(2026, 12, 1, 12, 0, tzinfo=dt.timezone.utc)


def test_the_post_has_the_shape_the_client_parses() -> None:
    post = news.make_post("emergency", "Sign-in is down. We are on it.", now=NOW)
    assert post["severity"] == "emergency"
    assert post["title"] == "Sign-in is down"
    assert post["body"] == "Sign-in is down. We are on it."
    assert post["expiresAt"] == "2026-12-02T12:00:00Z"
    assert post["meta"] == "1 Dec 2026"
    assert post["id"].startswith("emergency-")


def test_the_same_message_publishes_once() -> None:
    message = "Marketplace is slow while we move a machine."
    first = news.make_post("urgent", message, now=NOW)
    again = news.make_post("urgent", message, now=NOW + dt.timedelta(hours=3))
    assert first["id"] == again["id"], "a republished message must not stack a second copy"
    # the severity is part of the identity: the same words as an emergency are a different post
    assert news.make_post("emergency", message, now=NOW)["id"] != first["id"]


def test_a_title_is_never_empty() -> None:
    post = news.make_post("urgent", "x" * 120, now=NOW)
    assert post["title"], "the client drops a post with an empty title"
    assert len(post["title"]) <= 60


def test_no_expiry_means_no_timestamp() -> None:
    post = news.make_post("urgent", "Read the release notes.", expires_hours=0, now=NOW)
    assert post["expiresAt"] == ""


def test_publishing_prepends_and_replaces_by_id() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "emergency_news.json"
        path.write_text(json.dumps({"schemaVersion": 1, "posts": []}), encoding="utf-8")

        older = news.make_post("urgent", "Older news.", now=NOW)
        news.write(news.publish(older, path), path)
        newer = news.make_post("emergency", "Newer news.", now=NOW)
        document = news.publish(newer, path)
        news.write(document, path)

        on_disk = json.loads(path.read_text(encoding="utf-8"))
        assert [p["id"] for p in on_disk["posts"]] == [newer["id"], older["id"]]
        assert on_disk["$comment"], "the document keeps the note a maintainer reads"
        assert on_disk["schemaVersion"] == 1


def test_the_document_stays_bounded() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "emergency_news.json"
        for i in range(news.MAX_POSTS + 3):
            document = news.publish(
                news.make_post("urgent", f"Message {i}.", now=NOW), path)
            news.write(document, path)
        on_disk = json.loads(path.read_text(encoding="utf-8"))
        assert len(on_disk["posts"]) == news.MAX_POSTS


def test_an_empty_message_is_refused() -> None:
    for bad in ("", "   "):
        try:
            news.make_post("urgent", bad, now=NOW)
        except ValueError:
            continue
        raise AssertionError("a post with no message must not be published")


def run_tests() -> int:
    test_the_post_has_the_shape_the_client_parses()
    test_the_same_message_publishes_once()
    test_a_title_is_never_empty()
    test_no_expiry_means_no_timestamp()
    test_publishing_prepends_and_replaces_by_id()
    test_the_document_stays_bounded()
    test_an_empty_message_is_refused()
    print("publish_emergency_news_test: all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(run_tests())
