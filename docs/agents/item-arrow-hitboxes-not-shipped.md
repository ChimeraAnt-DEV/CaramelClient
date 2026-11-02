## Item/arrow hitboxes - not shipped (documented limitation)
- Phase 3 asked whether a nearby-entity walk (items, arrows, thrown items within ~16 blocks) is
  feasible with the existing anchor offsets. It is **not**, for the same reason the mob boxes are
  not: those entity kinds live in the game's entity list, which this build cannot read (see the
  native-feed section - game classes export no symbols, and no static vtable address yields a live
  `Actor*`). The overlay draws them only when a `HitboxMod.EntitySource` supplies a frame, and no
  provider is installed.
- **The options are disabled rather than removed** (`hitbox_show_items` / `hitbox_show_projectiles`
  / `hitbox_show_thrown_items` are in `ModIds.outputRequiresGameData`), so the switch is
  discoverable but cannot be turned on to no effect. This is the documented limitation the request
  asked for; do not "finish" it by drawing a guessed box.
