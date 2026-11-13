## One package format name (no `.AntEgg`, `.chimerapack` or `.levipack`)
- **`.AntEgg` is removed, along with `.chimerapack` and `.levipack`.** The launcher no longer
  accepts a named mod archive: an import is either a native `.so`, a bare `.zip` (the
  unpacked-layout import), or a `.universe` / `.singularity` package handled by the SweetLoader
  Rust engine. `FileHandler` accepts only those, the manifest lists only those filters, and
  `invalid_mod_file_reason` names only those in every locale.
- `grep -rn -i antegg app/src` must return nothing.
- Adding a format back means touching `FileHandler`, the manifest intent filters, every locale's
  string, and the JNI parity table. Do not re-add a name to one place only.
