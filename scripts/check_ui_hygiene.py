#!/usr/bin/env python3
"""Fail a build that adds an untranslated string, a layout literal or a findViewById.

Three hygiene budgets the project cannot regress (items 111, 136, 333). Each one has a baseline
file under `scripts/hygiene/`; the check compares the current tree against it and fails only on a
*new* offender. That is the honest gate: the existing debt is large and translating 1,348 strings
or removing 892 findViewById calls is not a single change, but nothing may make it larger.

  * `translation-baseline.txt`  — default strings with no translation in a locale (item 333). A new
    string must either be translated, marked `translatable="false"`, or be listed here on purpose.
  * `layout-literals-baseline.txt` — hardcoded `android:text` and colour literals in layouts
    (item 136). A new one must come from `@string` / the theme tokens.
  * `findviewbyid-baseline.txt` — one line per file and count (item 111). The total may only fall.

`--update` rewrites all three baselines, so the intended workflow is: make the change, run with
`--update`, and commit the baseline diff alongside it. That makes the diff reviewable rather than
hiding a new offender behind a silently raised number.

Usage:
  python3 scripts/check_ui_hygiene.py            # check (CI)
  python3 scripts/check_ui_hygiene.py --update   # rewrite the baselines
"""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
JAVA = ROOT / "app/src/main/java"
BASELINE_DIR = ROOT / "scripts/hygiene"

DEFAULT_STRINGS = RES / "values/strings.xml"
# Every locale the app ships. `values/` is the default and is not a translation target.
LOCALE_DIRS = [
    "values-es", "values-fr", "values-hi", "values-id", "values-ja",
    "values-pt", "values-ru", "values-tr", "values-vi", "values-zh-rCN",
]

STRING_RE = re.compile(r'<string name="([^"]+)"([^>]*)>', re.S)
PLURALS_RE = re.compile(r'<plurals name="([^"]+)"([^>]*)>', re.S)
# A literal `android:text="..."` that is not a resource or a data-binding expression.
TEXT_LITERAL_RE = re.compile(r'android:text="(?!@|\{)')
# A literal colour anywhere in a layout: `#RGB`, `#RRGGBB`, `#AARRGGBB`.
COLOR_LITERAL_RE = re.compile(r'"(#[0-9a-fA-F]{3,8})"')
FINDVIEWBYID_RE = re.compile(r"\bfindViewById\s*\(")


def read_strings(path: pathlib.Path) -> dict[str, bool]:
    """name -> translatable, from one strings.xml. Plurals count as strings for coverage."""
    if not path.is_file():
        return {}
    src = path.read_text(encoding="utf-8")
    out: dict[str, bool] = {}
    for regex in (STRING_RE, PLURALS_RE):
        for name, attrs in regex.findall(src):
            out[name] = 'translatable="false"' in attrs
    return out


def translation_offenders() -> list[str]:
    """Default strings that are translatable and absent from at least one shipped locale."""
    default = read_strings(DEFAULT_STRINGS)
    targets = {name: translatable for name, translatable in default.items() if not translatable}
    offenders: list[str] = []
    for locale in LOCALE_DIRS:
        have = set(read_strings(RES / locale / "strings.xml").keys())
        for name in sorted(targets):
            if name not in have:
                offenders.append(f"{locale}:{name}")
    return offenders


def layout_literals() -> list[str]:
    """Hardcoded text and colour literals in layouts, one entry per file and line."""
    offenders: list[str] = []
    for path in sorted((RES / "layout").glob("*.xml")):
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if TEXT_LITERAL_RE.search(line):
                offenders.append(f"{path.relative_to(ROOT)}:{number}:text")
            if COLOR_LITERAL_RE.search(line):
                offenders.append(f"{path.relative_to(ROOT)}:{number}:color")
    return offenders


def findviewbyid_counts() -> list[str]:
    """One `path count` line per source file that calls findViewById."""
    out: list[str] = []
    for path in sorted(JAVA.rglob("*")):
        if path.suffix not in (".java", ".kt"):
            continue
        count = len(FINDVIEWBYID_RE.findall(path.read_text(encoding="utf-8", errors="ignore")))
        if count:
            out.append(f"{path.relative_to(ROOT)} {count}")
    return out


def load_baseline(name: str) -> list[str]:
    path = BASELINE_DIR / name
    if not path.is_file():
        return []
    return [line.strip() for line in path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#")]


def write_baseline(name: str, lines: list[str], header: str) -> None:
    BASELINE_DIR.mkdir(parents=True, exist_ok=True)
    path = BASELINE_DIR / name
    path.write_text("# " + header + "\n" + "\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)} ({len(lines)} entries)")


def compare(name: str, current: list[str], label: str) -> list[str]:
    """Returns the new offenders, and reports how much of the budget was retired."""
    baseline = set(load_baseline(name))
    current_set = set(current)
    new = sorted(current_set - baseline)
    retired = sorted(baseline - current_set)
    if retired:
        print(f"{label}: {len(retired)} baseline entries no longer present "
              f"(run with --update to retire them)")
    return new


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--update", action="store_true",
                        help="rewrite the baselines from the current tree")
    args = parser.parse_args()

    translations = translation_offenders()
    literals = layout_literals()
    findviewbyid = findviewbyid_counts()

    if args.update:
        write_baseline("translation-baseline.txt", translations,
                       "Default strings with no translation in at least one shipped locale.")
        write_baseline("layout-literals-baseline.txt", literals,
                       "Hardcoded android:text and colour literals in res/layout.")
        write_baseline("findviewbyid-baseline.txt", findviewbyid,
                       "findViewById call count per source file.")
        return 0

    failed = False
    for name, current, label in (
        ("translation-baseline.txt", translations, "untranslated strings"),
        ("layout-literals-baseline.txt", literals, "layout literals"),
        ("findviewbyid-baseline.txt", findviewbyid, "findViewById"),
    ):
        new = compare(name, current, label)
        if new:
            failed = True
            print(f"\n{label}: {len(new)} NEW offender(s):")
            for entry in new[:40]:
                print(f"  {entry}")
            if len(new) > 40:
                print(f"  ... and {len(new) - 40} more")

    if failed:
        print("\nA new offender must be fixed, marked translatable=\"false\", or added to the "
              "baseline with `--update` and a reason in the commit message.")
        return 1
    print("UI hygiene budgets are unchanged.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
