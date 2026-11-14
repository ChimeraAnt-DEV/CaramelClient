## One Mod Menu (U4): the touch screen and the VIP screen were merged
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.
- **There is one Mod Menu.** `ModMenuOverlay` is the only menu. The VIP window
  (`VipModMenuOverlay`, `VipModuleAdapter`, `VipInputMode`, `overlay_vip_mod_menu.xml`, the VIP card
  layouts) is **deleted** — do not reintroduce a second menu, a second page list, or a "premium
  version" of the same screen. A module or a destination that exists in one and not the other is
  exactly the class of bug the merge removes.
- **`ModMenuPage` is the one page registry.** `pages()` returns every page in rail/cycle order;
  `next()`/`previous()` wrap; `defaultForInput(hasGamepad)` picks the opening page. The pages that
  used to be premium-only (`CONTROLLER`, `KEYBOARD`) are ordinary entries flagged `isSupporter()`,
  and `supporterPages()` returns them. `ModMenuPageTest` pins all of it. Do **not** add a
  `vipPages()`/`sharedPages()` split again — the badge marks a page, the list does not exclude it.
- **The Supporter badge is a small accent dot on the rail tile** (`ModMenuRail.build`, from
  `page.isSupporter()`). It is presentation only: the page behaves like every other page.
- **The launcher's Mods screen and the in-game bind open the same menu.** `ModMenuButton` no longer
  holds a second overlay; `openMenuSection(page)` creates the one `ModMenuOverlay` and calls
  `showPage(page)`. `showPage` records `pendingPage` when the menu is not open yet, and `setupViews`
  consumes it after the views exist, so the radial wheel's sectors land on the right page.
- **`activePage` is set in `selectPage`, not only in `updateNavigationItems`.** Controller and
  Keyboard pass all-false navigation booleans, so the shoulder cycle and the controller-input
  routing would otherwise not know which page is showing. `selectPage` also paints the rail tile and
  the top-bar title.
- **The Supporter pages are ported, not stubbed.** `ModMenuControllerSection` (profiles, dead zone,
  sensitivity, rumble, the shared `ControllerProfileEditor`) and `ModMenuKeyboardSection` (the
  `KeyboardIllustrationView` board, the rebindable actions via `MenuBindRelay`, the pointer slider)
  are real pages of the one menu, hosted in `controller_container` / `keyboard_container` in
  `overlay_mod_menu.xml`. Each step is isolated through `ModMenuGuard`, because a page built over a
  running game must not take the session down.
- **`hideAllContentExcept(keep)` is the one page-hiding path**, and it runs the panels' own
  `onHidden()` side effects (a Cosmetics spinner list is a separate window; a Replay panel owns a
  live recorder listener). Pages that hide their siblings by hand call `hideSupporterPages()`. Add a
  new host to both places, or a page switch will leave the previous host visible.
- **The Visuals/Misc filters are the VIP window's category split, kept.** They live in
  `ModuleFilter` and their predicates come from the tested `ModCategory` (`vip_category_visuals` /
  `vip_category_misc`), so there is one classification rule, not two. `ModCategory` stays in the
  `vip` package.
- **The VIP overlay's controller-input priority is gone with it.** `InbuiltOverlayManager` hands
  controller keys/motion to the one `ModMenuOverlay`; the radial wheel still takes the stick first
  while it is open. `ModMenuButton.isMenuShowing()` now covers the menu and the wheel.
- `VipTheme` and `bg_vip_*` / `ic_vip_*` survive only where the ported pages use them
  (`bg_vip_topbar`, `bg_vip_surface`, `bg_vip_icon_tile`, `bg_vip_icon_button`, `bg_vip_status_off`).
  The VIP-only shimmer/skeleton/empty/indicator drawables and the VIP module cards were deleted with
  the window; do not re-add a VIP card layout.
- **`MenuBindRelay` remains the single bind-capture sink**, now shared by the Settings tab's bind
  rows and the Keyboard page's rows.
