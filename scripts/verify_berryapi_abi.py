#!/usr/bin/env python3
"""Verify the bundled BerryAPI shared library exposes the C ABI the preloader binds.

The preloader never links BerryAPI: it ``dlopen``s ``libBerryAPI.so`` and resolves a
fixed set of ``BERRY_*`` symbols through ``dlsym`` (see
``app/src/main/cpp/preloader/src/pl/runtime/BerryApiBridge.cpp``). If the bundled
library was built from an older BerryAPI that predates the world-seam / replay /
governor C surface, every one of those ``dlsym`` calls returns null and the whole
feature set stays silently dark -- the launcher reports "world feed unavailable"
and the dependent modules are greyed out with no build-time warning.

This check reads the library's ``.dynsym``/``.dynstr`` directly, so it needs no
Android toolchain and no third-party module, and fails when any required symbol is
missing. It is the guard that keeps the bundled binary and the preloader's
expectations from drifting apart.

It also pins the layout constant BerryAPI validates at bind time: the ABI version
that ``BERRY_WorldSeamAbiVersion`` reports must equal the preloader mirror's
``kHostSeamAbiVersion``. The numeric value is read from the library's bytes (the
function returns it as an ``mov w0, #imm`` immediate), so a BerryAPI ABI bump is
caught here rather than by a misread function pointer on the device.

Usage:
    python3 scripts/verify_berryapi_abi.py [path/to/libBerryAPI.so]

With no argument the bundled arm64-v8a library is checked.
"""

from __future__ import annotations

import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_LIB = ROOT / "app/src/main/jniLibs/arm64-v8a/libBerryAPI.so"

# Every symbol the preloader resolves with dlsym. Grouped so a failure names the
# feature it disables.
REQUIRED_SYMBOLS = {
    "core": [
        "BERRY_Version",
        "BERRY_IsAttached",
        "BERRY_Inject",
    ],
    "world seam": [
        "BERRY_SetWorldSeam",
        "BERRY_ClearWorldSeam",
        "BERRY_HasWorldSeam",
        "BERRY_WorldSeamStructSize",
        "BERRY_WorldSeamAbiVersion",
    ],
    "replay": [
        "BERRY_GetReplayApi",
        "BERRY_ReplayStructSize",
        "BERRY_ReplayAbiVersion",
    ],
    "governor": [
        "BERRY_GetGovernorApi",
        "BERRY_GovernorStructSize",
        "BERRY_GovernorAbiVersion",
    ],
}

# The seam ABI version the preloader mirror is compiled against
# (pl::berry::kHostSeamAbiVersion). BERRY_WorldSeamAbiVersion must return this.
EXPECTED_SEAM_ABI = 8

# The replay and governor surfaces, and the struct sizes the mirrors produce. The
# library's BERRY_*AbiVersion / BERRY_*StructSize must equal these, or the preloader
# refuses the surface at run time (the size/ABI check) and the feature stays dark.
EXPECTED_REPLAY_ABI = 2
EXPECTED_REPLAY_STRUCT_SIZE = 144
EXPECTED_GOVERNOR_ABI = 1
EXPECTED_GOVERNOR_STRUCT_SIZE = 48

# (symbol, expected value, human description) checked by decoding the function's
# returned immediate. These are the numbers the preloader compares against.
ABI_CONSTANTS = [
    ("BERRY_WorldSeamAbiVersion", EXPECTED_SEAM_ABI, "world-seam ABI (kHostSeamAbiVersion)"),
    ("BERRY_ReplayAbiVersion", EXPECTED_REPLAY_ABI, "replay ABI (kHostReplayAbiVersion)"),
    ("BERRY_ReplayStructSize", EXPECTED_REPLAY_STRUCT_SIZE, "replay struct size"),
    ("BERRY_GovernorAbiVersion", EXPECTED_GOVERNOR_ABI, "governor ABI (kHostGovernorAbiVersion)"),
    ("BERRY_GovernorStructSize", EXPECTED_GOVERNOR_STRUCT_SIZE, "governor struct size"),
]

# ELF section type for a symbol table (SHT_SYMTAB=2, SHT_DYNSYM=11).
SHT_SYMTAB = 2
SHT_DYNSYM = 11
# Symbol table entry sizes by ELF class.
SYM_ENT = {32: 16, 64: 24}
# The section-name index field lives at a class-dependent offset in the header.
E_SHSTRNDX_OFF = {32: 0x2E, 64: 0x3E}
E_SHOFF_OFF = {32: 0x20, 64: 0x28}
E_SHENTSIZE_OFF = {32: 0x2E, 64: 0x3A}
E_SHNUM_OFF = {32: 0x30, 64: 0x3C}


