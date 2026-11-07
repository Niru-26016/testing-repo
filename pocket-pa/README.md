# Instant Mic — Phone to Speaker

Instant Mic relays the phone microphone to a compatible Bluetooth media output, such as a portable speaker, car stereo, or earbuds. Pair in Android Settings, choose the output, then tap **Start microphone**. Use **Stop microphone** in the app or its notification to end the session.

## What’s in 2.3.0

- Choose a connected Bluetooth media output or let Android choose. The app displays the active output and stops when it detects an output change, so you can choose where to restart.
- Clean voice is the default. The app also retains all 18 presets, microphone and output gain, noise gate and sensitivity, and optional Android echo reduction.
- A foreground service keeps a session running when the app moves to the background. The app only starts the microphone after a visible user action and Android microphone permission.
- Audio is processed in memory and sent to the selected media output. There is no saved audio, network permission, advertising, or analytics.

Bluetooth playback delay varies by the phone, codec, and receiver. Android echo reduction is device-dependent and cannot eliminate delay or acoustic feedback. Begin at low volume and keep the phone away from the speaker. Use a car’s Bluetooth media input while parked. Call-only Bluetooth profiles and phone-to-phone receiving are outside the supported workflow.

## Build and test

Install JDK 17 and Android SDK Platform 36. Point `ANDROID_HOME` at the SDK or open this directory in Android Studio. From `pocket-pa/`, run:

```sh
./gradlew :app:testDebugUnitTest :app:lint :app:assembleDebug :app:bundleRelease
```

The app supports Android 8.0+ (API 26), targets Android 16 (API 36), and uses release application ID `com.niru.pocketpa`. Install `app/build/outputs/apk/debug/app-debug.apk` for device testing. The debug package is `com.niru.pocketpa.debug` and uses a test signing certificate. Android may require uninstalling an older test APK with a different signature, which removes its local settings.

See the [release checklist and signing instructions](play-store/RELEASE.md), [store listing](play-store/STORE_LISTING.md), and [privacy policy draft](PRIVACY.md). [Verification and hardware test matrix](play-store/VERIFICATION.md) records what was tested and the remaining physical checks.
