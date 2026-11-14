# UI check list — Blueberry restyle

The dev environment has no device, so the pure JVM tests cover the geometry, tokens and rules, and
this list is what only a phone can confirm. Work top to bottom, screenshot each step, and send the
shots back.

Everything below is **unverified (needs device)** until you run it. No claim in the release notes
should say a screen "works in game" until this list passes.

## 1. Mod Menu (native, drawn in-game)

The menu is Dear ImGui on the game's GL context. Nothing about it is an Android view, so the checks
are about the seam (does the menu draw over the game at all) and about the theme matching the
launcher.

1. It opens and draws.
   - [ ] The in-game bind opens the menu; it appears over the game, centred, with the launcher's
     panel fill and hairline border.
   - [ ] Opening and closing costs no visible stutter in the game.
   - [ ] Back (or the menu's close control) closes it and returns to the game; the game keeps
     running underneath, never a black frame.
2. It is the same product as the launcher.
   - [ ] The header, the tab bar and the panel colours match the launcher's Caramel theme (same
     navy glass, same accent blue) — no default ImGui grey anywhere.
   - [ ] No violet/magenta anywhere; the accent is blue.
3. Controls.
   - [ ] A module row is a pill switch on the right; the whole row toggles it.
   - [ ] A row that cannot run says why, and its switch does not read as a plain off toggle.
   - [ ] A one-shot action is a chevron, not a switch.
   - [ ] A slider drags with the finger and its value is shown.
   - [ ] Touch targets are comfortably tappable; a small screen does not crowd a row.
4. Tabs.
   - [ ] The tab bar lists every page (Mods, Replay, Cosmetics, Voice, Settings).
   - [ ] Switching tabs redraws the page and keeps the tab bar in the same place.
   - [ ] The Cosmetics tab shows no fabricated preview: it states the preview is launcher-side and
     offers an "Open launcher" action, rather than drawing a stand-in character.
5. Honesty on device.
   - [ ] While the present hook is absent, the frame-rate readout is marked approximate (a leading
     `~`) and names its source; it never shows a bare number as if it were the game's.
   - [ ] Frame Generation and Motion Blur are badged while the present hook is absent, whatever
     their switches say.

## 2. Mod Menu row, switch and appearance rules

The menu is the native one (section 1); these are the row, switch and appearance rules it must keep.

- [ ] The panel is the dark navy surface at a 28dp radius — no violet/magenta and no pure black.
  The accent appears only on switches and focus rings, never on a header or a card border.
- [ ] **A module row shows a pill switch; an action row shows a chevron, never both.** Tapping a
  module toggles it and the switch slides; "Reset HUD Position" and "Reload Modules" fire once.
- [ ] The switch haptics on the state change only, never on press-down.
- [ ] Every row's touch target is at least 48dp even though the switch's visual is 52x32dp.
- [ ] A module that cannot run is dimmed, shows a coloured dot and states its reason in one line.
  Tapping it opens the reason sheet; a crash-disabled module offers "Re-enable".
- [ ] Long-pressing a module starts a drag; a module may only move within its own section, and the
  order survives closing and reopening the menu.
- [ ] Settings > Appearance: the theme, glass, density, toggle-style and motion presets apply
  immediately, and the radius / blur-refresh / HUD-opacity sliders and the accent swatches do too.
- [ ] With Android's "Remove animations" ON, the menu still animates but only as a short linear
  fade; with "Reduce transparency" ON the panel is solid. The OS setting beats the in-menu choice.
- [ ] Nothing in the menu follows the launcher's accent setting — the menu has its own.

## 3. Adaptive navigation (U5) and the type/shape scales (U6)

- [ ] On a phone-width window the destinations are a horizontal bar at the top, icon-only, with
  the selected entry's label shown.
- [ ] On a 600-839dp window the destinations are a vertical icon rail on the leading edge.
- [ ] On an 840dp+ window the rail also shows every label.
- [ ] Rotating does not change the navigation shape; unfolding / resizing across a breakpoint does.
- [ ] Every label uses a type role (display/title/body/label/caption); no screen sets a raw sp.
- [ ] The active rail row's corners animate between the chip and card stops as the selection moves,
  and its label is heavier than the inactive rows'.

## 4. First run, failure states, Play dock and mod rows (U13-U17)

- [ ] Fresh install with no instance: the splash opens the **first-run wizard**, not the dashboard.
- [ ] The wizard shows Import game -> Pick version -> Sign in -> Ready, and the progress rail has
  as many dots as the active steps (three when launcher-managed sign-in is off, four when on).
- [ ] Backing out and returning resumes on the first incomplete step, not at the start.
- [ ] Skip marks the wizard done and opens the launcher; it never appears again.
- [ ] With an instance already installed, an upgrade into this build does **not** show the wizard.
- [ ] Launch with no version selected: the dialog says "No Version Selected".
- [ ] Import a version whose APK ships 32-bit libs only: the dialog says "Unsupported build" and
  names the ABI, not a generic launch failure.
