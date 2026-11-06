package com.niru.pocketpa;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Offline and accessible Android UI. Deliberately no ads, analytics or trackers. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 14, 22);
    private static final int PANEL = Color.rgb(23, 30, 43);
    private static final int PANEL_LIGHT = Color.rgb(35, 45, 61);
    private static final int LIME = Color.rgb(199, 248, 114);
    private static final int PURPLE = Color.rgb(177, 159, 255);
    private static final int WHITE = Color.rgb(247, 250, 253);
    private static final int MUTED = Color.rgb(155, 171, 191);
    private static final int RED = Color.rgb(255, 124, 132);
    private static final String[] ICONS = {"◉", "↓", "↑", "♬", "☠", "◈", "↓", "↑", "▦", "✦", "★", "〰", "↝", "◎", "◈", "☎", "⚡", "◆"};
    private static final String[] DESCRIPTIONS = {
            "Natural voice", "Low male-style", "Higher female-style", "Little kid", "Scary growl", "Spooky floating",
            "Deep bass", "Funny high pitch", "Metallic", "Sci-fi wobble", "Cartoon squeak", "Breathy",
            "Repeating tail", "Big reverb", "Vintage sound", "Narrow-band", "Loud & gritty", "Overdrive"
    };
    private static final int REQUEST_AUDIO = 100;
    private static final int REQUEST_BLUETOOTH = 101;
    private static final int REQUEST_NOTIFICATIONS = 102;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<LinearLayout> presetCards = new ArrayList<>();
    private final List<TextView> presetTitles = new ArrayList<>();
    private final List<View> meterBars = new ArrayList<>();

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 120);
        }
    };

    private SharedPreferences prefs;
    private TextView speakerName;
    private TextView statusLabel;
    private TextView levelLabel;
    private TextView activePreset;
    private Button micButton;
    private int micPercent;
    private int outputPercent;
    private int sensitivity;
    private String selectedEffect;
    private boolean noiseGate;
    private boolean reduceEcho;
    private boolean lastRunning = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("pocketpa_prefs", MODE_PRIVATE);
        micPercent = prefs.getInt("mic", 100);
        outputPercent = prefs.getInt("out", 100);
        sensitivity = prefs.getInt("sensitivity", 55);
        selectedEffect = prefs.getString("effect", AudioProcessor.CLEAN);
        noiseGate = prefs.getBoolean("gate", false);
        reduceEcho = prefs.getBoolean("reduce_echo", true);
        micPercent = Math.max(0, Math.min(200, micPercent));
        outputPercent = Math.max(0, Math.min(150, outputPercent));
        sensitivity = Math.max(0, Math.min(100, sensitivity));
        buildInterface();
        refreshStatus();
    }

    private void buildInterface() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(27), dp(20), dp(34));
        scroll.addView(page);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                page.setPadding(dp(20), dp(26) + bars.top, dp(20), dp(34) + bars.bottom);
                return insets;
            });
        }
        setContentView(scroll);

        LinearLayout header = horizontal(Gravity.CENTER_VERTICAL);
        TextView symbol = label("◉", 29, BG, Typeface.BOLD);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(round(LIME, 17, null));
        header.addView(symbol, new LinearLayout.LayoutParams(dp(51), dp(51)));
        LinearLayout identity = vertical();
        identity.addView(label("Instant Mic", 23, WHITE, Typeface.BOLD));
        TextView caption = label("LIVE AUDIO • VOICE STUDIO", 10, MUTED, Typeface.BOLD);
        caption.setLetterSpacing(0.14f);
        identity.addView(caption);
        LinearLayout.LayoutParams identityLp = new LinearLayout.LayoutParams(0, -2, 1);
        identityLp.leftMargin = dp(13);
        header.addView(identity, identityLp);
        TextView version = label("V2.2", 10, LIME, Typeface.BOLD);
        version.setGravity(Gravity.CENTER);
        version.setBackground(round(PANEL_LIGHT, 11, null));
        header.addView(version, new LinearLayout.LayoutParams(dp(47), dp(30)));
        page.addView(header);
        gap(page, 26);

        LinearLayout hero = vertical();
        hero.setPadding(dp(21), dp(22), dp(21), dp(22));
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(49, 48, 84), Color.rgb(26, 43, 69), Color.rgb(24, 32, 45)});
        gradient.setCornerRadius(dp(25));
        gradient.setStroke(dp(1), Color.rgb(68, 70, 102));
        hero.setBackground(gradient);
        TextView eyebrow = label("YOUR STAGE, ANYWHERE", 11, LIME, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.15f);
        hero.addView(eyebrow);
        gap(hero, 12);
        TextView title = label("Turn your phone\ninto a microphone.", 29, WHITE, Typeface.BOLD);
        title.setLineSpacing(dp(3), 1f);
        hero.addView(title);
        gap(hero, 9);
        TextView subtitle = label("Speak or sing through your connected Bluetooth speaker. No internet needed.", 14, MUTED, Typeface.NORMAL);
        subtitle.setLineSpacing(dp(3), 1f);
        hero.addView(subtitle);
        gap(hero, 19);

        LinearLayout meter = horizontal(Gravity.BOTTOM);
        meter.setGravity(Gravity.CENTER);
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
        statusLabel = label("READY TO GO LIVE", 11, WHITE, Typeface.BOLD);
        statusLabel.setLetterSpacing(0.07f);
        liveStrip.addView(statusLabel, new LinearLayout.LayoutParams(0, -2, 1));
        levelLabel = label("MIC OFF", 11, MUTED, Typeface.BOLD);
        liveStrip.addView(levelLabel);
        hero.addView(liveStrip);
        gap(hero, 17);
        micButton = button("●  START MICROPHONE", true);
        micButton.setTextSize(17);
        micButton.setContentDescription("Start microphone broadcast");
        micButton.setOnClickListener(v -> toggleMicrophone());
        hero.addView(micButton, new LinearLayout.LayoutParams(-1, dp(66)));
        page.addView(hero);
        gap(page, 18);

        LinearLayout outputCard = card(page);
        LinearLayout outputRow = horizontal(Gravity.CENTER_VERTICAL);
        TextView deviceIcon = label("◖))", 23, PURPLE, Typeface.BOLD);
        deviceIcon.setGravity(Gravity.CENTER);
        deviceIcon.setBackground(round(PANEL_LIGHT, 13, null));
        outputRow.addView(deviceIcon, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout info = vertical();
        info.addView(label("CONNECTED AUDIO OUTPUT", 10, MUTED, Typeface.BOLD));
        gap(info, 5);
        speakerName = label("Checking output…", 15, WHITE, Typeface.BOLD);
        info.addView(speakerName);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1);
        infoLp.leftMargin = dp(13);
        outputRow.addView(info, infoLp);
        outputCard.addView(outputRow);
        gap(outputCard, 14);
        Button connect = button("Bluetooth settings  ↗", false);
        connect.setContentDescription("Open Bluetooth settings to connect a speaker");
        connect.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { speakerName.setText("Open Bluetooth in Android Settings"); }
        });
        outputCard.addView(connect, new LinearLayout.LayoutParams(-1, dp(46)));
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            gap(outputCard, 7);
            TextView permission = label("Allow Bluetooth permission to show the speaker name", 12, PURPLE, Typeface.BOLD);
            permission.setPadding(0, dp(6), 0, dp(6));
            permission.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH));
            outputCard.addView(permission);
        }
        gap(page, 27);

        section(page, "VOICE STUDIO", "Choose your live voice", "18 voice styles • switch instantly while speaking");
        gap(page, 15);
        String[] effects = AudioProcessor.EFFECTS;
        for (int i = 0; i < effects.length; i += 2) {
            LinearLayout pair = horizontal(Gravity.TOP);
            for (int j = i; j < Math.min(i + 2, effects.length); j++) {
                final String chosen = effects[j];
                LinearLayout option = vertical();
                option.setPadding(dp(14), dp(15), dp(12), dp(14));
                TextView glyph = label(ICONS[j], 23, PURPLE, Typeface.BOLD);
                option.addView(glyph);
                gap(option, 9);
                TextView effectTitle = label(chosen, 15, WHITE, Typeface.BOLD);
                effectTitle.setSingleLine(true);
                option.addView(effectTitle);
                gap(option, 3);
                option.addView(label(DESCRIPTIONS[j], 11, MUTED, Typeface.NORMAL));
                option.setContentDescription(chosen + " effect. " + DESCRIPTIONS[j]);
                option.setFocusable(true);
                option.setClickable(true);
                option.setOnClickListener(v -> {
                    selectedEffect = chosen;
                    updateSelectedPreset();
                    saveSettings();
                });
                presetCards.add(option);
                presetTitles.add(effectTitle);
                LinearLayout.LayoutParams optionLp = new LinearLayout.LayoutParams(0, dp(114), 1f);
                if (j % 2 == 1) optionLp.leftMargin = dp(11);
                pair.addView(option, optionLp);
            }
            page.addView(pair);
            gap(page, 11);
        }
        activePreset = label("Current preset: " + selectedEffect, 12, LIME, Typeface.BOLD);
        activePreset.setGravity(Gravity.CENTER);
        page.addView(activePreset);
        updateSelectedPreset();
        gap(page, 24);

        section(page, "MIXER", "Fine-tune your sound", "Make adjustments while live");
        gap(page, 15);
        LinearLayout mixer = card(page);
        TextView micPercentLabel = label(micPercent + "%", 14, LIME, Typeface.BOLD);
        addSlider(mixer, "MIC INPUT", "Voice capture level", micPercentLabel, micPercent, 200, value -> {
            micPercent = value;
            micPercentLabel.setText(value + "%");
            saveSettings();
        });
        gap(mixer, 23);
        TextView outputPercentLabel = label(outputPercent + "%", 14, LIME, Typeface.BOLD);
        addSlider(mixer, "SPEAKER OUTPUT", "Output boost and volume", outputPercentLabel, outputPercent, 150, value -> {
            outputPercent = value;
            outputPercentLabel.setText(value + "%");
            saveSettings();
        });
        gap(page, 24);

        section(page, "FEEDBACK CONTROL", "Less echo, clearer speech", "Noise gate and device echo cancellation");
        gap(page, 15);
        LinearLayout feedback = card(page);
        LinearLayout switchRow = horizontal(Gravity.CENTER_VERTICAL);
        LinearLayout switchCopy = vertical();
        switchCopy.addView(label("Noise gate", 16, WHITE, Typeface.BOLD));
        gap(switchCopy, 3);
        switchCopy.addView(label("Suppress quiet background noise", 12, MUTED, Typeface.NORMAL));
        switchRow.addView(switchCopy, new LinearLayout.LayoutParams(0, -2, 1));
        Switch gateSwitch = new Switch(this);
        gateSwitch.setChecked(noiseGate);
        gateSwitch.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{LIME, MUTED}));
        switchRow.addView(gateSwitch);
        feedback.addView(switchRow);
        gateSwitch.setOnCheckedChangeListener((compound, checked) -> {
            noiseGate = checked;
            saveSettings();
        });
        gap(feedback, 18);
        LinearLayout echoRow = horizontal(Gravity.CENTER_VERTICAL);
        LinearLayout echoCopy = vertical();
        echoCopy.addView(label("Reduce speaker echo", 16, WHITE, Typeface.BOLD));
        gap(echoCopy, 3);
        boolean supportsAec = android.media.audiofx.AcousticEchoCanceler.isAvailable();
        echoCopy.addView(label(supportsAec
                        ? "Android echo canceller (device-dependent)"
                        : "Not available on this phone", 12, MUTED, Typeface.NORMAL));
        echoRow.addView(echoCopy, new LinearLayout.LayoutParams(0, -2, 1));
        Switch echoSwitch = new Switch(this);
        echoSwitch.setEnabled(supportsAec);
        echoSwitch.setChecked(supportsAec && reduceEcho);
        echoSwitch.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{LIME, MUTED}));
        echoRow.addView(echoSwitch);
        feedback.addView(echoRow);
        echoSwitch.setOnCheckedChangeListener((compound, checked) -> {
            reduceEcho = checked;
            saveSettings();
        });
        gap(feedback, 12);
        feedback.addView(label("Echo reduction works best after stopping and restarting the mic. Bluetooth delay can defeat cancellation.", 12, MUTED, Typeface.NORMAL));
        gap(feedback, 20);
        TextView gateValue = label(sensitivity + "%", 14, LIME, Typeface.BOLD);
        addSlider(feedback, "GATE SENSITIVITY", "Higher catches softer voices", gateValue, sensitivity, 100, value -> {
            sensitivity = value;
            gateValue.setText(value + "%");
            saveSettings();
        });
        gap(page, 22);

        LinearLayout tipCard = vertical();
        tipCard.setPadding(dp(17), dp(17), dp(17), dp(17));
        tipCard.setBackground(round(Color.rgb(33, 34, 45), 18, Color.rgb(74, 68, 89)));
        tipCard.addView(label("✧  SOUND CHECK TIP", 12, PURPLE, Typeface.BOLD));
        gap(tipCard, 8);
        TextView hint = label("Keep the speaker 2+ metres away and pointed away from the phone. Start with speaker output at 40–60% and mic gain at 80–100%. Select Clean, Man, Woman or Child instead of Echo/Cave. Bluetooth delay varies by device.", 13, MUTED, Typeface.NORMAL);
        hint.setLineSpacing(dp(3), 1f);
        tipCard.addView(hint);
        page.addView(tipCard);
        gap(page, 22);

        TextView privacy = label("Privacy & microphone usage  •  100% offline", 12, MUTED, Typeface.NORMAL);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(dp(8), dp(10), dp(8), dp(10));
        privacy.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Your privacy matters")
                .setMessage("Instant Mic accesses your microphone only after you press Start. Audio is processed on your phone and streamed to your chosen output. It is not saved, collected, shared, or transmitted to a server. A foreground notification lets you stop playback at any time. Bluetooth permission is only used to show available device names. You can stop the mic or revoke permissions in Android Settings.")
                .setPositiveButton("Got it", null).show());
        page.addView(privacy);
    }

    private void toggleMicrophone() {
        if (MicService.running) {
            startService(new Intent(this, MicService.class).setAction(MicService.ACTION_STOP));
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            return;
        } else {
            try {
                startForegroundService(settingsIntent(MicService.ACTION_START));
                if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                        !prefs.getBoolean("notification_asked", false)) {
                    prefs.edit().putBoolean("notification_asked", true).apply();
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
                }
            } catch (Exception e) {
                statusLabel.setText("Couldn't start: " + e.getClass().getSimpleName());
            }
        }
        handler.postDelayed(this::refreshStatus, 240);
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == REQUEST_AUDIO) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) toggleMicrophone();
            else statusLabel.setText("Microphone permission required");
        } else if (code == REQUEST_BLUETOOTH) refreshStatus();
    }

    private Intent settingsIntent(String action) {
        return new Intent(this, MicService.class).setAction(action)
                .putExtra(MicService.EXTRA_MIC, micPercent / 100f)
                .putExtra(MicService.EXTRA_OUTPUT, outputPercent / 100f)
                .putExtra(MicService.EXTRA_EFFECT, selectedEffect)
                .putExtra(MicService.EXTRA_GATE, noiseGate)
                .putExtra(MicService.EXTRA_ECHO_REDUCTION, reduceEcho)
                .putExtra(MicService.EXTRA_SENSITIVITY, sensitivity);
    }

    private void saveSettings() {
        prefs.edit().putInt("mic", micPercent).putInt("out", outputPercent)
                .putString("effect", selectedEffect).putBoolean("gate", noiseGate)
                .putBoolean("reduce_echo", reduceEcho)
                .putInt("sensitivity", sensitivity).apply();
        if (MicService.running) startService(settingsIntent(MicService.ACTION_UPDATE));
    }

    private void refreshStatus() {
        if (micButton == null || speakerName == null || statusLabel == null) return;
        boolean running = MicService.running;
        if (running != lastRunning) {
            micButton.setText(running ? "■  STOP MICROPHONE" : "●  START MICROPHONE");
            micButton.setContentDescription(running ? "Stop microphone broadcast" : "Start microphone broadcast");
            micButton.setBackground(round(running ? RED : LIME, 17, null));
            lastRunning = running;
        }
        if (MicService.lastError != null && !running) statusLabel.setText(MicService.lastError);
        else statusLabel.setText(running ? "●  YOU'RE LIVE" : "READY TO GO LIVE");
        statusLabel.setTextColor(running ? LIME : WHITE);
        float level = running ? MicService.audioLevel : 0f;
        int peak = Math.round(level * meterBars.size());
        for (int i = 0; i < meterBars.size(); i++) {
            View bar = meterBars.get(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bar.getLayoutParams();
            boolean lit = running && (i < peak);
            int height = lit ? 12 + (i * 17 / meterBars.size()) : 7;
            lp.height = dp(height);
            bar.setLayoutParams(lp);
            bar.setBackground(round(lit ? (i > 15 ? PURPLE : LIME) : PANEL_LIGHT, 4, null));
        }
        levelLabel.setText(running ? Math.round(level * 100) + "% INPUT" : "MIC OFF");
        AudioDeviceInfo bt = findBluetoothDevice();
        if (bt != null) {
            String name = "Bluetooth speaker detected";
            try {
                CharSequence product = bt.getProductName();
                if (product != null && product.length() > 0) name = product.toString();
            } catch (SecurityException ignored) { }
            speakerName.setText("●  " + name);
            speakerName.setTextColor(LIME);
        } else {
            speakerName.setText("Phone / selected media output");
            speakerName.setTextColor(WHITE);
        }
    }

    private AudioDeviceInfo findBluetoothDevice() {
        try {
            AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (manager == null) return null;
            for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                type == AudioDeviceInfo.TYPE_BLE_SPEAKER))) return device;
            }
        } catch (SecurityException ignored) { }
        return null;
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    private void updateSelectedPreset() {
        for (int i = 0; i < presetCards.size(); i++) {
            boolean chosen = AudioProcessor.EFFECTS[i].equals(selectedEffect);
            presetCards.get(i).setBackground(round(chosen ? Color.rgb(53, 73, 63) : PANEL,
                    17, chosen ? LIME : Color.rgb(40, 50, 66)));
            presetTitles.get(i).setTextColor(chosen ? LIME : WHITE);
            presetCards.get(i).setSelected(chosen);
        }
        if (activePreset != null) activePreset.setText("✦  Current preset: " + selectedEffect);
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
        seek.setProgressTintList(ColorStateList.valueOf(LIME));
        seek.setThumbTintList(ColorStateList.valueOf(LIME));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(PANEL_LIGHT));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) changed.update(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        parent.addView(seek, new LinearLayout.LayoutParams(-1, dp(36)));
    }

    private void section(LinearLayout page, String heading, String title, String sub) {
        TextView overline = label(heading, 11, LIME, Typeface.BOLD);
        overline.setLetterSpacing(0.17f);
        page.addView(overline);
        gap(page, 5);
        page.addView(label(title, 21, WHITE, Typeface.BOLD));
        gap(page, 4);
        page.addView(label(sub, 13, MUTED, Typeface.NORMAL));
    }
    private LinearLayout card(LinearLayout page) {
        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(18), dp(18), dp(19));
        card.setBackground(round(PANEL, 20, Color.rgb(43, 52, 68)));
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }
    private Button button(String title, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(title);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? BG : LIME);
        button.setBackground(round(primary ? LIME : PANEL_LIGHT, 16, null));
        button.setElevation(0);
        return button;
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
