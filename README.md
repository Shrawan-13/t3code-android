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

## Install

Download the latest debug APK:

**[Download the latest t3code-android-debug.apk](https://github.com/Shrawan-13/t3code-android/releases/latest/download/t3code-android-debug.apk)**

Requires Android 8.0+ on an arm64 device. The APK is debug-signed and packages
only `arm64-v8a`. You also need a reachable T3 environment plus an access token
or device pairing link; enter either on the connect screen.

All releases:
<https://github.com/Shrawan-13/t3code-android/releases>

## Features

- Auto-connect with saved token, device pairing, and connection diagnostics
- Grouped, searchable thread list with project filters, pin, rename, archive,
  delete, and mark read/unread
- Live thread timeline: Markdown, reasoning, commands, tool calls, file changes,
  approvals, interrupts, and pull requests
- Composer with image/file attachments, send, queue while running, stop, and
  in-thread model switching
- Per-thread run permissions (ask first, auto-accept edits, auto, full access)
  and plan mode
- Project browser, usage dashboard with charts, provider list, and self-test
- Terminal, workspace file browser, and diff viewer
- Material 3 theme with custom violet palette and system/light/dark modes

## Native core

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
