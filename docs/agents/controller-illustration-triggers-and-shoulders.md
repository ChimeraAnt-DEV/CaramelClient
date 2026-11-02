## Controller illustration: triggers and shoulders
- **`ControllerLayout` triggers were raised** (`lt`/`rt` y ~ 0.096 Xbox, 0.112 PlayStation) and
  `Spec.halfHeight()` now returns the trigger's real extent (`r * 0.95f`, not the circle default).
  At the old height the lever sat entirely behind the body and only an invisible sliver poked out --
  the "back triggers don't show" report.
- `drawTrigger` is a knee-raised lever (sides rise via knee segments to a lifted crown) with a lit
  crown band and a **recessed piston face**, so a front-on trigger reads as a moving part rather
  than a flat pill. It is intentionally not clipped to the shell; the crown is meant to poke above
  the body edge. `ControllerLayoutTest.triggersStayWithinReachOfTheShell` bounds it.
