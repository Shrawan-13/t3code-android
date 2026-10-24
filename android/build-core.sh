#!/usr/bin/env bash
# Rebuild the Android arm64 JNI core. Requires Termux clang + Go on this host.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/.." && pwd)"
out="$here/app/src/main/jniLibs/arm64-v8a/libt3core.so"
mkdir -p "$(dirname "$out")"
export PATH="$PATH:/usr/local/go/bin"
cd "$repo/core"
GOOS=android GOARCH=arm64 CGO_ENABLED=1 \
  CC="/data/data/com.termux/files/usr/bin/clang --target=aarch64-linux-android24" \
  CGO_LDFLAGS="-L/data/data/com.termux/files/usr/lib" \
  go build -tags netgo,osusergo -buildmode=c-shared -trimpath -o "$out" ./mobile
python3 "$repo/tools/patch-android-so.py" "$out"
ls -lh "$out"
