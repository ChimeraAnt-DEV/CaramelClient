## Recovering the build toolchain in a fresh container
A recreated dev container can come up with the source tree and previous build outputs intact but the JDK, Android SDK and `~/.gradle` cache all gone (`/opt/android-sdk` missing, no `java` on `PATH`). The repo's `local.properties` is gitignored, so it points at a path that no longer exists. Nothing in the project is broken in that state; the toolchain just needs reinstalling. `apt-get` will not work (no root), so install under a writable prefix and point `local.properties` at it:

```
# JDK 21 (Temurin, matches the toolchain named above)
mkdir -p /workspace/tools && cd /workspace/tools
curl -sL -o jdk21.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar xzf jdk21.tar.gz && export JAVA_HOME=/workspace/tools/jdk-21.0.12.1+1

# Android cmdline-tools, then the packages the build actually needs
mkdir -p /workspace/android-sdk/cmdline-tools && cd /workspace/android-sdk/cmdline-tools
curl -sL -o /tmp/cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
python3 -c "import zipfile;zipfile.ZipFile('/tmp/cmdtools.zip').extractall('.')" && mv cmdline-tools latest
chmod -R +x /workspace/android-sdk/cmdline-tools/latest/bin
yes | latest/bin/sdkmanager --sdk_root=/workspace/android-sdk --licenses
nohup latest/bin/sdkmanager --sdk_root=/workspace/android-sdk \
  "platform-tools" "platforms;android-36" "platforms;android-35" \
  "build-tools;36.0.0" "build-tools;35.0.0" "ndk;28.2.13676358" "cmake;3.22.1" \
  > /tmp/sdk.log 2>&1 &

printf 'sdk.dir=/workspace/android-sdk\n' > /workspace/project/CaramelClient/local.properties
```

Notes: `unzip` is not installed тАФ extract with `python3 -c "import zipfile..."` instead. The zip contains a `cmdline-tools/` directory that must be renamed to `latest` or `sdkmanager` refuses to run. `sdkmanager` runs long; start it with `nohup ... &` and poll `/tmp/sdk.log` (a foreground command with a large timeout is rejected). Cmdline-tools land non-executable, hence the `chmod`. Budget ~3 GB for the SDK plus ~200 MB for the JDK.