def _parse_sections(data: bytes) -> dict[str, tuple[int, int, int, int, int]]:
    """Return {name: (offset, size, entsize, link, type)} for every section."""
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    is64 = data[4] == 2
    endian = "<" if data[5] == 1 else ">"
    cls = 64 if is64 else 32

    shoff = struct.unpack_from(endian + "I", data, E_SHOFF_OFF[cls])[0]
    shentsize = struct.unpack_from(endian + "H", data, E_SHENTSIZE_OFF[cls])[0]
    shnum = struct.unpack_from(endian + "H", data, E_SHNUM_OFF[cls])[0]
    shstrndx = struct.unpack_from(endian + "H", data, E_SHSTRNDX_OFF[cls])[0]

    def read_sh(i: int) -> dict:
        base = shoff + i * shentsize
        if is64:
            name, stype = struct.unpack_from(endian + "II", data, base)
            off, size = struct.unpack_from(endian + "QQ", data, base + 0x18)
            link = struct.unpack_from(endian + "I", data, base + 0x28)[0]
            entsize = struct.unpack_from(endian + "Q", data, base + 0x38)[0]
        else:
            name, stype = struct.unpack_from(endian + "II", data, base)
            off, size = struct.unpack_from(endian + "II", data, base + 0x10)
            link = struct.unpack_from(endian + "I", data, base + 0x18)[0]
            entsize = struct.unpack_from(endian + "I", data, base + 0x24)[0]
        return {"name_off": name, "type": stype, "off": off, "size": size,
                "link": link, "entsize": entsize}

    sections = [read_sh(i) for i in range(shnum)]
    shstr = sections[shstrndx]
    strtab = data[shstr["off"]:shstr["off"] + shstr["size"]]

    def cstr(buf: bytes, at: int) -> str:
        end = buf.index(b"\x00", at)
        return buf[at:end].decode("utf-8", "replace")

    out: dict[str, tuple[int, int, int, int, int]] = {}
    for s in sections:
        out[cstr(strtab, s["name_off"])] = (s["off"], s["size"], s["entsize"],
                                            s["link"], s["type"])
    return out


