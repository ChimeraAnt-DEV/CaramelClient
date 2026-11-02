## 3D controller illustrations (Controller3DProjection + Controller3DView)
- The flat `ControllerIllustrationView` read as a squashed top-down sketch and hid the shoulder
  triggers behind the shell. `Controller3DView` is the replacement, used by the controller settings
  screen, the mod-menu bind picker and the bind summary badge.
- **The view owns no geometry.** `Controller3DProjection` (Android-free, in the controller package)
  projects the same `ControllerLayout.regionTable` the 2D view uses through a small camera pitch and
  a perspective divide, returning plain `x,y,radius,z`; the view only paints. That keeps the
  projection testable тАФ `Controller3DProjectionTest` pins the recede, the perspective sign, the
  shoulder lift and the far-to-near sort, because a projection defect looks plausible on screen.
- **Shoulder controls are lifted toward the camera (`SHOULDER_Z = -0.55f`), not pushed away.**
  Pushing them "behind" the body under this pitch would draw them smaller and further out; the
  lift toward the camera (so the tilt cannot bury them) plus the body painting over their lower
  edge is what makes bumpers/triggers read as attached to the top. They draw in a shoulder pass
  **before** the body.
- `Controller3DView.animateConfirm(id)` is the green "got it": a `ValueAnimator` on two floats with
  a single overshoot, invalidating only. Short and allocation-free per frame, so the bind dialog
  never touches the game's frame budget.
- Package split to remember: the view is `org.chimeramc.client.launcher.ui.views.Controller3DView`
  while `ControllerLayout` is `org.chimeramc.client.ui.views`; XML refs must use the `launcher`
  path or inflation fails.
