# Clone Master — Recreated

A new Android project that **recreates the UI, theme, navigation and user-facing behavior**
of the analyzed APK (`com.cmaster.cloner` "Clone Master" v2.8.0.10), built from scratch —
no proprietary code was copied.

## Build

```bash
cd CloneMasterRecreated
# Gradle 8.9 (AGP 8.5.2) — Android Studio's bundled Gradle works out of the box
gradle assembleDebug       # or open in Android Studio
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires: JDK 17, Android SDK 35. No Android Studio needed.

## What the original APK actually is (reverse-engineering summary)

| Aspect | Finding |
|---|---|
| Identity | `com.cmaster.cloner`, sharedUserId `com.cmaster.cloner`, **"Clone Master"**, v2.8.0.10 (code 20080) |
| Purpose | Dual-app / parallel-space tool ("Batch Clone", "Clone Mode") |
| Engine | Native container VM: `libcm.so` + `libcmvm.so` (clang 19) + stub `FIEL$Proxy*` IPC services + `AliveService` (specialUse FGS, "Container App Message Channel") |
| Screens | `HomeActivity` (banner → search → space-card list → FAB), `RouterActivity` (encrypted class-name routing), QR preview/history (MLKit), `UpdateActivity`, device-spoofing editor (PRODUCT/BRAND/MODEL/DEVICE + restore/reset), Free-VIP page (coins, redeem, plans) |
| UI stack | XML + AppCompat + Material + DataBinding; theme green `#4caf50` primary; system font (no TTF/OTF shipped) |
| SDK | minSdk 32, targetSdk 35, compileSdk 37 |
| Monetization | AdMob (banner + offline-ads dialog), Firebase, VIP/coins economy |
| Obfuscation | R8 full renaming (4,911 app classes) + runtime string decryption (`sn2.OooO00o`) |
| Network | No custom backend endpoints found — only Google/Firebase/AdMob infra |
| Privacy flags | ~90 permissions (superset so clones can inherit them); QUERY_ALL_PACKAGES, fine location, contacts, calendar, media. No SMS/call/IMEI exfil code paths found in app package. |

## What this recreation includes

- **Theme**: exact primary color, Material3 mapping, spacing dims (4/8/16/32dp), 48dp search, 40dp icons.
- **Home**: banner slot, toggleable search bar ("Search App/Space"), loading/empty states, space-card grid with red corner mark + overflow icon, FAB.
- **Apps picker**: installed launchable apps, permission-error state like the original.
- **Device properties**: Restore/Reset/Save/Edit, PRODUCT/BRAND/MODEL/DEVICE fields, persisted in Room.
- **Free VIP**: coins, VIP status, plan radios, redeem code (`CLONE-FREE` grants 1 demo day).
- **QR scanner + history**: CameraX + MLKit (bundled model, offline), scan list persisted.
- **Rate dialog**: shows once after 3 spaces exist.

## Honest functional differences

- The original's core feature — running multiple app instances inside a **native container VM**
  — cannot be recreated from static analysis; it is a proprietary engine (`libcm.so`/`libcmvm.so`).
  This project launches the real installed app via `PackageManager` instead (a launcher shell).
- Device "spoofing" values are stored locally only; no hidden-API hooks.
- Ads/VIP are stubs (no AdMob SDK wired); search uses local filtering.

## Emulator note

The sandbox has **no Android SDK, no adb, no emulator, and no `/dev/kvm`**, so the app could
not be run or built here — static analysis (apktool 3.0.3 + jadx 1.5.6) was verified instead.
Build it on your machine to see it live.
