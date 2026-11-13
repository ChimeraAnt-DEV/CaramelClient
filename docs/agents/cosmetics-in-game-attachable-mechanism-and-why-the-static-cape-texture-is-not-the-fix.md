## Cosmetics in-game: attachable mechanism, and why the static cape texture is not the fix
- **The static `textures/entity/cape_invisible.png` override is a dead end for this build.** The
  shipped binary routes an *equipped* cape through Microsoft's Persona cosmetics system
  (`persona_capes`, `persona.cape.texture_`, `PersonaNetworkHandlerDelegate::_fetchLodSkinTexture`),
  whose texture is fetched per account rather than read from that fixed slot. A resource pack that
  overrides the slot therefore changes a file the renderer does not consult for the case that
  matters, which is why a cape could be "applied successfully" and still never show.
- **A `minecraft:attachable` cannot be triggered by a cape.** Verified against the Bedrock Wiki:
  an attachable binds to an *item/block identifier* (main hand, off hand, armour) and renders when
  that item is equipped; there is no client-side condition to bind it to a cosmetic. The cape's
  correct data-driven route is the player client entity's `cape` *bone* plus the pack's own
  animation, described below. Do not "convert the cape to an attachable"; there is nothing for it
  to attach to.
- **Do NOT redirect the vanilla `"cape"` animation key.** It is played by
  `controller.animation.player.root` in third person and the paperdoll for *every* player, and it
  drives the vanilla `cape` bone on a player wearing a vanilla or Persona cape. Pointing that key at
  `animation.chimera_cape` (the first implementation) replaced vanilla's own
  `math.lerp(0.0, -126.0, query.cape_flap_amount) - 6.0` swing globally, so a plain vanilla cape тАФ
  with no Caramel cosmetic at all тАФ froze. The pack must leave `"cape": "animation.player.cape"`
  alone and instead play `animation.chimera_cape` from the entity's own `animate` list. That is what
  scopes it: the animation only writes the `cape_1..N` bones, which exist only in this pack's
  geometry, so vanilla's `cape` bone and animation are never touched. `CapeResourcePackBuilderTest`
  pins both the untouched key and the `animate`-list playback.
- **The cape animation must read `query.cape_flap_amount`.** Vanilla drives its cape from that 0..1
  signal, not from `modified_move_speed` (which is game-scaled and small at a walk). The curve uses
  both: `cape_flap_amount` as the primary driver and `modified_move_speed` as a fallback so the
  cloth still moves with no vanilla cape equipped. `loop: true`, matching vanilla. Amplitudes are
  anchored to vanilla's 126┬░ (deliberately shorter, 42┬░, so it reads as cloth not a flag).
- **The worn headwear needs its own head-look animation.** An accessory is a separate geometry in
  entity space, so it follows position and body facing but *not* the head's look rotation; without
  `animation.chimera_hat` (played from `animate`) turning the `acc` bone by
  `query.target_x_rotation`/`query.target_y_rotation`, a hat stays bolt-upright while the player
  looks around. The `acc` pivot is the neck (0, 24, 0), the vanilla `head` bone's pivot.
- **Authored Blockbench meshes are the intended fix for the "box glued to a box" look.** Procedural
  box-stacking cannot produce the catalogue's quality bar. Drop a Blockbench `.geo.json` into
  `resources/cosmetics/models/` (`chimera_cape.geo.json` / `chimera_hat.geo.json` /
  `chimera_pet.geo.json`); `AuthoredGeometry` retargets its `identifier` to the pack id and the
  builder writes it verbatim, falling back to the procedural mesh when absent or malformed. No code
  change is needed to swap a mesh.
- **A skin pack is the other documented Bedrock cape path, but it cannot serve a launcher
  cosmetic.** It pairs a cape texture with a skin in `skins.json`, so it only works while the
  player wears that exact skin and requires a Dressing Room re-entry. The render-controller route
  adds a cape the player did not have, on top of whatever skin they wear.
- **`cosmetics_scope_note` describes the honest model**: all three cosmetics are drawn on the
  player's own client entity by the generated pack, so only Caramel users (via cosmetic sync) see
  them - vanilla players cannot, because Bedrock offers no app API to push a cosmetic onto another
  account. Keep it in step with the feature or the UI over-claims.
- Pets and hats are drawn **on your own player**, never spawned: `/summon` is a server-side command
  that would flag the account, so the client-entity route is the only server-safe one.
- **A named geometry that does not resolve can fail the whole client entity.** `AccessoryGeometry`
  and `PetGeometry` always emit a resolving-but-empty mesh for "none equipped", so the player
  entity's geometry references resolve and the cape is not dragged down with them.
- **In-place pack reload is not implemented in this build.** The sweetloader now has a real
  fail-safe `nativeReloadResourcePacks` body, but it resolves to "no hook" on the shipped builds
  (verified against the binary тАФ see `preloader-android/docs/resource-pack-reload.md`), so a
  running world keeps its cached pack stack and the supported apply path relaunches the instance
  (`MinecraftSessionRestarter`). Do not promise a live recolour.
