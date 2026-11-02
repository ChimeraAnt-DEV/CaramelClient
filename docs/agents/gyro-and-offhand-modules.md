## Gyro and Offhand modules
- **The Gyro module drives look through `PojavControlsMod.nativeSendLookDelta`** тАФ the same path
  mouse/touch look uses тАФ instead of a separate native gyro hook. Sensitivity, invert and dead zone
  are applied in Java (`GyroOverlay.sendLookDelta`), so it needs no native gyro symbol and works
  wherever the game does. `GyroMod` (the native class) is still pre-resolved at launch but the overlay
  no longer calls it.
- **The Offhand module opens the inventory, it does not "swap".** Bedrock has no swap-to-off-hand key at all тАФ there is no
  `key.offhand` in the shipped input map, so Java's `F` swap simply does not exist here. The off-hand slot is filled from the
  inventory screen and accepts only a handful of items (shields, arrows, firework rockets, totems of undying, maps, nautilus
  shells), so the module's primary action sends the game's inventory key (`E`) and lets the player place an allowed item;
  the secondary action uses the off-hand item (`V`). `OffhandAction.MODE_INVENTORY` is 0, `MODE_USE` 1, `MODE_BOTH` 2.
  `OffhandOverlay.performAction` is shared by the button and the hardware bind; a static `injecting` guard stops the bind
  from matching its own injected key and recursing. `OffhandAction.keysFor(mode, inventoryKey, useKey)` is the pure mode
  rule. The manager getter is `getOffhandInventoryKey()` (pref key kept as `offhand_inventory_key`, falling back to the
  legacy `offhand_swap_key` so a pre-rename profile still reads). Both modules are `ModIds`-registered, available (not
  greyed), and have config schemas.
