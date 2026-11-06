# Pocket PA — Android wireless microphone

An original, free, open-source **Android app** for speaking into your phone's microphone and hearing it through a previously connected **Bluetooth A2DP speaker**. This is an independent implementation, not a copy of Bitechunk's proprietary code or design.

## Features

- One-tap live mic monitor: phone microphone → Android media output (Bluetooth speaker when connected)
- Input gain (0–200%) and output gain (0–150%)
- Live effects: Clean, Echo, Megaphone, Telephone, Robot and Distortion
- Optional noise gate to reduce room noise between speech (may clip quiet words)
- Foreground microphone service: continues after screen lock with visible notification and Stop action
- Bluetooth output detection, route reminder, and quick access to Bluetooth settings
- No Internet permission, accounts, ads, or analytics

## Build in GitHub Actions (no computer needed)

1. Push the `PocketPA` project to a GitHub repository, or use the `bluetooth-mic-app` branch of `Niru-26016/testing-repo` (the app lives at `pocket-pa/` there).
2. Open **Actions → Build Pocket PA APK → latest run**. Wait for the build to succeed.
3. Download the artifact named **PocketPA-Android-APK**. Extract its ZIP, then install `PocketPA-debug.apk` on your Android phone. This is a developer-signed debug APK; Android may require **Install unknown apps** permission for your file manager.

The standalone workflow `/.github/workflows/build-apk.yml` builds the app from project root. The testing-repo workflow is adjusted to build from `pocket-pa/`.

### Local build

Open this folder with Android Studio, or with JDK 17, Android SDK Platform 35 and Gradle 8.11.1 installed run `gradle :app:assembleDebug`.

## Use

1. Pair your phone to a Bluetooth speaker **first**, and select it as the phone's **Media audio** output.
2. Launch **Pocket PA** and allow microphone permission. Bluetooth access is optional but recommended for speaker detection. Notification permission is optional on Android 13+.
3. Keep the phone and speaker separated. Set the output volume low initially.
4. Press **START MICROPHONE**. Adjust gains and effects. Press Stop or the persistent notification action to end the session.

## Technical limitations

- Bluetooth A2DP audio has **hardware / codec / OS buffering latency**. The app cannot guarantee zero delay or remove physical feedback if the speaker is close to the phone.
- Bluetooth headset **hands-free/SCO** mode is not supported as output. Connect an A2DP speaker for better audio.
- A phone may reroute audio when connected/disconnected; reconnect and restart the microphone if necessary.
- Some devices heavily process microphone input; results vary by Android manufacturer.
- Voice effects are lightweight DSP, not studio-quality pitch shifting. This v0.1 does not include deep/chipmunk or multilingual UI.
- Streaming continues after screen lock using a microphone foreground service, but Android can still stop it because of battery restrictions. To start a new session, unlock and open the app.
- This build has **not been physically tested** on your specific phone or speaker.

## Privacy

Microphone samples are processed **in memory on the phone only**, not recorded or uploaded. No network permission. The persistent notification indicates when capture is active.

## License

MIT
