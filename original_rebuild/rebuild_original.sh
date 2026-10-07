#!/usr/bin/env bash
# Reproducible rebuild of the original "Clone Master" APK (com.cmaster.cloner v2.8.0.10).
#
# The full original implementation is preserved 1:1: the untouched original
# classes.dex (verified SHA-256-identical to the shipped APK), all native libs
# (libcm.so, libcmvm.so VM engine), resources, assets and config. The only
# modifications are two manifest attributes that block a standalone install,
# plus our own signing key (required: the Google Play signature is not available).
#
# Usage: ./rebuild_original.sh <path-to-original.apk> <out-signed.apk>
set -euo pipefail

IN_APK="${1:?usage: rebuild_original.sh <original.apk> <out.apk>}"
OUT="${2:?usage: rebuild_original.sh <original.apk> <out.apk>}"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# --- toolchain -------------------------------------------------------------
BT="$(find "${BT_DIR:-/tmp/bt}" -maxdepth 2 -name zipalign | head -1 || true)"
if [ -z "$BT" ]; then
  mkdir -p /tmp/bt && cd /tmp/bt
  curl -sL -o bt.zip https://dl.google.com/android/repository/build-tools_r33.0.2-linux.zip
  unzip -qo bt.zip
fi
BT="$(find "${BT_DIR:-/tmp/bt}" -maxdepth 2 -name zipalign | head -1)"
APKSIGNER="$(dirname "$BT")/lib/apksigner.jar"
APKTOOL="${APKTOOL:-/tmp/apktool.jar}"
KS="${KS:-$PWD/clonekey.jks}"
KS_PASS="${KS_PASS:-clonepass123}"

# --- decode ----------------------------------------------------------------
java -jar "$APKTOOL" d -f -s -o "$WORK/dec" "$IN_APK" >/dev/null

# --- patch manifest for standalone install ---------------------------------
python3 - "$WORK/dec/AndroidManifest.xml" <<'EOF'
import sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
s = s.replace(' android:requiredSplitTypes="base__density"', '')
s = s.replace(' android:splitTypes=""', '')
open(p, "w", encoding="utf-8").write(s)
EOF

# --- rebuild + align + sign ------------------------------------------------
java -jar "$APKTOOL" b "$WORK/dec" -o "$WORK/rebuilt.apk" >/dev/null
"$BT" -f 4 "$WORK/rebuilt.apk" "$WORK/aligned.apk"
java -jar "$APKSIGNER" sign \
  --ks "$KS" --ks-pass pass:"$KS_PASS" --ks-key-alias clonekey --key-pass pass:"$KS_PASS" \
  --out "$OUT" "$WORK/aligned.apk"
java -jar "$APKSIGNER" verify --print-certs "$OUT"

echo "Signed rebuild: $OUT"
