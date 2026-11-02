## Text input path (Enter-in-chat crash)
- A committed control code point (Enter arrives as `'\n'` from several IMEs) must not be replayed
  as a synthetic character key - that is what crashed the instance. `TextInputSanitizer`
  (`sanitizeUnicodeChar` for the hardware path, `isKeyFallbackCodePoint` for the synthetic-key
  fallback) encodes the rule; the preloader's `nativeOnTextInput` skips non-printable code points
  too, and the dedicated key code still delivers the Enter press. The preloader also wraps every
  registered input callback in a try/catch so a throwing mod reads as "not consumed" instead of
  unwinding through the game's dispatch and killing the session.
