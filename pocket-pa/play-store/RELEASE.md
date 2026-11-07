# Instant Mic Play Console submission checklist

Policy checked 7 November 2026. Release candidate **1.0**, version code **6**, package `com.niru.pocketpa`. The app supports Android 8.0 and later and targets Android 16 (API 36).

## 1. Sign the release bundle

The testing APK, `InstantMic-v1.0-test.apk`, is debug-signed and installs as `com.niru.pocketpa.debug`. The release AAB, `InstantMic-v1.0-UNSIGNED-release.aab`, requires your upload signature before Play Console will accept it. Both are in `Instant-Mic-1.0-Play-Store-Kit.zip`.

- If Instant Mic already has a Play listing, open that listing and use its registered **upload key**. If you lost it, request an upload-key reset in Play Console. Check that version code 6 is unused and higher than previous uploads. If needed, bump the version code in `app/build.gradle` and rebuild before signing.
- For a first listing, create an upload key on a trusted machine. Use a private directory outside the source checkout and an encrypted backup. The following commands prompt for passwords. Replace the example paths with your own. Use JDK 17 or newer for these commands.

```sh
umask 077
keytool -genkeypair -keystore "/private/path/instant-mic-upload.p12" \
  -storetype PKCS12 -alias instant-mic-upload -keyalg RSA -keysize 4096 \
  -validity 10000
```

Sign a separate copy of the delivered AAB. If using an existing key, substitute the registered keystore path and alias.

```sh
jarsigner -keystore "/private/path/instant-mic-upload.p12" \
  -signedjar "InstantMic-v1.0-signed-release.aab" \
  "InstantMic-v1.0-UNSIGNED-release.aab" instant-mic-upload
jarsigner -verify -verbose -certs "InstantMic-v1.0-signed-release.aab"
keytool -printcert -jarfile "InstantMic-v1.0-signed-release.aab"
```

Verify that `jarsigner` reports a verified signature. A newly generated upload certificate is self-signed. For an existing listing, compare the SHA-256 fingerprint with the **upload key certificate** in Play Console’s App integrity page. If Play asks for the public certificate, export only that certificate using `keytool -exportcert -rfc -keystore "/private/path/instant-mic-upload.p12" -alias instant-mic-upload -file instant-mic-upload.pem`. Keep the private keystore and passwords out of Git, the deliverables, chat and shell history.

