# T3 Code Android

A Kotlin + Material 3 Android client for T3 Code. The Go core talks to a T3
environment over the orchestration protocol (Effect RPC over WebSocket plus the
HTTP API) and exposes a small pull-based JNI facade to Kotlin, so the app does
not need a JS runtime.

## Repositories

- **App** — this repository: Compose UI, Kotlin bindings, packaging, CI.
- **[t3code-core](https://github.com/Shrawan-13/t3code-core)** — Go core,
  protocol transport, projections, terminal/files/git APIs, JNI facade, and
  cross-compiled Android libraries.

Native libraries are **not** committed here. The app CI downloads the arm64
library from a `t3code-core` release:

```text
https://github.com/Shrawan-13/t3code-core/releases/download/v0.1.0/libt3core-arm64-v8a.so
```

The core repository also publishes `armeabi-v7a` and `x86_64` builds. The debug
APK currently packages `arm64-v8a` only; add the other ABI filters and download
steps when they are needed.

## Layout

```
android/            Gradle project (Compose, Material 3)
  app/src/main/java/com/t3codes/core/    Kotlin JNI bindings and models
  app/src/main/java/com/t3codes/mobile/  App UI and state
scripts/            CI helpers (cache pruning)
.github/            Debug APK workflow
```

## Build

CI builds on pushes to `main` and uploads `t3code-android-debug`:

```bash
gh workflow run build-apk.yml
```

Locally, fetch the core first:

```bash
mkdir -p android/app/src/main/jniLibs/arm64-v8a
curl -fL -o android/app/src/main/jniLibs/arm64-v8a/libt3core.so \
  https://github.com/Shrawan-13/t3code-core/releases/download/v0.1.0/libt3core-arm64-v8a.so
cd android && gradle :app:assembleDebug
```

Requirements: JDK 17+, Android SDK 34, build-tools 34.0.0.

Never commit tokens or pairing credentials.
