## Top-level navigation (org.chimeramc.client.ui.navigation.LauncherTab + BaseActivity)
- **The navigation shape is adaptive (U5), chosen at runtime from the window width.** `NavLayoutMode`
  (in `ui.navigation`) is the pure rule: `< 600dp` → `BOTTOM_NAV` (the top bar, `nav_bar.xml`),
  `600–839dp` → `NAV_RAIL` (a vertical icon rail, `nav_rail.xml`), `>= 840dp` → `EXPANDED_RAIL` (the
  same rail with every label). `BaseActivity.wrapWithNavBar` reads it once, picks the layout and the
  wrapper orientation (`VERTICAL` for the bar, `HORIZONTAL` for the rail), and the content's enter
  animation axis with it. **Both layouts declare the same ids**, so nothing downstream branches on
  which one inflated — only `navLayoutMode` does, for labels and the indicator axis.
- **The breakpoint is read from `smallestScreenWidthDp`, not `screenWidthDp`.** A rotation flips the
  latter, which would swap a rail for a bar (or the reverse) under the user mid-turn. A window that
  does cross a breakpoint (a foldable unfolding, split screen) recreates in
  `onConfigurationChanged` — that is the one place the shape may change.
- **The rail indicator slides on Y and its length tracks the row height**, via
  `placeIndicator(view, x, y, vertical)`; the bar's slides on X. Both share the first-placement fade
  and the reduced-motion snap, so the two layouts cannot drift.
- `values/bools.xml` (`nav_show_app_name`, `nav_compact_tabs`) and `values-w600dp/{bools,dimens}.xml`
  are **deleted**: the label rule is now a function of `NavLayoutMode`, and a resource qualifier could
  only ever half-apply against a runtime choice. Do not reintroduce them.
