#!/usr/bin/env python3
"""Tests for scripts/verify_berryapi_abi.py.

The verifier is the only thing standing between a stale bundled BerryAPI and a
silently dark feature set, so its two jobs are pinned here against the real
library that ships in the repository: it must accept the bundled binary, and it
must reject a binary whose C surface is incomplete.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import verify_berryapi_abi as v


def run_tests() -> int:
    lib = v.DEFAULT_LIB
    assert lib.is_file(), f"bundled library missing: {lib}"

    # 1. The bundled library passes and pins the seam ABI.
    assert v.check(lib) == 0, "bundled libBerryAPI.so must satisfy the ABI check"

    data = lib.read_bytes()

    # 2. Every required symbol resolves, and the seam ABI immediate decodes to the
    #    value the preloader mirror targets.
    defined = {name for name, value in v._dynamic_symbols(data) if value != 0}
    for names in v.REQUIRED_SYMBOLS.values():
        for name in names:
            assert name in defined, f"required symbol absent from the bundle: {name}"
    fn_off = v._find_function(data, "BERRY_WorldSeamAbiVersion")
    assert fn_off is not None, "BERRY_WorldSeamAbiVersion code not found"
    assert v._seam_abi_immediate(data, fn_off) == v.EXPECTED_SEAM_ABI, \
        "the bundled library's seam ABI must equal the preloader mirror's"

    # 3. Renaming a required symbol in the string table makes the check fail, which
    #    is the regression this verifier exists to catch.
    import tempfile
    broken = bytearray(data)
    needle = b"BERRY_SetWorldSeam\x00"
    at = bytes(broken).find(needle)
    assert at != -1, "could not locate BERRY_SetWorldSeam in .dynstr for the negative case"
    broken[at:at + len(needle)] = b"BERRY_SetWorldSeaX\x00"  # same length, one char changed
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "libBerryAPI.so"
        path.write_bytes(broken)
        assert v.check(path) == 1, "a library missing a required symbol must be rejected"

    # 4. A non-ELF file is a usage error (exit 2), not a silent pass.
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "not-an-elf.so"
        path.write_bytes(b"not an elf at all")
        assert v.check(path) == 2, "a non-ELF file must be reported as unreadable"

    # 5. The core entry points return `int`, not C++ `bool`. BerryAPI's header documents this: a
    #    1-byte bool return leaves the upper result-register bytes undefined, so a caller that
    #    declared `bool (*)()` could read a true as false and report "not attached" on a live
    #    runtime. The preloader must resolve them as int-returning pointers.
    bridge = (v.ROOT / "app/src/main/cpp/preloader/src/pl/runtime/BerryApiBridge.cpp").read_text()
    assert "using InjectFn = int (*)();" in bridge, \
        "BERRY_Inject returns int; the preloader must not resolve it as bool"
    assert "using IsAttachedFn = int (*)();" in bridge, \
        "BERRY_IsAttached returns int; the preloader must not resolve it as bool"

    print("verify_berryapi_abi_test: OK")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(run_tests())
    except AssertionError as error:
        print(f"verify_berryapi_abi_test: FAILED: {error}", file=sys.stderr)
        sys.exit(1)
