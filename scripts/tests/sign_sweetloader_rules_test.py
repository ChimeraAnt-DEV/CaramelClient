#!/usr/bin/env python3
"""Host tests for scripts/sign_sweetloader_rules.py.

The signer is what keeps the shipped rule signature honest. These tests exercise the pure parts
(argument handling, base64 round-trip, the tamper case) with a throwaway key pair generated with
openssl, so a broken signer fails CI rather than shipping a signature the client refuses.
"""
from __future__ import annotations

import base64
import importlib.util
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent


def load():
    spec = importlib.util.spec_from_file_location(
        "sign_sweetloader_rules", ROOT / "scripts/sign_sweetloader_rules.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_sign_then_verify_round_trip():
    module = load()
    with tempfile.TemporaryDirectory() as tmp:
        tmp = pathlib.Path(tmp)
        key = tmp / "key.pem"
        subprocess.run(["openssl", "genpkey", "-algorithm", "RSA",
                        "-pkeyopt", "rsa_keygen_bits:2048", "-out", str(key)],
                       check=True, capture_output=True)
        payload = tmp / "rules.json"
        payload.write_text('{"revision":7,"rules":[]}', encoding="utf-8")
        sig = tmp / "rules.json.sig"

        assert module.sign(key, payload, sig) == 0
        raw = base64.b64decode(sig.read_text(encoding="utf-8").strip())
        assert len(raw) == 256, "a 2048-bit RSA signature is 256 bytes"

        public = tmp / "key.pub"
        subprocess.run(["openssl", "rsa", "-in", str(key), "-pubout", "-out", str(public)],
                       check=True, capture_output=True)
        assert module.verify(payload, sig, public) == 0


def test_verify_refuses_a_tampered_payload():
    module = load()
    with tempfile.TemporaryDirectory() as tmp:
        tmp = pathlib.Path(tmp)
        key = tmp / "key.pem"
        subprocess.run(["openssl", "genpkey", "-algorithm", "RSA",
                        "-pkeyopt", "rsa_keygen_bits:2048", "-out", str(key)],
                       check=True, capture_output=True)
        payload = tmp / "rules.json"
        payload.write_text('{"revision":7,"rules":[]}', encoding="utf-8")
        sig = tmp / "rules.json.sig"
        assert module.sign(key, payload, sig) == 0
        public = tmp / "key.pub"
        subprocess.run(["openssl", "rsa", "-in", str(key), "-pubout", "-out", str(public)],
                       check=True, capture_output=True)

        payload.write_text('{"revision":8,"rules":[]}', encoding="utf-8")
        assert module.verify(payload, sig, public) == 1


def test_verify_refuses_a_missing_signature():
    module = load()
    with tempfile.TemporaryDirectory() as tmp:
        tmp = pathlib.Path(tmp)
        payload = tmp / "rules.json"
        payload.write_text("{}", encoding="utf-8")
        assert module.verify(payload, tmp / "absent.sig", tmp / "absent.pub") == 1


def test_the_shipped_pair_verifies():
    module = load()
    assert module.verify(module.DEFAULT_FILE,
                         module.DEFAULT_FILE.with_suffix(module.DEFAULT_FILE.suffix + ".sig"),
                         module.PUBLIC_KEY) == 0


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    failed = 0
    for test in tests:
        try:
            test()
            print(f"PASS {test.__name__}")
        except Exception as exc:  # noqa: BLE001 - a test harness reports, it does not raise
            failed += 1
            print(f"FAIL {test.__name__}: {exc}", file=sys.stderr)
    print(f"{len(tests) - failed}/{len(tests)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