To build from source, open `pocket-pa` with JDK 17 and Android SDK Platform 36. Run `./gradlew :app:bundleRelease`. With the four signing variables unset, this creates an unsigned bundle at `app/build/outputs/bundle/release/app-release.aab`. To sign during a local build, inject all four variables together from your local secret manager or password prompts. Set `POCKETPA_KEYSTORE_FILE` to an absolute path and supply `POCKETPA_KEYSTORE_PASSWORD`, `POCKETPA_KEY_ALIAS` and `POCKETPA_KEY_PASSWORD`. The repository workflow builds testing artifacts without upload keys. [Android app signing](https://developer.android.com/studio/publish/app-signing), [Command-line signing](https://developer.android.com/build/building-cmdline), [Play App Signing and upload key resets](https://support.google.com/googleplay/android-developer/answer/9842756)

## 2. Verify the developer account and create or open the listing

- Complete any Play Console Home identity and contact verification. For a new personal account, use the Play Console mobile app on a physical, non-rooted Android 10+ device. For an organization account, provide the organization verification details and D-U-N-S number where required. [Developer identity verification](https://support.google.com/googleplay/android-developer/answer/10841920), [Device verification](https://support.google.com/googleplay/android-developer/answer/14316361)
- Complete the trader-status declaration if shown. Google requires it at account creation for new accounts and by 1 February 2027 for existing accounts worldwide. Use your actual business status. [Trader status](https://support.google.com/googleplay/android-developer/answer/17594752)
- Open the existing Instant Mic listing if you already own it. For a new listing, use **Create app**, choose English (United States), select **App**, and choose the intended pricing and countries. Keep `com.niru.pocketpa` if it is available to this account. Check verification and package registration status on Play Console Home. [App setup](https://support.google.com/googleplay/android-developer/answer/9859152), [Android developer verification](https://developer.android.com/developer-verification)
- Open **Grow users → Store presence → Main store listing**. Paste the title and descriptions from `STORE_LISTING.md`. Upload the icon, feature graphic and actual final-build screenshots to the relevant slots. Enter the category and your verified, monitored support email in Store settings. [Store assets](https://support.google.com/googleplay/android-developer/answer/9866151)

## 3. Host the privacy policy and complete App content

- Add a direct privacy contact you control to `privacy.html`, ideally the same monitored address as the listing’s support email. Host the page at a stable public HTTPS URL. Verify from a signed-out browser that it loads without login, geographical restrictions or edit controls. Paste the URL into **App content → Privacy policy**. The app includes offline policy text. Keep the hosted policy and the in-app text consistent. [Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311), [App review setup](https://support.google.com/googleplay/android-developer/answer/9859455)
- Complete **Ads** (No), **App access** (all functionality is available without an account), **Health apps** (no health features), **Financial features** (none), and any other declarations Console shows. Complete the IARC questionnaire and select the actual intended audience. Selecting ages that include children triggers Play Families requirements. [App review setup](https://support.google.com/googleplay/android-developer/answer/9859455), [Health declaration](https://support.google.com/googleplay/android-developer/answer/14738291), [Financial features declaration](https://support.google.com/googleplay/android-developer/answer/13849271), [Families policy](https://support.google.com/googleplay/android-developer/answer/9893335)
- Reviewer test instructions if needed. Pair a compatible Bluetooth media accessory in Android Settings and select it in Instant Mic. Tap Start, allow microphone access and speak. With notifications enabled, return to Home, expand the live microphone notification and tap Stop. Without a Bluetooth accessory, the reviewer can exercise the controls using system audio output at a low volume.

### Data Safety working answers

| Flow | Release behavior and draft answer |
|---|---|
| Microphone processing on the phone | Audio is processed in memory after Start. The app writes no recordings, uses no server, and retains no audio after the session. On-handset playback alone is outside the Data Safety collection scope. |
| Bluetooth media output | When the user routes playback to a connected accessory, Android sends live audio off the handset to that accessory. The conservative form interpretation is **Yes** to the initial data collection/sharing question. Declare **Audio files → Voice or sound recordings** as **Collected**, **processed ephemerally**, **optional** (people can use the phone output), purpose **App functionality** only. |
| Sharing | The accessory is a user-selected audio destination, and the app sends no data to a third-party organization. Draft answer **No data shared**. |
| Device information and settings | Android supplies connected audio output names for route selection. Names are displayed locally. User settings stay on the handset in app storage. No separate device identifiers, settings or diagnostic information are uploaded by the app. |
| Security and deletion | Use the app or notification to stop live audio. Clear app storage or uninstall to remove preferences. No app accounts or server records exist. If the form asks whether all collected data is encrypted in transit, answer No unless you verify that every disclosed transfer qualifies. |

The Bluetooth row is a conservative application of Google’s off-device transmission definition. Google’s public guidance gives no specific example for local Bluetooth media playback routed by Android. Confirm that interpretation and review the generated store label before submitting. If Play treats this flow as outside collection, the form answer may be **No** because there is no other collection or sharing in this build. Data Safety applies across distributed builds, so also check any versions already active on your listing. [Data Safety definitions and ephemeral processing](https://support.google.com/googleplay/android-developer/answer/10787469)

### Microphone foreground service declaration

In **App content → Foreground service permissions**, declare **Microphone / Background audio access** for `FOREGROUND_SERVICE_MICROPHONE`. Runtime `RECORD_AUDIO` is necessary to capture live sound. `POST_NOTIFICATIONS` is optional on Android 13+ and enables the active-session notification and Stop action in the notification drawer. If permission is denied, Android 13+ shows the service in its active apps manager. Pair devices in Android Settings. The app uses Android media routing with no Bluetooth, location or internet permission. [Foreground service declaration](https://support.google.com/googleplay/android-developer/answer/13392821), [Foreground service policy](https://support.google.com/googleplay/android-developer/answer/16559646), [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

- **Function.** The user taps Start from the visible app to send live microphone audio to the selected media output. The microphone foreground service keeps the session running if the user moves away from the app. The app and active-session notification provide Stop. Capture ends when the session stops.
- **If deferred.** The audience would miss the beginning of the user’s speech because live playback must begin when the user starts the mic.
- **If interrupted.** The audience hears a gap or loses live audio entirely. The user must return to the app and start a new session after an interruption.
- **Demo video.** Capture the actual app on a phone with a compatible accessory. Show the selected audio output, the tap on Start, the microphone permission if prompted, the live state, then Home and the live microphone notification. Tap Stop there and show the app back at idle with the mic indicator cleared. Use low volume and a short spoken test phrase. Host a copy at a reviewer-accessible link approved by the account owner and paste the link into the declaration.

## 4. Upload, test and release

1. Go to **Test and release → Testing → Internal testing → Create new release**. Follow the Play App Signing setup if prompted. For new apps, Play defaults to generating and managing the signing keys used for distributed APKs. You retain the separate upload key. Upload the signed AAB and add the release notes. [Create a release](https://support.google.com/googleplay/android-developer/answer/9859348), [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
2. Confirm the package, target SDK, version code, upload certificate and device coverage in the release preview or App Bundle Explorer. Since 31 August 2026, a new or updated handset app must target Android 16/API 36 or higher. The extension date was 1 November 2026. Play also checks 64-bit and 16 KB page-size support where an app ships native libraries. Java-only bundles are compatible by default. Check the final AAB archive for `.so` entries and investigate any Play compatibility warning. [Target API policy](https://support.google.com/googleplay/android-developer/answer/11926878), [Play technical requirements](https://support.google.com/googleplay/android-developer/answer/17492799), [16 KB Android guide](https://developer.android.com/guide/practices/page-sizes)
3. Install the Play-delivered build through the internal tester link and review the pre-launch report if available. Run the device matrix with physical hardware, including a Bluetooth speaker, earbuds and a parked car using Bluetooth media audio. Test permission denial and revocation, background and screen lock, rotation, output disconnect, notification Stop and restart. Log device models, Android versions and actual behavior.
4. If the account is a personal account created after 13 November 2023, use **Closed testing** with at least 12 testers who stay opted in continuously for at least 14 days. Ask testers to exercise the app, collect feedback, and address problems. When eligible, select **Apply for production** on the Dashboard and describe the test and the changes it led to. Internal testers alone do not satisfy this requirement. [Closed-testing and production access requirements](https://support.google.com/googleplay/android-developer/answer/14151465)
5. Review Play Console errors, warnings and declarations before promoting the tested bundle or uploading a newer signed bundle. Keep the production release in draft until the owner is ready to launch. Confirm the selected countries and the production rollout action. A first production release reaches all eligible users in the selected countries and does not offer a staged percentage. [Release review and rollout](https://support.google.com/googleplay/android-developer/answer/9859348), [Publishing overview](https://support.google.com/googleplay/android-developer/answer/9859654)
