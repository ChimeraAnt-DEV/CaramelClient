// A minimal GlowberryClient native mod: one config value and one lifecycle object.
//
// It is the on-ramp for a contributor. The whole API surface a first mod needs is
// `PL_REGISTER_MOD`, a lifecycle object, and a typed config; everything else in the SDK is
// optional. The example deliberately installs no hook, so it is safe to load on any build - a mod
// that hooks an address it did not verify is how a session dies.
//
// Build (from this directory, with the Android NDK on PATH):
//
//   cmake -B build -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
//         -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=28
//   cmake --build build
//
// The result is `libminimal_mod.so`; package it with the manifest in `manifests/`.

#include <optional>
#include <string>
#include <string_view>

#include <pl/Config.hpp>
#include <pl/Logger.hpp>
#include <pl/Mod.hpp>

namespace {

/// The mod's config. Every field is reflected into `config.json` and `config.schema.json`, so a
/// new field is one line here plus one entry in the `Schema` specialisation below.
struct MinimalConfig {
  int version = 1;
  bool enabled = true;
  std::string label = "Minimal Mod";
};

} // namespace

namespace pl::config {

template <> struct Schema<MinimalConfig> {
  static constexpr std::string_view title = "Minimal Mod";
  static constexpr std::string_view description =
      "One toggle and one label, the smallest useful config.";

  static constexpr FieldSchema field(std::string_view name) {
    if (name == "version") {
      return {"Version", "Config schema version.", std::nullopt, std::nullopt, true};
    }
    if (name == "enabled") {
      return {"Enabled", "Whether the example feature is active.", std::nullopt,
              std::nullopt, false};
    }
    if (name == "label") {
      return {"Label", "Text the mod logs on load.", std::nullopt, std::nullopt, false};
    }
    return {"", "", std::nullopt, std::nullopt, false};
  }
};

} // namespace pl::config

/// The mod's lifecycle object. The preloader materialises it once and calls the four hooks.
class MinimalMod {
public:
  static MinimalMod &instance() {
    static MinimalMod mod;
    return mod;
  }

  bool load(pl::mod::ModContext &) {
    // ConfigFile resolves its own path from the mod's config directory, so the mod never builds a
    // filesystem path by hand. `load()` merges the file over the defaults, writes the file back
    // when it is missing or stale, and writes the schema beside it.
    mConfig.emplace();
    if (!mConfig->load()) {
      log().warn("could not load config.json; using defaults for this session");
      mConfig.reset();
      return true;
    }
    log().info("loaded, label='{}'", mConfig->value().label);
    return true;
  }

  bool enable(pl::mod::ModContext &) {
    if (mConfig.has_value() && !mConfig->value().enabled) {
      log().info("disabled by config; nothing to do");
      return true;
    }
    log().info("enabled (this example installs no hook)");
    return true;
  }

  bool disable(pl::mod::ModContext &) {
    log().info("disabled");
    return true;
  }

  bool unload(pl::mod::ModContext &) {
    log().info("unloaded");
    return true;
  }

private:
  pl::log::Logger &log() { return pl::mod::NativeMod::current()->getLogger(); }

  std::optional<pl::config::ConfigFile<MinimalConfig>> mConfig;
};

// The one export the preloader looks for: the macro emits `PLGetModRegistration`.
PL_REGISTER_MOD(MinimalMod, MinimalMod::instance())
