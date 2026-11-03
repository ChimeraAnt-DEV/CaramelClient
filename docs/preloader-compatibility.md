# Preloader and Minecraft compatibility

Which Minecraft Bedrock builds the preloader's hook rules cover, and what happens on a build they do
not. The rules themselves live in `resources/preloader/preloader_signature_rules_source.json` and are
delivered to the native side at launch; this page is the human-readable index of that file.

The rule file is the source of truth. This table is regenerated from it and a test
(`PreloaderCompatibilityDocTest`) fails when the two disagree, so a rule added without a doc update
is a red build rather than a silently stale page.

## Support policy

- **A build is supported when a rule's range contains it.** The rules are per-range, not per-exact
  build, so a point release inside a range is covered without a new rule.
- **A build above every range is unsupported, not broken.** The client warns, the hooks
  fail closed, and the launcher still starts the game — no hook means no in-game modules, not a
  crash.
- **A build below every range is unsupported the same way.** There is no floor rule older than the
  oldest entry in the table.
- **The signature set is versioned by `revision`.** A new rule file is published separately from the
  app, signed, and verified by the client before use (see `preloader-rule-signing.md`), so a new
  Minecraft build needs a rule update, not an app update.

## Covered builds

The table is generated from `preloader_signature_rules_source.json` (revision 7 at the time of
writing). "Hooks" is the set of native seams the range resolves for; an unresolved seam disables
only the features that need it.

<!-- BEGIN GENERATED: preloader-compatibility -->
| Range (min - max) | Hooks resolved |
| --- | --- |
| 1.21.132 - 1.26.39.99 | floor rule: the 1.26.30 signatures and the render slot |
| 1.26.30.32 - 1.26.30.32 | per-build signatures |
| 1.26.31 - 1.26.31 | per-build signatures |
| 1.26.32 - 1.26.32 | per-build signatures |
| 1.26.33 - 1.26.33.99 | per-build signatures (widened to cover 1.26.33.1) |
| 1.26.40.00 - 1.26.40.19 | 1.26.40 signatures |
| 1.26.40.20 - 1.26.40.20 | per-build signatures |
| 1.26.40.26 - 1.26.40.26 | per-build signatures |
| 1.26.40.27 - 1.26.40.27 | per-build signatures |
| 1.26.40.30 - 1.26.40.99 | 1.26.40 signatures |
| 1.26.50.00 - 1.26.50.19 | 1.26.50 signatures |
| 1.26.50.20 - 1.26.50.23 | per-build signatures |
| 1.26.50.24 - 1.26.50.24 | per-build signatures |
| 1.26.50.25 - 1.26.50.25 | per-build signatures |
| 1.26.50.26 - 1.26.50.99 | 1.26.50 signatures |
<!-- END GENERATED: preloader-compatibility -->

## Adding a build

1. Derive the patterns with `tools/verify_binary.py` against the new `libminecraftpe.so`.
2. Add a rule to `preloader_signature_rules_source.json`, widen an existing range when the patterns
   are unchanged, and bump `revision`.
3. Re-sign the file (`scripts/sign_preloader_rules.py`).
4. Update the generated table above; the test will fail until it matches.

## Known limitations

- The engine image loader signature (`imageLoaderSig`) is build-specific and matches zero places on
  some newer builds; when it does not match, the cape seam stays off and the feature falls back
  rather than patching a wrong address.
- The player-render hook resolves its vtable slot by RTTI name at runtime, so it survives a version
  bump that a byte pattern would not, but the detour only draws once a mesh is published; a slot
  that resolves to a non-executable address is refused.
