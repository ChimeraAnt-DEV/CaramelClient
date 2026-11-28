## Launcher layout: resize (scale) and rearrange (columns)

Two separate jobs, two separate rules, and they must not be confused again:

- **`UiResizeRule` + `CaramelWindowScale`** — *scales* the dp layout for the window it is in
  (0.88 below 320dp shortest side, 1.0 on a phone, up to 1.12 on a large tablet). It scales the
  density, not the font, so padding, radii and glyphs move together. It measures the window in
  pixels first: reading it in dp from inside the scaled scope feeds the new density back into its own
  constraints and oscillates.
- **`CaramelLayoutRule`** — *rearranges* the content: `newsColumns()` (1 below 560dp, 2 to 839dp, 3
  from 840dp), `homeSplitsSideBySide()` (the hero keeps a readable width beside the fixed 374dp side
  column from 720dp), `sideColumnWidth()`, `contentMaxWidth()` and `gutter()`. Bands are Material's
  own window-size classes (600dp, 840dp), so a window changes shape where Android developers already
  expect it to.

`CaramelHomeScreen` reads the window with `BoxWithConstraints` and picks the two-column or the
one-column arrangement. The one-column arrangement scrolls: hero, news header, news strip, friends,
promo. A short last row of news cards is padded with `Spacer(weight(1f))` rather than stretched.

**Height matters as much as width, and the phone here is 800x360dp, not 411dp.** The Mobilebox phone
is 2400x1080px at density 3.0, so landscape is 800dp wide and only 360dp tall. Three things follow,
and all three were measured on the phone (screenshots before and after):

- The split threshold is 720dp, not 900dp. At 800dp the two columns do fit, and they are the only
  arrangement that shows the hero and the news in one 360dp-tall window; at 900dp this phone stacked,
  which pushed the news and the friends card a screen down.
- **`Hero` is responsive**, because a 307dp-wide hero cannot hold the design's fixed 210dp art, its
  fixed 340dp launch button and a three-button row. `BoxWithConstraints` inside it switches to a
  compact composition below 560dp wide or 240dp tall: the art is dropped (the top bar already carries
  the mark), the launch button becomes `fillMaxWidth().widthIn(max = 340.dp)`, and the Profiles /
  Instances / Screenshots row is a `FlowRow` that wraps instead of running off the right edge. Before
  this the buttons drew on top of the apple art and `Screenshots` was clipped.
- **The left column scrolls and the side column adapts.** `heroHeight = min(320.dp, height * 0.62f)`,
  the news strip takes its natural height inside a `verticalScroll` column, and the side column gives
  the friends card `weight(1f)` (dropping the promo card entirely) when the window is shorter than
  600dp. Before this a news card was clipped at the bottom edge and the promo card was a 20dp sliver.

Verified on the phone: hero compact and complete, three action buttons on one row inside the hero,
the left column scrolling to the full news cards, the friends card filling the side column, no
crash (`exits` in logcat are only `package_updated` from the installs). Still unverified: a tablet
width (>=840dp, three news columns, scale 1.12) and a portrait phone (stacked arrangement) — neither
shape exists on this device, so those branches are pinned by `CaramelLayoutRuleTest` only.
