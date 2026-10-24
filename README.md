# T3 Code Android

A Kotlin + Material 3 Android app for T3 Code, backed by a native Go core over
JNI. The Go core speaks the T3 orchestration protocol (Effect RPC over
WebSocket plus the HTTP API) so the Android app does not need a JS runtime.

## Status

- `core/` — Go SDK, RPC transport, live projection reducer, terminal, files,
  git, previews, pull requests, usage, settings, and JNI facade.
- `android/` — Kotlin bindings (`com.t3codes.core`) and a small Material 3
  diagnostics UI that connects, pairs, lists projects/threads, and runs a
  core self-test.
- `android/app/src/main/jniLibs/arm64-v8a/libt3core.so` — prebuilt arm64 core
  used by the app build.

This repository currently ships the arm64-v8a core. Other ABIs can be added by
rebuilding `libt3core.so` for `GOOS=android` with the corresponding NDK target.

## Layout

```
core/          Go module: RPC transport, SDK, CLI, JNI facade
  internal/rpc Effect RPC v4 client
  t3/          typed SDK over the T3 protocol
  mobile/      JNI entry points
  cmd/t3ctl/   terminal client used for live protocol probing
android/       Gradle project (Compose Material 3)
tools/         helpers (Android .so runpath patch)
scripts/       CI helpers (cache pruning)
.github/       CI workflow that builds the debug APK
```

## CI

`.github/workflows/build-apk.yml` builds `:app:assembleDebug` on a GitHub runner
with Gradle dependency and configuration caches, uploads the APK as an
artifact, and then prunes Actions caches down to the newest 20 entries.

## Building locally

Prerequisites: JDK 17+, Android SDK with platform 34 and build-tools 34.0.0.

```bash
cd android
gradle :app:assembleDebug
```

The Go core can be rebuilt from a Termux/Android host with the script
`android/build-core.sh` (it also strips the Termux-only rpath from the shared
library).

## Go core tests

```bash
cd core
go test ./...
```

Live tests read a bearer token from `.secrets/token.txt` (ignored by git) or
`T3_TEST_TOKEN`, and write-path tests additionally require `T3_TEST_WRITE=1`.

Never commit tokens, `.secrets/`, or pairing credentials.
