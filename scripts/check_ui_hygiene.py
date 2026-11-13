#!/usr/bin/env python3
"""Fail a build that adds an untranslated string, a layout literal or a findViewById.

Three hygiene budgets the project cannot regress. Each one has a baseline
file under `scripts/hygiene/`; the check compares the current tree against it and fails only on a
*new* offender. That is the honest gate: the existing debt is large and translating 1,348 strings
or removing 892 findViewById calls is not a single change, but nothing may make it larger.

  * `translation-baseline.txt`  — default strings with no translation in a locale. A new
    string must either be translated, marked `translatable="false"`, or be listed here on purpose.
  * `layout-literals-baseline.txt` — hardcoded `android:text` and colour literals in layouts. A new one must come from `@string` / the theme tokens.
  * `findviewbyid-baseline.txt` — one line per file and count. The total may only fall.

A fourth rule has no baseline, because it is not debt: a `padding(...)` argument may not be a
runtime value. Padding rejects a negative value with an `IllegalArgumentException` thrown during
composition, so a value that is negative on the first frame crashes the process on launch rather
than at the point of use. The Launch tab's floating changelog card did exactly that: its idle
float drifts between -3dp and +3dp and `.padding(top = float.dp)` threw before the card was ever
drawn. The correct modifier for a signed value is `offset`, which is what it now uses. Every
`padding` argument must be a numeric literal or a `GlowDimens` token.

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

# A `padding(...)` argument that is a runtime value: an identifier chain ending in `.dp` that is
# not a token holder. Padding must not be able to go negative, because it throws otherwise.
PADDING_RE = re.compile(r"\bpadding\s*\(")
DP_EXPR_RE = re.compile(r"([A-Za-z_][A-Za-z0-9_.]*)\.dp")
# Identifiers holding a fixed, non-negative token. These are safe in padding by construction.
TOKEN_HOLDERS = ("GlowDimens", "UiTokens", "CaramelDimens")


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


def _balanced_args(src: str, open_index: int) -> str:
    """The text between the `(` at open_index and its matching `)`, nesting-aware."""
    depth = 0
    for i in range(open_index, len(src)):
        char = src[i]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return src[open_index + 1:i]
    return src[open_index + 1:]


def dynamic_padding_offenders() -> list[str]:
    """A `padding(...)` argument that is not a numeric literal or a token constant.

    Padding rejects a negative value with an IllegalArgumentException thrown from the composition
    itself, so a negative there is a launch-time crash rather than a layout glitch. Only `padding`
    is checked: `size`/`width`/`height` legitimately take a computed value (a component's own
    `height` parameter, a measured width), and flagging those would be a false positive.
    """
    offenders: list[str] = []
    for path in sorted(JAVA.rglob("*.kt")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in PADDING_RE.finditer(text):
            args = _balanced_args(text, match.end() - 1)
            line = text[:match.start()].count("\n") + 1
            for name in DP_EXPR_RE.findall(args):
                if any(name.split(".")[0] == holder for holder in TOKEN_HOLDERS):
                    continue
                offenders.append(
                    f"{path.relative_to(ROOT)}:{line}:padding({name}.dp) is a runtime value"
                )
    return offenders


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


def compare_findviewbyid(current: list[str]) -> list[str]:
    """The findViewById budget is a count per file, so a *lower* count is progress, not a new
    offender. Comparing the raw lines treats "InstancesFragment 18" becoming "InstancesFragment
    10" as a new entry, which would make every conversion fail the gate that exists to encourage
    it. A file's count may only fall; a file that was not in the baseline may only appear below
    the ceiling its entry would have had, i.e. it must be genuinely new (not a rename)."""
    baseline: dict[str, int] = {}
    for line in load_baseline("findviewbyid-baseline.txt"):
        path, _, count = line.rpartition(" ")
        if count.isdigit():
            baseline[path] = int(count)
    offenders = []
    for line in current:
        path, _, count = line.rpartition(" ")
        if not count.isdigit():
            continue
        if int(count) > baseline.get(path, 0):
            offenders.append(line)
    return sorted(offenders)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--update", action="store_true",
                        help="rewrite the baselines from the current tree")
    args = parser.parse_args()

    translations = translation_offenders()
    literals = layout_literals()
    findviewbyid = findviewbyid_counts()
    dynamic_padding = dynamic_padding_offenders()

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
    ):
        new = compare(name, current, label)
        if new:
            failed = True
            print(f"\n{label}: {len(new)} NEW offender(s):")
            for entry in new[:40]:
                print(f"  {entry}")
            if len(new) > 40:
                print(f"  ... and {len(new) - 40} more")

    # The findViewById budget is a per-file count, so it is compared as counts: a conversion that
    # lowers a file's number is the point of the gate, not a new offender.
    baseline_files = {line.rpartition(" ")[0] for line in load_baseline("findviewbyid-baseline.txt")}
    current_files = {line.rpartition(" ")[0] for line in findviewbyid}
    for gone in sorted(baseline_files - current_files):
        print(f"findViewById: {gone} no longer calls findViewById "
              f"(run with --update to retire it)")
    new_findviewbyid = compare_findviewbyid(findviewbyid)
    if new_findviewbyid:
        failed = True
        print(f"\nfindViewById: {len(new_findviewbyid)} file(s) rose above their baseline:")
        for entry in new_findviewbyid[:40]:
            print(f"  {entry}")

    if dynamic_padding:
        failed = True
        print(f"\ndynamic padding: {len(dynamic_padding)} offender(s):")
        for entry in dynamic_padding:
            print(f"  {entry}")
        print("\nA padding argument must be a literal or a token: a runtime value can be "
              "negative, and padding throws on a negative value during composition. Use "
              "`offset` for a signed value.")

    if failed:
        print("\nA new offender must be fixed, marked translatable=\"false\", or added to the "
              "baseline with `--update` and a reason in the commit message.")
        return 1
    print("UI hygiene budgets are unchanged.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
