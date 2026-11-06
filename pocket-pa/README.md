# Pocket PA 2.0 — live Bluetooth microphone and voice studio

Pocket PA is an **original, offline Android microphone app**, not a copy of another company's code. Speak into your phone and hear the processed voice on your selected audio output (including connected Bluetooth speakers). Bluetooth codec delay varies by device and cannot be eliminated in software.

## What's in v2
- Redesigned dark UI with a live audio level meter and large one-tap Start/Stop control
- 12 local voice presets, including dual-window approximate pitch effects
- Input and output gain controls; optional noise gate
- Foreground microphone service with notification Stop action
- Android 8.0+ (`minSdk 26`), **Android 16 target (`targetSdk 36`)**
- No network permissions, no recording, no ads, no analytics

## Build

Use JDK 17 and Gradle 8.11.1 from `pocket-pa/`:

```sh
gradle :app:assembleDebug
# Unsigned, not publishable:
gradle :app:bundleRelease
```

The GitHub Actions workflow also runs local DSP smoke tests and generates a debug APK and release bundle. For Play submission, configure a privately held upload key as documented in [`play-store/RELEASE.md`](play-store/RELEASE.md). Without signing secrets, release AAB is **unsigned and not Play-uploadable**.

## Permissions

- `RECORD_AUDIO`: live mic after explicit Start.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE`: mic session while app is not foregrounded, with notification.
- `BLUETOOTH_CONNECT`: Bluetooth device name detection; optional on supported Android releases.
- `POST_NOTIFICATIONS`: optional notification in supported Android versions.

See [`PRIVACY.md`](PRIVACY.md) for the privacy policy and [`play-store/STORE_LISTING.md`](play-store/STORE_LISTING.md) for Play Store copy and release checklist.

## Known limits

- Echo, Cave, and pitch shifts use lightweight DSP; quality depends on the handset and speaker.
- Some Bluetooth routes may have a significant delay or behave differently on different manufacturers' Android versions.
- Live recording on Android is subject to microphone privacy restrictions; start while the app is visible.
- Test the unsigned-debug APK on several real phones/speakers before publishing. The build does not prove UX or low latency.
