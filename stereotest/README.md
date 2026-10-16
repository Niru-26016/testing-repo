# EarSplit Stereo Test v0.2

A minimal offline Android test for one stereo pair of Bluetooth or wired earbuds, including Redmi Buds 5.

- Left speaker: generated 440 Hz sine wave
- Right speaker: generated 660 Hz sine wave
- Modes: left, right, or both simultaneously; separate levels.
- No screen capture, microphone permission, internet permission, root, or Shizuku.
- Controls hidden until an earphone output is detected.
- Stops playback on disconnect or when the app goes into the background.
- Uses standard Android AudioTrack in stereo PCM mode.
- This is NOT per-app audio separation for Spotify, Instagram, etc. It only establishes whether the output channels work.

## Build
Open `stereotest/` in Android Studio, or run `gradle :app:assembleDebug` from that directory with Android SDK 35.

## Safety
Set the phone volume low before tapping Play. This is a test build, not device-certified.