- [ ] Settings -> Diagnostics -> Launch readiness shows the state key and whether it blocks.
- [ ] The same state is what the Play button reports, so the two cannot disagree.
- [ ] Stale native libs (bump the extractor version) launch anyway after a silent re-extract.
- [ ] The dock is one row: profile on the left, version as a chip on the right, then the full-width
  Play button. The chip opens the instance picker.
- [ ] A mod row shows its runtime kind (native/script) and, when it failed, a red badge whose
  content description is the recorded `kind: reason`.
- [ ] A mod that failed to load can still be switched off from its row.

## 5. Per-instance account binding (F2)

- [ ] Sign in two accounts. Open Instance Settings: the account row says "Using the account that is
  active in the launcher."
- [ ] Tap Change, pick the second account: the row names that gamertag, and the launcher's own
  active account has **not** changed.
- [ ] Launch that instance: it signs in as the bound account.
- [ ] Launch a different, unbound instance: it still signs in as the launcher's active account.
- [ ] Pick "Use the active account": the row returns to the active-account text.
- [ ] Delete the bound account from the account screen, then reopen Instance Settings: the row shows
  the warning, not a different name, and the instance still launches (as the active account).
- [ ] Bind an account, then remove its sign-in data: the warning appears and the launch is not
  blocked.

## 6. Closet (cosmetics)

- [ ] Left rail: Capes, Hats, Pets, Back, Offhand, Effects.
- [ ] Centre: a 3-column grid of cards with a name and an Equipped/Owned pill.
- [ ] Right: the live 3D preview of your character with your real skin; drag to rotate; it turns
  slowly when idle.
- [ ] Tapping a card equips it (the pill moves to it) and the preview updates.
- [ ] The Equip button below the preview is the accent gradient.

## 7. Launch tab

- [ ] The hero shows your character (real skin) standing on a soft shadow, with the instances/mods/
  playtime stats beside it.
- [ ] The LAUNCH button is the accent gradient, ~56dp, with a soft glow.
- [ ] The dock is opaque: no text shows through it.
- [ ] Scrolling to the bottom, no row runs underneath the dock.
- [ ] No empty grey banner at the top of the tab.

## 8. Skin behaviour (the special update)

- [ ] The Closet and the Launch hero preview show the **same** skin (both are the launcher's own
  real-skin preview). The in-game Cosmetics tab holds **no** preview; it says so and points at the
  launcher, so the two surfaces never disagree about your skin.
- [ ] Change your skin (import one in the launcher, or apply a skin pack) and reopen the Closet /
  Cosmetics page: the head updates **without restarting** the app.
- [ ] With a game session running, the preview shows your **real Minecraft skin** read from the
  game (the engine image-loader seam). The Cosmetics diagnostics screen should read
  "Game-skin feed installed: yes" and "Base skin captured from game: yes" once you are in a world.

## 9. UI gallery (debug)

- [ ] Settings > Updates, tap the version text 7 times: the gallery opens.
- [ ] Every component (toggle off/on/disabled, rows, colours, type) is visible and legible.

## Known limitations (not bugs)

- **The in-game skin read is content capture, not attribution.** The engine image-loader seam carries
  no player id, so the held texture is the most recent base-skin-shaped upload — in practice the local
  player's skin, but not proven to be. Whether the game routes the player's skin through this seam at
  all is **unverified (needs device)**; if it does not, the preview falls back to the imported /
  applied / Steve skin.
- **Cosmetics in game** depend on the sweetloader's player-render hook, which resolves to "no hook" on
  the shipped builds (see `AGENTS.md`); the preview is the verified surface.

---

## 10. Device screenshots (B7)

No screenshot in the README is a device capture yet. This is how they get produced, and what makes
one acceptable.

### Which devices

Three, at minimum, because the layout changes by width class (U5):

| Slot | Target | Why this one |
|---|---|---|
| Low-end phone | ~360dp wide, Android 9-11, 2-3 GB RAM | The narrowest layout, the API-28 fallbacks (R12), and the memory floor (B11). |
| Mid-range phone | ~410dp wide, Android 12+ | The common case, and the first build with `RenderEffect` glass (R2/R12). |
| Tablet | 600dp+ wide | The expanded layout; also where a nav rail replaces the bottom bar. |

### How

1. Install the **signed release** APK (B16), not a debug build - a screenshot of a debug artifact
   does not represent what users get.
2. Set the device to its **default** font scale and a light **and** dark wallpaper if you can, so
   the dynamic-color path is visible.
3. Capture: Launch tab, Library/Instances, Mods, Settings, the Mod Menu open in game, and the
   Diagnostics screen.
4. Save to `docs/assets/screenshots/<slot>-<screen>.png`, e.g. `lowend-launch.png`.

### Caption rules

- Every caption names the **device model, Android version and font scale**. "On a phone" is not a
  caption.
- A screenshot may only be captioned with a claim the checklist above has actually verified. If a
  screen was captured but its checklist section did not pass, the caption says so.
- **Never caption a mockup with a device name**, and never place a mockup where a reader would
  assume it is a capture.

Until these exist, no release note may describe a screen as working on a device.
