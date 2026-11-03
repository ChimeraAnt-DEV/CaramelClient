#!/usr/bin/env bash
#
# Verifies the preloader (and every other) submodule is pinned to a commit, not a branch
#.
#
# A gitlink is always a commit SHA, so the way a submodule silently floats is a `branch =` entry
# in .gitmodules: it lets `git submodule update --remote` move the checkout to whatever that branch
# now points at, which turns a reproducible build into a moving target and makes the "bump the
# preloader in a dedicated commit" rule unenforceable.
#
# Checks, in order:
#   1. .gitmodules declares no `branch` for any submodule.
#   2. Every declared submodule has a gitlink recorded in the index (mode 160000).
#   3. The checked-out submodule commit equals the recorded gitlink, so the tree is not dirty with
#      an unrecorded bump.
#
# Exits non-zero with a specific message on the first failure.

set -u

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root" || exit 2

fail() {
    echo "submodule-pin: $1" >&2
    exit 1
}

[ -f .gitmodules ] || { echo "submodule-pin: no .gitmodules; nothing to check"; exit 0; }

# 1. No branch tracking.
if grep -Eq '^[[:space:]]*branch[[:space:]]*=' .gitmodules; then
    fail "a submodule declares 'branch =', so 'git submodule update --remote' can float it. Remove the entry and bump the pinned commit in a dedicated commit instead."
fi

# The submodule paths, one per line.
paths="$(git config -f .gitmodules --get-regexp '^submodule\..*\.path$' | awk '{print $2}')"
[ -n "$paths" ] || { echo "submodule-pin: .gitmodules declares no paths"; exit 0; }

status=0
while IFS= read -r path; do
    [ -n "$path" ] || continue

    # 2. A gitlink in the index.
    entry="$(git ls-files -s -- "$path" 2>/dev/null)"
    if [ -z "$entry" ]; then
        fail "$path is declared in .gitmodules but has no gitlink in the index"
    fi
    recorded="$(printf '%s\n' "$entry" | awk '{print $1" "$2}')"
    case "$recorded" in
        "160000 "*) : ;;
        *) fail "$path is recorded with mode $(printf '%s' "$recorded" | awk '{print $1}'), not a submodule gitlink (160000)" ;;
    esac
    want="$(printf '%s\n' "$entry" | awk '{print $2}')"

    # 3. The checkout matches the pin.
    if [ -d "$path/.git" ] || [ -f "$path/.git" ]; then
        got="$(git -C "$path" rev-parse HEAD 2>/dev/null || true)"
        if [ -n "$got" ] && [ "$got" != "$want" ]; then
            fail "$path is checked out at $got but the index pins $want. Commit the bump in a dedicated commit so the pin and the tree agree."
        fi
    fi
done <<EOF
$paths
EOF

if [ "$status" -ne 0 ]; then
    exit "$status"
fi

echo "submodule-pin: all submodules are pinned to commits"
