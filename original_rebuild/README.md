# Original APK — 1:1 Rebuild

This folder documents how the **fully working original app** ("Clone Master",
`com.cmaster.cloner` v2.8.0.10) was reproduced from the provided APK — reusing
the original implementation verbatim rather than rewriting it.

## What is preserved (unchanged)

- **`classes.dex`** — the original bytecode, byte-identical (verified:
  SHA-256 `4afa02f4567c8f1972f78b20311f8d2a7505c81c970a5330944e0722f468fdef`
  matches the shipped APK). All 4,911 original classes, including the runtime
  string-decryption loader.
- **Native libraries** — the entire VM engine: `libcm.so`, `libcmvm.so` +
  support libs (`libimage_processing_util_jni.so`, `libPPNDZQ*.so`,
  `libsurface_util_jni.so`), both `arm64-v8a` and `armeabi-v7a`.
- **Resources / layouts / drawables / colors / dimens / menus** — decoded by
  apktool from ARSC and re-encoded.
- **Assets** — `baseline.prof`, `PublicSuffixDatabase.list`, etc.
- **Dependencies** — original versions of AdMob, Firebase, MLKit, Room, Work,
  CameraX baked into the DEX.
- **Navigation / manifest** — original components, deep links, services, with
  the minimal patch below.

## The only modification

Two manifest attributes from its Play-Store split-APK delivery would make a
standalone APK refuse to install, so they were removed:

- `android:requiredSplitTypes="base__density"`
- `android:splitTypes=""`

Nothing else was changed. No smali was recompiled (the DEX is untouched).

## Rebuild pipeline

見 `rebuild_original.sh` — the exact steps:

1. `apktool d -s` (sources kept as original DEX, resources decoded)
2. patch the two manifest attributes
3. `apktool b` (rebuilds resources around the original DEX)
4. `zipalign -f 4` (build-tools 33.0.2)
5. sign with apksigner (v1 + v2 schemes) using a local keystore
6. verify: `apksigner verify --print-certs`

Result: `clone_signed.apk`, ~12.3 MB, installs as a normal standalone APK.

## Verification performed

| Check | Result |
|---|---|
| `classes.dex` SHA-256 vs original | identical |
| File count | 514 vs 513 (adds the v2 signature block) |
| Native libs | all 6 present |
| `apksigner verify` | PASS (v1+v2) |
| Emulator/device smoke test | not possible in this sandbox (no SDK/adb/KVM) |

## Legal note

The APK is the user's own copy of an app they use; this rebuild exists so they
can run the app they already have. Respect the original developer's rights;
don't redistribute it.