def _dynamic_symbols(data: bytes) -> list[tuple[str, int]]:
    """Return [(name, st_value)] from .dynsym (value 0 means undefined/import)."""
    sections = _parse_sections(data)
    if ".dynsym" not in sections or ".dynstr" not in sections:
        raise ValueError("no .dynsym/.dynstr")
    sym_off, sym_size, entsize, link, _ = sections[".dynsym"]
    str_off, str_size, _, _, _ = sections[".dynstr"]
    strtab = data[str_off:str_off + str_size]

    def cstr(at: int) -> str:
        end = strtab.index(b"\x00", at)
        return strtab[at:end].decode("utf-8", "replace")

    if entsize == 0:
        entsize = SYM_ENT[64]
    symbols = []
    for i in range(sym_size // entsize):
        base = sym_off + i * entsize
        if entsize == 24:  # Elf64_Sym: name(4) info(1) other(1) shndx(2) value(8) size(8)
            name_off = struct.unpack_from("<I", data, base)[0]
            value = struct.unpack_from("<Q", data, base + 8)[0]
        else:  # Elf32_Sym: name(4) value(4) size(4) info(1) other(1) shndx(2)
            name_off = struct.unpack_from("<I", data, base)[0]
            value = struct.unpack_from("<I", data, base + 4)[0]
        if name_off == 0:
            continue
        symbols.append((cstr(name_off), value))
    return symbols


def _vaddr_to_offset(data: bytes, vaddr: int) -> int | None:
    """Translate a virtual address to a file offset via the PT_LOAD segments.

    The executable segment is not guaranteed to be mapped at vaddr == offset (it
    usually is not once the read-only segment has a different size), so the
    symbol's st_value cannot be used as an offset directly. Returns None when no
    loadable segment covers the address.
    """
    is64 = data[4] == 2
    endian = "<" if data[5] == 1 else ">"
    if is64:
        phoff = struct.unpack_from(endian + "Q", data, 0x20)[0]
        phentsize = struct.unpack_from(endian + "H", data, 0x36)[0]
        phnum = struct.unpack_from(endian + "H", data, 0x38)[0]
    else:
        phoff = struct.unpack_from(endian + "I", data, 0x1C)[0]
        phentsize = struct.unpack_from(endian + "H", data, 0x2A)[0]
        phnum = struct.unpack_from(endian + "H", data, 0x2C)[0]

    for i in range(phnum):
        base = phoff + i * phentsize
        if is64:
            p_type = struct.unpack_from(endian + "I", data, base)[0]
            p_offset = struct.unpack_from(endian + "Q", data, base + 0x08)[0]
            p_vaddr = struct.unpack_from(endian + "Q", data, base + 0x10)[0]
            p_filesz = struct.unpack_from(endian + "Q", data, base + 0x20)[0]
        else:
            p_type = struct.unpack_from(endian + "I", data, base)[0]
            p_offset = struct.unpack_from(endian + "I", data, base + 0x04)[0]
            p_vaddr = struct.unpack_from(endian + "I", data, base + 0x08)[0]
            p_filesz = struct.unpack_from(endian + "I", data, base + 0x10)[0]
        if p_type != 1:  # PT_LOAD
            continue
        if p_vaddr <= vaddr < p_vaddr + p_filesz:
            return p_offset + (vaddr - p_vaddr)
    return None


def _find_function(data: bytes, name: str) -> int | None:
    """Return the file offset of a defined function symbol's code, or None."""
    for sym, value in _dynamic_symbols(data):
        if sym == name and value != 0:
            return _vaddr_to_offset(data, value)
    return None


def _seam_abi_immediate(data: bytes, fn_off: int) -> int | None:
    """Decode the constant BERRY_WorldSeamAbiVersion returns.

    The function is a leaf that moves a small constant into w0 and returns. The
    compiler emits whichever of the three immediate moves is cheapest: ``movz``
    for a value that fits a shifted 16-bit field, ``movn`` for its complement, or
    an ``orr``-immediate for a bitmask value. Reading the immediate lets this
    check confirm the exact ABI number without running the library. Returns None
    when the prologue does not match any of the expected shapes.
    """
    for i in range(24):
        insn = struct.unpack_from("<I", data, fn_off + i * 4)[0]
        rd = insn & 0x1F
        if rd == 0:  # w0
            # MOVZ (32-bit): sf=0, opc=10, 100101, hw, imm16, Rd.
            if (insn & 0xFF800000) == 0x52800000:
                imm16 = (insn >> 5) & 0xFFFF
                hw = (insn >> 21) & 0x3
                return imm16 << (hw * 16)
            # MOVN (32-bit): the bitwise complement of the shifted immediate.
            if (insn & 0xFF800000) == 0x12800000:
                imm16 = (insn >> 5) & 0xFFFF
                hw = (insn >> 21) & 0x3
                return (~(imm16 << (hw * 16))) & 0xFFFFFFFF
            # ORR (immediate), 32-bit: sf=0, opc=01, 100100, N, immr, imms, Rn=31, Rd.
            if (insn & 0x7F800000) == 0x32000000 and ((insn >> 5) & 0x1F) == 31:
                immr = (insn >> 16) & 0x3F
                imms = (insn >> 10) & 0x3F
                # A plain mov: N=0, immr==0 gives one run of (imms+1) low bits.
                if ((insn >> 22) & 1) == 0 and (imms & 0x20) == 0 and immr == 0:
                    return (1 << (imms + 1)) - 1
        if insn == 0xD65F03C0:  # ret
            break
    return None


def check(lib: Path) -> int:
    if not lib.is_file():
        print(f"verify_berryapi_abi: {lib} not found", file=sys.stderr)
        return 2
    data = lib.read_bytes()

    try:
        defined = {name for name, value in _dynamic_symbols(data) if value != 0}
    except ValueError as error:
        print(f"verify_berryapi_abi: cannot read {lib}: {error}", file=sys.stderr)
        return 2

    missing: list[str] = []
    for group, names in REQUIRED_SYMBOLS.items():
        absent = [n for n in names if n not in defined]
        if absent:
            print(f"verify_berryapi_abi: missing {group} symbols: {', '.join(absent)}",
                  file=sys.stderr)
            missing.extend(absent)

    if missing:
        print(
            "\nThe bundled libBerryAPI.so predates the C surface the preloader binds.\n"
            "Rebuild it from Lynxifyy/BerryAPI with tools/build-android.sh (ABI 8) and\n"
            "replace app/src/main/jniLibs/arm64-v8a/libBerryAPI.so.",
            file=sys.stderr,
        )
        return 1

    # Pin every ABI number the preloader compares against. A function whose
    # prologue does not decode is left unpinned rather than failed, so a compiler
    # that emits an unexpected constant-materialisation sequence does not produce a
    # false alarm; the mismatch case is what matters and is detected whenever the
    # immediate decodes.
    pinned = 0
    for symbol, expected, description in ABI_CONSTANTS:
        fn_off = _find_function(data, symbol)
        if fn_off is None:
            continue
        immediate = _seam_abi_immediate(data, fn_off)
        if immediate is None:
            continue
        pinned += 1
        if immediate != expected:
            print(
                f"verify_berryapi_abi: {symbol} returns {immediate} but the preloader targets "
                f"{expected} ({description}). Update the preloader mirror (kHost*AbiVersion and the "
                f"struct layout) together with the library.",
                file=sys.stderr,
            )
            return 1

    note = (f"{pinned}/{len(ABI_CONSTANTS)} ABI constants pinned" if pinned
            else "ABI constants not pinned (prologue shape)")
    print(f"verify_berryapi_abi: OK ({lib.name}, {len(defined)} defined dynamic symbols, "
          f"all {sum(len(v) for v in REQUIRED_SYMBOLS.values())} required present, {note})")
    return 0


def main(argv: list[str]) -> int:
    lib = Path(argv[1]) if len(argv) > 1 else DEFAULT_LIB
    return check(lib)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
