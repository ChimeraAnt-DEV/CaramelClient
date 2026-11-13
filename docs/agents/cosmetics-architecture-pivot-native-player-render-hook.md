## Cosmetics architecture pivot: native player-render hook
- **The resource-pack route has four structural limits** and is now treated as a *fallback*, not the
  destination: (1) no per-bone cloth physics (a pack has no physics driver, only Molang queries),
  (2) no live apply without a world reload (the running world caches its pack stack тАФ verified, see
  the section above), (3) a cosmetic is only on *your* client entity, so another player sees it only
  if they also run the same pack, and (4) the catalogue is texture-variant-limited. The pivot is a
  hook on the game's own player renderer, which fixes all four in principle.
- **Hook point: `LivePlayerRenderer::render`, resolved by RTTI name.** The **slot index moves
  between builds** тАФ verified slot **17** on 1.26.60.x but slot 17 is null on **26.52** тАФ so the
  hook resolves the slot at runtime: `resolveVtableSlotReferencingString("18LivePlayerRenderer",
  "variable.player_x_rotation", ...)` finds the slot whose body references the render diagnostic,
  then falls back to the per-version `playerRenderVtableIndex` rule and the default (17). Before
  hooking, `isExecutableAddress()` refuses a resolved address that is not in an executable mapping,
  so a stale index that lands in data fails closed instead of faulting the render thread. The slot
  is therefore a **floor**-gated attempt (min 1.21.132, no upper bound), not a version allowlist.
  The sweetloader module is `pl::runtime::GamePlayerRender`; full method in
  `preloader-android/docs/player-render-hook.md`.
- **One hook on the slot, many consumers.** `GamePlayerRender` owns the single detour and exposes
  `RegisterPostRenderCallback`; the cosmetics renderer registers its draw callback into it rather
  than installing a second detour on the same function (two detours duplicated the frame accounting
  and left ownership ambiguous). It also dispatches the public `pl/Render.hpp` mod callbacks from the
  same seam.
- **The detour is a pure passthrough** тАФ the game strips symbols, so the C++ signature is not
  recoverable; the hook forwards `x0..x7` unchanged and dereferences nothing. That is what makes an
  unknown ABI safe. It publishes a render tick and a per-frame call count; `callsThisFrame > 1`
  proves the hook fires for non-local players' models too, which is the property the pack cannot
  give.
- **Launcher probe: `NativeCosmeticsFeed`** (in `core.cosmetics`) reads the feed over
  `PreloaderInput.isPlayerRenderHookLive()` / `readPlayerRenderStats()` and classifies
  `NATIVE_RENDER` vs `PACK_FALLBACK`. **Fail-closed:** no library, no hook, or the renderer never
  ran reads as `PACK_FALLBACK`, so the launcher never claims a native path it does not have and the
  pack path keeps working unchanged. `CosmeticsDiagnostics` reports the native mode as a separate
  informational row.
- **The native hook is additive and currently the *foundation*, not the finished cosmetic.** It
  establishes the frame tick and proves multi-player coverage; driving segmented cape/pet motion and
  rendering geometry from the hook are the next steps. Do not describe native cape rendering as
  shipped until the render substitution is actually in place.
- **An attachable cannot replace this, and the earlier "convert the cape to an attachable" advice
  was wrong.** Verified against the shipped binary: `minecraft:attachable` binds to an
  *item/block identifier* in an equipment slot тАФ the only supported armor attachable slots are
  `slot.armor.chest` and `slot.armor.body` тАФ and `query.equipped_item_is_attachable` takes a hand
  (0/1). There is no client-only player attachable, so there is nothing for a cape to attach to.
  `enable_attachables` is only an entity flag that gates the *item* attachable path. The pack's
  player-entity `cape` bone route (section above) remains the correct data-driven cape mechanism.
