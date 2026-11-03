#!/usr/bin/env python3
"""Split AGENTS.md into per-area docs under docs/agents/ and regenerate a one-page index.

AGENTS.md had grown past 2,500 lines with 97 sections, so the file a contributor is told
to read first was the last thing anyone would finish. The content is worth keeping; the shape is
not. This moves each `## ` section verbatim into `docs/agents/<slug>.md` and rewrites AGENTS.md as
an index that links to them.

Run it whenever AGENTS.md is edited:

    python3 scripts/split_agents_md.py

It is idempotent: the section bodies are the source of truth and are carried across unchanged, so
re-running after adding a section regenerates the index and writes the new file. Nothing here is a
one-off migration script - it is the tool that keeps the two in step, which is what
AgentsDocTest enforces.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AGENTS = ROOT / "AGENTS.md"
OUT_DIR = ROOT / "docs" / "agents"

HEADER = """# AGENTS.md

This is an index. The detail lives in `docs/agents/`, one file per area, and every entry below
links to it. Read the area you are about to change; the index is short on purpose so the first
thing a contributor opens is the map, not the territory.

`python3 scripts/split_agents_md.py` regenerates this index from the section files, and
`AgentsDocTest` fails the build if an entry here points at a file that does not exist or a section
file is missing from the index.

"""


def slug_for(heading: str, keep_parentheticals: bool = False) -> str:
    """A stable file name for a section heading.

    Parentheses are dropped rather than transliterated, because the slug is a link target a person
    reads: `one-design-system-colour-type-spacing-radius-motion` is easier to quote in a bug report
    than `one-design-system-u7u11`.

    Two headings can normalise to the same slug (this repo has two "Controller illustrations"
    sections). A bare `-2` suffix would not tell a reader which file is which, so a collision is
    retried with the parenthetical kept: the second section becomes
    `controller-illustrations-controllerillustrationview-controllerlayout`, which is long but says
    what is inside.
    """
    text = heading.strip()
    text = text.replace("\u2014", "-").replace("\u2013", "-")
    if not keep_parentheticals:
        text = re.sub(r"\(.*?\)", "", text)
    else:
        text = re.sub(r"[()]", " ", text)
    text = re.sub(r"[^0-9A-Za-z]+", "-", text)
    text = re.sub(r"-+", "-", text).strip("-").lower()
    return text or "section"


def split() -> None:
    text = AGENTS.read_text(encoding="utf-8")
    starts = [m.start() for m in re.finditer(r"(?m)^## ", text)]
    if not starts:
        sys.exit("no '## ' sections found in AGENTS.md; refusing to write an empty index")
    ends = starts[1:] + [len(text)]
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    blocks = []
    for start, end in zip(starts, ends):
        block = text[start:end].rstrip() + "\n"
        blocks.append((block.splitlines()[0][3:].strip(), block))

    # First pass: the short slug. Second pass, for a collision only: keep the parentheticals, which
    # is what distinguishes two sections with the same title. Third pass: a numeric suffix, which
    # should never be reached but keeps the script total if a future heading defeats both.
    slugs = [slug_for(h) for h, _ in blocks]
    for i, (heading, _) in enumerate(blocks):
        if slugs.count(slugs[i]) > 1:
            slugs[i] = slug_for(heading, keep_parentheticals=True)
    used = set()
    for i, (heading, _) in enumerate(blocks):
        candidate = slugs[i]
        suffix = 2
        while candidate in used:
            candidate = f"{slugs[i]}-{suffix}"
            suffix += 1
        used.add(candidate)
        slugs[i] = candidate

    entries = []
    for (heading, block), slug in zip(blocks, slugs):
        (OUT_DIR / f"{slug}.md").write_text(block, encoding="utf-8")
        entries.append((heading, slug))

    lines = [HEADER.rstrip(), ""]
    for heading, slug in entries:
        lines.append(f"- [{heading}](docs/agents/{slug}.md)")
    lines.append("")
    AGENTS.write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {len(entries)} sections to {OUT_DIR} and an index to {AGENTS}")


if __name__ == "__main__":
    split()
