#!/usr/bin/env python3
"""Insert localized strings into the values-<locale>/strings.xml files.

Keeps the locale files in step with the default strings so the UI-hygiene budget does not rise.
Escaping is done here rather than by hand: an unescaped apostrophe or a non-breaking space makes
aapt2 reject the whole file, which is a build break with a confusing message.

Usage:  python3 scripts/add_translations.py <json-file>
The JSON file is {"<string-name>": {"<locale>": "<value>", ...}, ...}.
"""
import json
import pathlib
import re
import sys


def escape(body: str) -> str:
    body = body.replace("\u00a0", " ")
    return re.sub(r"(?<!\\)'", r"\\'", body)


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    data = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
    for name, per_locale in data.items():
        for locale, value in per_locale.items():
            path = pathlib.Path(f"app/src/main/res/values-{locale}/strings.xml")
            if not path.exists():
                print(f"missing {path}")
                return 1
            text = path.read_text(encoding="utf-8")
            if f'name="{name}"' in text:
                print(f"present {locale}:{name}")
                continue
            entry = f'    <string name="{name}">{escape(value)}</string>\n'
            text = text.replace("</resources>", entry + "</resources>", 1)
            path.write_text(text, encoding="utf-8")
            print(f"added {locale}:{name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
