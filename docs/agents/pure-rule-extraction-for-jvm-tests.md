## Pure-rule extraction for JVM tests (VersionCodeNormalizer)
- A rule that lives inside a class whose **static initializer loads a native library** cannot be
  unit-tested: initialising `LibBindings` in a plain JVM test throws `ExceptionInInitializerError`
  (its `System.loadLibrary` + `android.util.Log` fail off-device). The fix is to extract the pure
  part (here version parsing) into a standalone class - `VersionCodeNormalizer` - and have the JNI
  class delegate to it. `VersionCodeNormalizerTest` then covers the rule with no device and no
  mocks. Apply the same shape to any other pure rule otherwise trapped behind a native-loading
  class.
