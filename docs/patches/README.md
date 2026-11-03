# Pending upstream patches

## `berryapi-replay-c-abi.patch`

A C ABI for BerryAPI's `ReplayService` (`berry/replay_c.h` + its adapter and host test),
so a host that `dlopen`s `libBerryAPI.so` can load a replay and drive playback without
linking BerryAPI's C++ surface.

**Applied upstream.** Merged into `Lynxifyy/BerryAPI` as PR #16 (merge commit `b44f8fb4`),
so the file is kept here only as a historical record of the change; the preloader bridge and
the client mirror in this repository are built against the merged `berry/replay_c.h`.

To apply:

```sh
git clone https://github.com/Lynxifyy/BerryAPI && cd BerryAPI
git checkout -b lynxifyy/replay-c-abi
git am docs/patches/berryapi-replay-c-abi.patch   # or: git apply <patch>
git push -u origin lynxifyy/replay-c-abi
```

Verified locally before export: `test_replay_c: all checks passed`; the four `BERRY_*`
symbols export in `libBerryAPI.so` (`nm -D`); `dlopen_test: ALL OK`; `check-sources`
complete.

The client side of the same contract is `ReplayAbi` and `GovernorAbi` in
`app/src/main/java/org/chimeramc/client/core/replay/` and `.../core/mods/fps/`, so an ABI
drift fails a unit test once the library half lands.
