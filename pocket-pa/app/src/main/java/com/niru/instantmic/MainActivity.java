package com.niru.instantmic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Offline and accessible Android UI. Deliberately no ads, analytics or trackers. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(25, 27, 32);
    private static final int PANEL = Color.rgb(34, 37, 43);
    private static final int PANEL_LIGHT = Color.rgb(49, 52, 60);
    private static final int ORANGE = Color.rgb(255, 140, 66);
    private static final int WHITE = Color.rgb(248, 243, 236);
    private static final int MUTED = Color.rgb(194, 189, 184);
    private static final int RED = Color.rgb(255, 142, 142);
    private static final int BORDER = Color.rgb(68, 67, 72);
    private static final List<String> FEATURED_PRESETS = Arrays.asList(
            AudioProcessor.CLEAN, AudioProcessor.MAN, AudioProcessor.ROBOT, AudioProcessor.HELIUM);
    private static final String[] ICONS = {"◉", "↓", "↑", "♬", "☠", "◈", "↓", "↑", "▦", "✦", "★", "〰", "↝", "◎", "◈", "☎", "⚡", "◆"};
    private static final String[] DESCRIPTIONS = {
            "Natural voice", "Low male-style", "Higher female-style", "Little kid", "Scary growl", "Spooky floating",
            "Deep bass", "Funny high pitch", "Metallic", "Sci-fi wobble", "Cartoon squeak", "Breathy",
            "Repeating tail", "Big reverb", "Vintage sound", "Narrow-band", "Loud & gritty", "Overdrive"
    };
    private static final int REQUEST_AUDIO = 100;
    private static final int REQUEST_NOTIFICATIONS = 102;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PresetCard> presetCards = new ArrayList<>();
    private final List<View> meterBars = new ArrayList<>();

    private record PresetCard(String effect, LinearLayout view, TextView title) { }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 120);
        }
    };

    private SharedPreferences prefs;
    private AudioManager audioManager;
    private List<AudioDeviceInfo> bluetoothOutputs = new ArrayList<>();
    private AudioDeviceInfo preferredOutput;
    private int outputSelection = MediaOutput.AUTOMATIC;
    private boolean resumed;
    private boolean startAfterPermission;
    private final AudioDeviceCallback outputCallback = new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added) { refreshOutputs(); }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) { refreshOutputs(); }
    };

    private TextView speakerName;
    private Button chooseOutputButton;
    private Button notificationButton;
    private TextView statusLabel;
    private TextView levelLabel;
    private TextView activePreset;
    private Button micButton;
    private int micPercent;
    private int outputPercent;
    private int sensitivity;
    private String selectedEffect;
    private boolean presetsExpanded;
    private boolean noiseGate;
    private boolean reduceNoise;
    private boolean reduceEcho;
    private MicService.State lastState;
    private int lastPeak = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("instantmic_prefs", MODE_PRIVATE);
        audioManager = getSystemService(AudioManager.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (state != null) {
            outputSelection = state.getInt("output_device", MediaOutput.AUTOMATIC);
            startAfterPermission = state.getBoolean("start_after_permission");
            presetsExpanded = state.getBoolean("presets_expanded");
        }
        micPercent = prefs.getInt("mic", 100);
        outputPercent = prefs.getInt("out", 45);
        sensitivity = prefs.getInt("sensitivity", 55);
        selectedEffect = prefs.getString("effect", AudioProcessor.CLEAN);
        noiseGate = prefs.getBoolean("gate", true);
        reduceNoise = prefs.getBoolean("reduce_noise", true);
        reduceEcho = prefs.getBoolean("reduce_echo", true);
        micPercent = Math.max(0, Math.min(200, micPercent));
        outputPercent = Math.max(0, Math.min(150, outputPercent));
        sensitivity = Math.max(0, Math.min(100, sensitivity));
        buildInterface();
        refreshOutputs();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("output_device", outputSelection);
        state.putBoolean("start_after_permission", startAfterPermission);
        state.putBoolean("presets_expanded", presetsExpanded);
    }

    @SuppressWarnings("deprecation") // Android 15+ enforces edge-to-edge; opt in on Android 11–14.
    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setId(R.id.home_scroll);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(27), dp(20), dp(34));
        scroll.addView(page);
        if (Build.VERSION.SDK_INT >= 30) {
            if (Build.VERSION.SDK_INT < 35) getWindow().setDecorFitsSystemWindows(false);
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        setContentView(scroll);

        LinearLayout header = horizontal(Gravity.CENTER_VERTICAL);
        ImageView symbol = new ImageView(this);
        symbol.setImageResource(R.drawable.ic_mic);
        symbol.setImageTintList(ColorStateList.valueOf(BG));
        symbol.setPadding(dp(10), dp(10), dp(10), dp(10));
        symbol.setBackground(round(ORANGE, 14, null));
        symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(symbol, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout identity = vertical();
        identity.addView(label(getString(R.string.app_name), 23, WHITE, Typeface.BOLD));
        identity.addView(label(getString(R.string.tagline), 13, MUTED, Typeface.NORMAL));
        LinearLayout.LayoutParams identityLp = new LinearLayout.LayoutParams(0, -2, 1);
        identityLp.setMarginStart(dp(13));
        header.addView(identity, identityLp);
        page.addView(header);
        gap(page, 22);

        LinearLayout hero = card(page);
        TextView title = label("Your phone is the mic", 27, WHITE, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        hero.addView(title);
        gap(hero, 8);
        TextView subtitle = label("Play your voice through a compatible Bluetooth speaker, car stereo, or headphones.", 14, MUTED, Typeface.NORMAL);
        subtitle.setLineSpacing(dp(2), 1f);
        hero.addView(subtitle);
        gap(hero, 12);

        LinearLayout meter = horizontal(Gravity.BOTTOM);
        meter.setGravity(Gravity.CENTER);
        meter.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        for (int i = 0; i < 21; i++) {
            View bar = new View(this);
            bar.setBackground(round(PANEL_LIGHT, 4, null));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(7), 1f);
            lp.leftMargin = dp(2);
            lp.rightMargin = dp(2);
            meter.addView(bar, lp);
            meterBars.add(bar);
        }
        hero.addView(meter, new LinearLayout.LayoutParams(-1, dp(48)));
        gap(hero, 8);
        LinearLayout liveStrip = horizontal(Gravity.CENTER_VERTICAL);
        statusLabel = label(getString(R.string.status_ready), 13, WHITE, Typeface.BOLD);
        statusLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        liveStrip.addView(statusLabel, new LinearLayout.LayoutParams(0, -2, 1));
        levelLabel = label(getString(R.string.mic_off), 11, MUTED, Typeface.BOLD);
        levelLabel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        liveStrip.addView(levelLabel);
        hero.addView(liveStrip);
        gap(hero, 14);
        micButton = button(getString(R.string.start_microphone), true);
        micButton.setTextSize(16);
        micButton.setMinHeight(dp(56));
        micButton.setOnClickListener(v -> toggleMicrophone());
        hero.addView(micButton, new LinearLayout.LayoutParams(-1, -2));
        gap(hero, 9);
        hero.addView(label("The mic can stay live when you leave this screen. Tap Stop to end the session. Audio is never saved.",
                12, MUTED, Typeface.NORMAL));
        gap(hero, 7);
        hero.addView(label("Bluetooth adds delay. Start at low volume and keep the speaker away from the phone.",
                12, MUTED, Typeface.NORMAL));
        gap(page, 14);

        LinearLayout outputCard = card(page);
        TextView outputHeading = label(getString(R.string.output_heading), 16, WHITE, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= 28) outputHeading.setAccessibilityHeading(true);
        outputCard.addView(outputHeading);
        gap(outputCard, 5);
        speakerName = label(getString(R.string.output_system), 14, ORANGE, Typeface.NORMAL);
        outputCard.addView(speakerName);
        gap(outputCard, 12);
        chooseOutputButton = button(getString(R.string.choose_output), false);
        chooseOutputButton.setOnClickListener(v -> chooseOutput());
        outputCard.addView(chooseOutputButton, new LinearLayout.LayoutParams(-1, -2));
        gap(outputCard, 6);
        Button connect = button(getString(R.string.connect_bluetooth), false);
        connect.setOnClickListener(v -> openSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        outputCard.addView(connect, new LinearLayout.LayoutParams(-1, -2));
        notificationButton = button(getString(R.string.enable_notification), false);
        notificationButton.setOnClickListener(v -> enableNotifications());
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(-1, -2);
        notificationParams.topMargin = dp(6);
        outputCard.addView(notificationButton, notificationParams);
        gap(page, 24);

        section(page, "VOICE EFFECTS", "Change how you sound", "18 optional presets • switch while live");
        gap(page, 13);
        activePreset = label("", 13, ORANGE, Typeface.BOLD);
        page.addView(activePreset);
        gap(page, 10);
        addPresetRows(page, FEATURED_PRESETS);
        Button more = button(getString(presetsExpanded ? R.string.presets_show_less : R.string.presets_show_more), false);
        page.addView(more, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout extraPresets = vertical();
        gap(extraPresets, 11);
        List<String> remaining = new ArrayList<>(Arrays.asList(AudioProcessor.EFFECTS));
        remaining.removeAll(FEATURED_PRESETS);
        addPresetRows(extraPresets, remaining);
        extraPresets.setVisibility(presetsExpanded ? View.VISIBLE : View.GONE);
        page.addView(extraPresets);
        more.setOnClickListener(v -> {
            presetsExpanded = !presetsExpanded;
            more.setText(presetsExpanded ? R.string.presets_show_less : R.string.presets_show_more);
            extraPresets.setVisibility(presetsExpanded ? View.VISIBLE : View.GONE);
        });
        updateSelectedPreset();
        gap(page, 24);

        section(page, "MIXER", "Fine-tune your sound", "Make adjustments while live");
        gap(page, 15);
        LinearLayout mixer = card(page);
        TextView micPercentLabel = label(getString(R.string.percent, micPercent), 14, ORANGE, Typeface.BOLD);
        addSlider(mixer, "MIC INPUT", "Voice capture level", micPercentLabel, micPercent, 200, value -> {
            micPercent = value;
            micPercentLabel.setText(getString(R.string.percent, value));
            saveSettings();
        });
        gap(mixer, 23);
        TextView outputPercentLabel = label(getString(R.string.percent, outputPercent), 14, ORANGE, Typeface.BOLD);
        addSlider(mixer, "OUTPUT GAIN", "Adjust the app’s playback level", outputPercentLabel, outputPercent, 150, value -> {
            outputPercent = value;
            outputPercentLabel.setText(getString(R.string.percent, value));
            saveSettings();
        });
        gap(page, 24);

        section(page, "ECHO & NOISE", "Reduce unwanted sound", getString(R.string.capture_cleanup_subtitle));
        gap(page, 15);
        LinearLayout feedback = card(page);
        addAudioSwitch(feedback, R.string.noise_gate_title, R.string.noise_gate_detail,
                true, noiseGate, (compound, checked) -> {
            noiseGate = checked;
            saveSettings();
        });
        gap(feedback, 18);
        addAudioSwitch(feedback, R.string.noise_reduction_title, R.string.noise_reduction_detail,
                NoiseSuppressor.isAvailable(), reduceNoise, (compound, checked) -> {
            reduceNoise = checked;
            saveSettings();
        });
        gap(feedback, 18);
        addAudioSwitch(feedback, R.string.echo_reduction_title, R.string.echo_reduction_detail,
                AcousticEchoCanceler.isAvailable(), reduceEcho, (compound, checked) -> {
            reduceEcho = checked;
            saveSettings();
        });
        gap(feedback, 12);
        feedback.addView(label("Echo cancellation depends on the phone and audio route. Bluetooth delay and physical feedback can still occur.", 12, MUTED, Typeface.NORMAL));
        gap(feedback, 20);
        TextView gateValue = label(getString(R.string.percent, sensitivity), 14, ORANGE, Typeface.BOLD);
        addSlider(feedback, "GATE SENSITIVITY", "Higher catches softer voices", gateValue, sensitivity, 100, value -> {
            sensitivity = value;
            gateValue.setText(getString(R.string.percent, value));
            saveSettings();
        });
        gap(page, 22);

        LinearLayout tipCard = vertical();
        tipCard.setPadding(dp(17), dp(17), dp(17), dp(17));
        tipCard.setBackground(round(PANEL, 18, BORDER));
        tipCard.addView(label("✧  SOUND CHECK TIP", 12, ORANGE, Typeface.BOLD));
        gap(tipCard, 8);
        TextView hint = label("Point the speaker away from the phone and raise volume slowly. If you hear squealing, lower the volume or use headphones. Choose Clean to hear your natural voice.", 13, MUTED, Typeface.NORMAL);
        hint.setLineSpacing(dp(3), 1f);
        tipCard.addView(hint);
        page.addView(tipCard);
        gap(page, 22);

        Button privacy = button(getString(R.string.privacy_title), false);
        privacy.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.privacy_title)
                .setMessage(R.string.privacy_policy)
                .setPositiveButton(android.R.string.ok, null).show());
        page.addView(privacy, new LinearLayout.LayoutParams(-1, -2));
    }

    private void chooseOutput() {
        refreshOutputs();
        List<AudioDeviceInfo> choices = bluetoothOutputs;
        if (choices.isEmpty()) {
            showNoBluetoothDialog();
            return;
        }
        CharSequence[] names = new CharSequence[choices.size() + 1];
        names[0] = getString(R.string.output_auto);
        int checked = outputSelection == MediaOutput.AUTOMATIC ? 0 : -1;
        for (int i = 0; i < choices.size(); i++) {
            AudioDeviceInfo device = choices.get(i);
            names[i + 1] = MediaOutput.label(this, device);
            if (device.getId() == outputSelection) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_output)
                .setSingleChoiceItems(names, checked, (dialog, item) -> {
                    outputSelection = item == 0 ? MediaOutput.AUTOMATIC : choices.get(item - 1).getId();
                    MicService.error = 0;
                    refreshOutputs();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void refreshOutputs() {
        bluetoothOutputs = MediaOutput.bluetoothDevices(audioManager);
        preferredOutput = MediaOutput.preferred(audioManager, bluetoothOutputs, outputSelection);
        refreshStatus();
    }

    private void toggleMicrophone() {
        if (MicService.state == MicService.State.LIVE || MicService.state == MicService.State.STARTING) {
            MicService.stop();
            refreshStatus();
        } else if (MicService.state == MicService.State.IDLE) {
            refreshOutputs();
            if (preferredOutput == null) {
                showNoBluetoothDialog();
                return;
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            } else {
                startAfterPermission = true;
                startWhenVisible();
            }
        }
    }

    private void startWhenVisible() {
        if (!resumed || !startAfterPermission || isFinishing()) return;
        startAfterPermission = false;
        refreshOutputs();
        if (preferredOutput == null) {
            showNoBluetoothDialog();
            return;
        }
        int selected = preferredOutput.getId();
        updateServiceSettings();
        MicService.start(this, selected);
        refreshStatus();
    }

    private void showNoBluetoothDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_bluetooth_dialog_title)
                .setMessage(R.string.no_bluetooth_dialog_message)
                .setPositiveButton(R.string.connect_bluetooth, (dialog, button) ->
                        openSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openSettings(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void showPermissionSettings(int title, int message, Intent settings) {
        new AlertDialog.Builder(this)
                .setTitle(title).setMessage(message)
                .setPositiveButton(R.string.open_settings, (dialog, button) -> openSettings(settings))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else {
            showNotificationSettings();
        }
    }

    private void showNotificationSettings() {
        showPermissionSettings(R.string.notification_permission_title, R.string.notification_permission_message,
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (grants.length == 0) return;
        if (code == REQUEST_AUDIO) {
            if (grants[0] == PackageManager.PERMISSION_GRANTED) {
                startAfterPermission = true;
                startWhenVisible();
            } else {
                MicService.error = R.string.error_permission;
                refreshStatus();
                if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    showPermissionSettings(R.string.error_permission, R.string.mic_permission_message,
                            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", getPackageName(), null)));
                }
            }
        } else if (code == REQUEST_NOTIFICATIONS) {
            refreshNotificationButton();
            if (grants[0] != PackageManager.PERMISSION_GRANTED &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showNotificationSettings();
            }
        }
    }

    private void updateServiceSettings() {
        MicService.updateSettings(new MicService.Settings(micPercent / 100f, outputPercent / 100f,
                selectedEffect, noiseGate, sensitivity, reduceEcho, reduceNoise));
    }

    private void saveSettings() {
        prefs.edit().putInt("mic", micPercent).putInt("out", outputPercent)
                .putString("effect", selectedEffect).putBoolean("gate", noiseGate)
                .putBoolean("reduce_echo", reduceEcho)
                .putBoolean("reduce_noise", reduceNoise)
                .putInt("sensitivity", sensitivity).apply();
        updateServiceSettings();
    }

    private void refreshStatus() {
        MicService.State state = MicService.state;
        boolean live = state == MicService.State.LIVE;
        if (state != lastState) {
            boolean active = state != MicService.State.IDLE;
            micButton.setText(active ? R.string.stop_microphone : R.string.start_microphone);
            micButton.setBackground(buttonBackground(active ? RED : ORANGE, true));
            micButton.setEnabled(state != MicService.State.STOPPING);
            chooseOutputButton.setEnabled(state == MicService.State.IDLE);
            lastState = state;
        }
        int status = switch (state) {
            case STARTING -> R.string.status_starting;
            case STOPPING -> R.string.status_stopping;
            case LIVE -> R.string.status_live;
            case IDLE -> {
                if (MicService.error != 0) {
                    yield MicService.error;
                } else if (preferredOutput == null) {
                    yield R.string.status_no_bluetooth;
                } else {
                    yield R.string.status_ready;
                }
            }
        };
        updateText(statusLabel, getString(status));
        statusLabel.setTextColor(live ? ORANGE : WHITE);

        float level = live ? MicService.audioLevel : 0f;
        int peak = Math.round(level * meterBars.size());
        if (peak != lastPeak) {
            for (int i = 0; i < meterBars.size(); i++) {
                View bar = meterBars.get(i);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bar.getLayoutParams();
                boolean lit = i < peak;
                lp.height = dp(lit ? 12 + (i * 17 / meterBars.size()) : 7);
                bar.setLayoutParams(lp);
                bar.setBackground(round(lit ? ORANGE : PANEL_LIGHT, 4, null));
            }
            lastPeak = peak;
        }
        updateText(levelLabel, live ? getString(R.string.input_level, Math.round(level * 100))
                : getString(R.string.mic_off));

        String output;
        if (live && MicService.activeOutput != null) {
            output = getString(R.string.output_active, MicService.activeOutput);
        } else if (preferredOutput != null) {
            String name = MediaOutput.label(this, preferredOutput);
            output = outputSelection == MediaOutput.AUTOMATIC
                    ? getString(R.string.output_auto_device, name) : name;
        } else {
            output = getString(R.string.output_no_bluetooth);
        }
        updateText(speakerName, output);
    }

    private void refreshNotificationButton() {
        notificationButton.setVisibility(MicService.stopNotificationEnabled(this) ? View.GONE : View.VISIBLE);
    }

    private static void updateText(TextView view, String value) {
        if (!value.contentEquals(view.getText())) view.setText(value);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        audioManager.registerAudioDeviceCallback(outputCallback, handler);
        refreshOutputs();
        refreshNotificationButton();
        startWhenVisible();
        handler.post(ticker);
    }

    @Override protected void onPause() {
        resumed = false;
        audioManager.unregisterAudioDeviceCallback(outputCallback);
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private void addPresetRows(LinearLayout parent, List<String> effects) {
        List<String> allEffects = Arrays.asList(AudioProcessor.EFFECTS);
        for (int i = 0; i < effects.size(); i += 2) {
            LinearLayout pair = horizontal(Gravity.TOP);
            for (int j = i; j < Math.min(i + 2, effects.size()); j++) {
                String effect = effects.get(j);
                int index = allEffects.indexOf(effect);
                String title = presetName(effect);
                String description = DESCRIPTIONS[index];
                LinearLayout option = vertical();
                option.setPadding(dp(14), dp(15), dp(12), dp(14));
                TextView glyph = label(ICONS[index], 23, ORANGE, Typeface.BOLD);
                option.addView(glyph);
                gap(option, 9);
                TextView effectTitle = label(title, 15, WHITE, Typeface.BOLD);
                option.addView(effectTitle);
                gap(option, 3);
                TextView effectDescription = label(description, 11, MUTED, Typeface.NORMAL);
                option.addView(effectDescription);
                option.setContentDescription(getString(R.string.preset_description, title, description));
                option.setFocusable(true);
                option.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                effectTitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                effectDescription.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                option.setOnClickListener(v -> {
                    selectedEffect = effect;
                    updateSelectedPreset();
                    saveSettings();
                });
                presetCards.add(new PresetCard(effect, option, effectTitle));
                option.setMinimumHeight(dp(110));
                LinearLayout.LayoutParams optionLp = new LinearLayout.LayoutParams(0, -1, 1f);
                if (j % 2 == 1) optionLp.setMarginStart(dp(11));
                pair.addView(option, optionLp);
            }
            parent.addView(pair);
            gap(parent, 11);
        }
    }

    private String presetName(String effect) {
        return AudioProcessor.MAN.equals(effect) ? getString(R.string.preset_men) : effect;
    }

    private void updateSelectedPreset() {
        for (PresetCard card : presetCards) {
            boolean chosen = card.effect().equals(selectedEffect);
            card.view().setBackground(round(chosen ? Color.rgb(62, 43, 34) : PANEL,
                    17, chosen ? ORANGE : BORDER));
            card.title().setTextColor(chosen ? ORANGE : WHITE);
            card.view().setSelected(chosen);
        }
        activePreset.setText(getString(R.string.current_preset, presetName(selectedEffect)));
    }

    private void addAudioSwitch(LinearLayout parent, int title, int detail, boolean available,
                                boolean enabled, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = horizontal(Gravity.CENTER_VERTICAL);
        LinearLayout copy = vertical();
        String name = getString(title);
        copy.addView(label(name, 16, WHITE, Typeface.BOLD));
        gap(copy, 3);
        copy.addView(label(getString(available ? detail : R.string.capture_effect_unavailable),
                12, MUTED, Typeface.NORMAL));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        Switch control = new Switch(this);
        control.setEnabled(available);
        control.setChecked(available && enabled);
        control.setContentDescription(name);
        control.setMinWidth(dp(48));
        control.setMinHeight(dp(48));
        control.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ORANGE, MUTED}));
        control.setOnCheckedChangeListener(listener);
        row.addView(control);
        parent.addView(row);
    }

    private interface SliderHandler { void update(int value); }
    private void addSlider(LinearLayout parent, String title, String subtitle, TextView value,
                           int initial, int max, SliderHandler changed) {
        LinearLayout row = horizontal(Gravity.CENTER_VERTICAL);
        LinearLayout copy = vertical();
        copy.addView(label(title, 12, WHITE, Typeface.BOLD));
        gap(copy, 3);
        copy.addView(label(subtitle, 12, MUTED, Typeface.NORMAL));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(value);
        parent.addView(row);
        gap(parent, 8);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max);
        seek.setProgress(initial);
        seek.setContentDescription(title);
        seek.setProgressTintList(ColorStateList.valueOf(ORANGE));
        seek.setThumbTintList(ColorStateList.valueOf(ORANGE));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(PANEL_LIGHT));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) changed.update(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        parent.addView(seek, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void section(LinearLayout page, String heading, String title, String sub) {
        TextView overline = label(heading, 11, ORANGE, Typeface.BOLD);
        overline.setLetterSpacing(0.17f);
        page.addView(overline);
        gap(page, 5);
        TextView headingView = label(title, 21, WHITE, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= 28) headingView.setAccessibilityHeading(true);
        page.addView(headingView);
        gap(page, 4);
        page.addView(label(sub, 13, MUTED, Typeface.NORMAL));
    }
    private LinearLayout card(LinearLayout page) {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(18), dp(18), dp(19));
        card.setBackground(round(PANEL, 20, BORDER));
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }
    private Button button(String title, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(title);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? BG : ORANGE);
        button.setBackground(buttonBackground(primary ? ORANGE : PANEL_LIGHT, primary));
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setStateListAnimator(null);
        button.setElevation(0);
        return button;
    }
    private RippleDrawable buttonBackground(int color, boolean primary) {
        int ripple = primary ? 0x33000000 : 0x33FFFFFF;
        return new RippleDrawable(ColorStateList.valueOf(ripple), round(color, 14, null), null);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    private LinearLayout horizontal(int gravity) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(gravity);
        return layout;
    }
    private TextView label(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        return view;
    }
    private GradientDrawable round(int color, float radius, Integer stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != null) d.setStroke(dp(1), stroke);
        return d;
    }
    private void gap(LinearLayout column, int height) {
        column.addView(new View(this), new LinearLayout.LayoutParams(dp(1), dp(height)));
    }
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
