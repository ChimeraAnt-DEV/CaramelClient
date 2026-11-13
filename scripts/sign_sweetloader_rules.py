#!/usr/bin/env python3
"""Signs the sweetloader signature-rules data file.

The rule file is what decides which code addresses the sweetloader patches, so it is remote input
with the highest possible blast radius. It ships signed, and the client verifies the detached
signature with a public key it holds before installing anything.

The signature covers the exact bytes of the file. That is why this script signs a path rather
than a parsed document: re-serialising the JSON would change the bytes (key order, spacing) and
invalidate a signature that a human cannot see is wrong.

Usage:
    python3 scripts/sign_sweetloader_rules.py --key <private.pem> [--file <rules.json>] [--verify]

Without --file it signs resources/sweetloader/sweetloader_signature_rules_source.json and writes
resources/sweetloader/sweetloader_signature_rules_source.json.sig. --verify checks the existing
signature against the public key shipped in resources/sweetloader/sweetloader_rules_public_key.pem,
which is what CI runs.
"""
from __future__ import annotations

import argparse
import base64
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_FILE = ROOT / "resources/sweetloader/sweetloader_signature_rules_source.json"
PUBLIC_KEY = ROOT / "resources/sweetloader/sweetloader_rules_public_key.pem"


def _label(path: pathlib.Path) -> str:
    """A path for a message: relative to the repo when it is inside it, absolute otherwise.

    A caller can sign a file outside the repo (the host tests use a temporary directory), and
    relative_to() raises for those, so the message must not assume the file is in the tree.
    """
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def sign(key: pathlib.Path, payload: pathlib.Path, out: pathlib.Path) -> int:
    result = subprocess.run(
        ["openssl", "dgst", "-sha256", "-sign", str(key), "-out", str(out), str(payload)],
        capture_output=True,
    )
    if result.returncode != 0:
        print(result.stderr.decode("utf-8", "replace"), file=sys.stderr)
        return 1
    raw = out.read_bytes()
    # The client reads a base64 detached signature, which is the form every signing tool can
    # produce and the form that survives being pasted into a release asset.
    out.write_text(base64.b64encode(raw).decode("ascii") + "\n", encoding="utf-8")
    print(f"signed {_label(payload)} -> {_label(out)}")
    return 0


def verify(payload: pathlib.Path, sig: pathlib.Path, public_key: pathlib.Path) -> int:
    if not sig.exists():
        print(f"no signature at {sig}", file=sys.stderr)
        return 1
    raw = base64.b64decode(sig.read_text(encoding="utf-8").strip())
    tmp = sig.with_suffix(sig.suffix + ".bin")
    tmp.write_bytes(raw)
    try:
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-verify", str(public_key),
             "-signature", str(tmp), str(payload)],
            capture_output=True,
        )
    finally:
        tmp.unlink(missing_ok=True)
    if result.returncode != 0:
        print(result.stdout.decode("utf-8", "replace"), file=sys.stderr)
        return 1
    print(f"verified {_label(payload)} against {_label(public_key)}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--key", type=pathlib.Path, help="RSA private key PEM (signing)")
    parser.add_argument("--file", type=pathlib.Path, default=DEFAULT_FILE)
    parser.add_argument("--out", type=pathlib.Path)
    parser.add_argument("--verify", action="store_true",
                        help="verify the shipped signature instead of signing")
    args = parser.parse_args()

    payload = args.file if args.file.is_absolute() else (ROOT / args.file)
    out = args.out or payload.with_suffix(payload.suffix + ".sig")
    if not out.is_absolute():
        out = ROOT / out

    if args.verify:
        return verify(payload, out, PUBLIC_KEY)
    if args.key is None:
        parser.error("--key is required unless --verify is given")
    return sign(args.key, payload, out)


if __name__ == "__main__":
    raise SystemExit(main())
