# Instant Mic 1.0 verification

Release candidate for package `com.niru.pocketpa`, public version 1.0 and Android version code 6. Android 8.0 minimum, Android 16 target. Verify the upload certificate and version-code availability in Play Console before uploading.

## Version 1.0 regression checklist

Built the test APK and unsigned release AAB. Package checks confirmed version code 6, release version 1.0, the expected application IDs and the APK signature. The AAB validated with bundletool and requires the developer’s upload signature. Further functional and latency testing was skipped at the owner’s request. The voice panel screenshot was captured on Android 16.

- Open the voice panel on a fresh install. Check that Clean, Men, Robot and Helium appear in that order, with Clean selected. Tap **Show more options** and check that all 14 other presets are available.
- Select a preset from the expanded panel and collapse it. Check that the active preset label remains accurate, then reopen the panel and check that the selection is still highlighted. Choose Men and check that it uses the existing Man preset.
- Check that the noise gate starts on for fresh installs and that saved on/off choices carry over when updating. Compare end-to-end delay on physical devices. Version 1.0 uses approximately 5 ms PCM chunks, shorter I/O waits and requests Android low-latency playback. The audio route and accessory still affect delay. [Android AudioTrack latency guidance](https://developer.android.com/reference/android/media/AudioTrack#PERFORMANCE_MODE_LOW_LATENCY)

## Previous candidate build and package verification

The following results were recorded on version 2.3.0, version code 5, before the 1.0 voice panel update.

- Built with JDK 17, Gradle 8.14.6 and Android Gradle Plugin 8.13.2. Both debug and release unit test runs passed all five tests, covering all 18 presets at 16, 44.1 and 48 kHz, silence, gain and clipping, noise-gate behavior, echo history and invalid inputs. Debug and release Android lint both reported no issues.
- Verified the installable test APK signature and checked APK alignment. Its package is `com.niru.pocketpa.debug`. The older public GitHub 2.2 debug APK used a different test certificate. Uninstall that build first if Android reports an incompatible update. Uninstalling removes its preferences.
- Validated the release AAB with bundletool 1.18.3 and generated a locally test-signed APK from it for Android 16. Checked the final manifest for version 5 (2.3.0), minimum API 26, target API 36 and microphone foreground-service type. It requests microphone and notification permissions plus the required service permissions. It declares no internet, location or Bluetooth runtime permission and ships no native shared libraries.
- Verified that the delivered AAB is unsigned. Tested the separate signing workflow with a disposable test key. Play upload requires the developer’s upload key.

Run the same build and tests from `pocket-pa/`.

```sh
./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:bundleRelease
```

## Previous candidate emulator verification

| Check | Android 13 (API 33), test APK | Android 16 (API 36), APK from the AAB |
|---|---|---|
| Microphone permission denial followed by grant | Passed | Passed |
| Start with notification permission unset, Android media output reports the phone speaker | Passed | Passed |
| Home, screen off and wake while the service is running | Passed | Passed |
| Portrait/landscape rotation during the session, followed by in-app Stop | Passed | Passed |
| Stop from the notification drawer while the app is in the background | Passed | Passed |
| Revoke microphone permission during playback, then reopen idle | Passed | Passed |
| Select a voice preset and preserve it across rotation | Passed | Passed |
| App crash log during the checks | None | None |

On Android 13, explicitly denying notifications still allowed the mic session. Android’s Active apps control stopped the service. The Android microphone privacy toggle also stopped an active session, and two subsequent start/stop cycles succeeded.

The tests observed the Android audio route, foreground service, notification and app state. They did not measure acoustic playback quality or accessory latency. Android 14 and 15 were reviewed against the platform rules but were not run separately.

## Physical hardware matrix

No physical Android phone or Bluetooth audio accessory was attached to the workstation. The emulator exercises the framework and UI with its virtual handset audio route. Record the phone, OS, accessory model, connection profile, outcome and any measured delay when running this matrix.

| Setup | Checks and expected behavior | Status |
|---|---|---|
| Android 13 phone, handset speaker | Deny microphone then grant. Start and stop several times at low volume. Verify actual voice output, the microphone indicator and clean capture teardown. Repeat without notifications, then enable them and stop from the notification. | Pending |
| Android 14/15/16 phone, handset speaker | Start while app is visible, then test Home, lock, rotation and system microphone toggle. Verify intentional Stop and permission revocation release the microphone. Repeat with large text and TalkBack. | Pending |
| Android 8–12 phone | Pair in Settings, start/stop, allow/deny mic, try background and notification Stop. Confirm the phone sees a compatible media route on its OS version. | Pending |
| Portable Bluetooth speaker (Classic A2DP) | Pair with Media audio enabled, choose the speaker, and confirm the active output label and audible voice. While live, turn off the speaker and verify the app ends the session instead of continuing on a different output. Reconnect and restart deliberately. Record delay, dropouts and any brief tail during routing. | Pending |
| Parked car with Bluetooth media | Select Bluetooth Audio as the car source. Test media playback and delay at low volume. Switch to a call or another source and verify how the session stops or is interrupted. Check vehicle-specific behavior separately from portable speakers. | Pending |
| Earbuds/headphones (A2DP and LE Audio, if available) | Verify sound goes to the chosen accessory and capture uses the phone mic. Switch two connected outputs, remove one earbud, and disconnect. Verify routing labels, restart behavior and audio focus. | Pending |
| AEC on a supported phone and external speaker | At low volume, compare optional echo reduction on and off with Clean. Log whether Android exposes AEC and any audible change. Repeat with phone farther from speaker. Bluetooth delay and acoustic feedback can still occur. | Pending |
| Competing audio and longer use | Trigger a competing call/media session and a system Stop. Restart from the visible app. Run a 15-minute locked session and check dropouts, temperature, battery and final mic release. | Pending |

Use a supported Bluetooth **media** profile. Pairing, codec, buffering and the choice of routable devices depend on Android and the accessory. Phone-to-phone receiving is outside this release.

See [submission checklist](RELEASE.md) and [Play listing](STORE_LISTING.md).
