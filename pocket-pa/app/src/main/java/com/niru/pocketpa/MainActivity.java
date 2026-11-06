package com.niru.pocketpa;

import android.Manifest;
import android.app.Activity;
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
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Native app UI; no network permission or cloud service required. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 17, 25);
    private static final int SURFACE = Color.rgb(22, 32, 44);
    private static final int SURFACE_2 = Color.rgb(31, 43, 57);
    private static final int ACCENT = Color.rgb(101, 228, 207);
    private static final int TEXT = Color.rgb(242, 247, 250);
    private static final int MUTED = Color.rgb(159, 178, 190);
    private static final String[] EFFECTS = {"Clean", "Echo", "Megaphone", "Telephone", "Robot", "Distortion"};
    private static final int REQUEST_AUDIO = 100;
    private static final int REQUEST_BLUETOOTH = 101;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 1000);
        }
    };

    private SharedPreferences prefs;
    private TextView speakerLabel;
    private TextView statusLabel;
    private Button micButton;
    private TextView micValue;
    private TextView outValue;
    private TextView gateHint;
    private final java.util.ArrayList<TextView> effectChips = new java.util.ArrayList<>();
    private int micPercent;
    private int outputPercent;
    private int sensitivity;
    private String selectedEffect;
    private boolean noiseGate;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("pocketpa_prefs", MODE_PRIVATE);
        micPercent = prefs.getInt("mic", 100);
        outputPercent = prefs.getInt("out", 100);
        sensitivity = prefs.getInt("sensitivity", 55);
        selectedEffect = prefs.getString("effect", AudioProcessor.CLEAN);
        noiseGate = prefs.getBoolean("gate", false);
        buildInterface();
        refreshStatus();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(26), dp(22), dp(34));
        scroll.addView(page);
        setContentView(scroll);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView brandIcon = text("◉", 25, ACCENT, Typeface.BOLD);
        brandIcon.setGravity(Gravity.CENTER);
        brandIcon.setBackground(shape(SURFACE, 15, null));
        brand.addView(brandIcon, new LinearLayout.LayoutParams(dp(47), dp(47)));
        TextView brandName = text("POCKET PA", 18, TEXT, Typeface.BOLD);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(-2, -2);
        brandParams.leftMargin = dp(12);
        brand.addView(brandName, brandParams);
        page.addView(brand);
        space(page, 25);

        TextView eyebrow = text("GO LIVE IN SECONDS", 12, ACCENT, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.13f);
        page.addView(eyebrow);
        space(page, 10);
        TextView headline = text("Your phone.\nA wireless mic.", 32, TEXT, Typeface.BOLD);
        headline.setLineSpacing(dp(2), 1f);
        page.addView(headline);
        space(page, 9);
        page.addView(text("Speak into your phone and hear your voice through your connected Bluetooth speaker.", 15, MUTED, Typeface.NORMAL));
        space(page, 24);

        LinearLayout deviceCard = card(page);
        LinearLayout deviceRow = new LinearLayout(this);
        deviceRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout deviceText = new LinearLayout(this);
        deviceText.setOrientation(LinearLayout.VERTICAL);
        deviceText.addView(text("AUDIO OUTPUT", 11, MUTED, Typeface.BOLD));
        space(deviceText, 6);
        speakerLabel = text("Checking media output…", 16, TEXT, Typeface.BOLD);
        deviceText.addView(speakerLabel);
        deviceRow.addView(deviceText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView speakerIcon = text("◖))", 20, ACCENT, Typeface.BOLD);
        deviceRow.addView(speakerIcon);
        deviceCard.addView(deviceRow);
        space(deviceCard, 13);
        Button bluetoothButton = button("OPEN BLUETOOTH SETTINGS", false);
        bluetoothButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { statusLabel.setText("Open Android Settings → Bluetooth manually."); }
        });
        deviceCard.addView(bluetoothButton, new LinearLayout.LayoutParams(-1, dp(43)));
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            space(deviceCard, 6);
            TextView askBt = text("Allow speaker name detection", 12, ACCENT, Typeface.BOLD);
            askBt.setPadding(0, dp(8), 0, dp(8));
            askBt.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH));
            deviceCard.addView(askBt);
        }
        space(page, 17);

        LinearLayout liveCard = card(page);
        TextView micHeading = text("MICROPHONE", 11, MUTED, Typeface.BOLD);
        micHeading.setGravity(Gravity.CENTER);
        liveCard.addView(micHeading);
        space(liveCard, 13);
        micButton = button("START MICROPHONE", true);
        micButton.setTextSize(17);
        micButton.setOnClickListener(v -> toggleMicrophone());
        liveCard.addView(micButton, new LinearLayout.LayoutParams(-1, dp(78)));
        space(liveCard, 13);
        statusLabel = text("Connect a speaker, then tap Start.", 13, MUTED, Typeface.NORMAL);
        statusLabel.setGravity(Gravity.CENTER);
        liveCard.addView(statusLabel);
        space(page, 26);

        sectionTitle(page, "LEVELS", "Adjust the balance");
        space(page, 13);
        LinearLayout levelsCard = card(page);
        micValue = text("100%", 14, ACCENT, Typeface.BOLD);
        outValue = text("100%", 14, ACCENT, Typeface.BOLD);
        addSlider(levelsCard, "Mic sensitivity / input gain", micValue, micPercent, 200,
                value -> { micPercent = value; micValue.setText(value + "%"); commit(); });
        space(levelsCard, 19);
        addSlider(levelsCard, "Speaker output gain", outValue, outputPercent, 150,
                value -> { outputPercent = value; outValue.setText(value + "%"); commit(); });
        space(page, 26);

        sectionTitle(page, "VOICE EFFECTS", "Add some character");
        space(page, 14);
        HorizontalScrollView effectsScroll = new HorizontalScrollView(this);
        effectsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (String effectName : EFFECTS) {
            TextView chip = text(effectName, 14, TEXT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(17), dp(13), dp(17), dp(13));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.rightMargin = dp(8);
            chips.addView(chip, cp);
            effectChips.add(chip);
            chip.setOnClickListener(v -> {
                selectedEffect = effectName;
                updateChipColors();
                commit();
            });
        }
        effectsScroll.addView(chips);
        page.addView(effectsScroll);
        updateChipColors();
        space(page, 25);

        sectionTitle(page, "FEEDBACK CONTROL", "Reduce noise between words");
        space(page, 14);
        LinearLayout feedbackCard = card(page);
        LinearLayout gateRow = new LinearLayout(this);
        gateRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView gateLabel = text("Noise gate", 16, TEXT, Typeface.BOLD);
        gateRow.addView(gateLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Switch gateSwitch = new Switch(this);
        gateSwitch.setChecked(noiseGate);
        gateSwitch.setThumbTintList(new ColorStateList(new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}}, new int[] {ACCENT, MUTED}));
        gateRow.addView(gateSwitch);
        feedbackCard.addView(gateRow);
        gateHint = text("Off · Most natural speech. Turn on if background noise causes feedback.", 13, MUTED, Typeface.NORMAL);
        feedbackCard.addView(gateHint);
        gateSwitch.setOnCheckedChangeListener((button, checked) -> {
            noiseGate = checked;
            gateHint.setText(checked ? "On · Quiet sounds may be clipped. Adjust the threshold below." :
                    "Off · Most natural speech. Turn on if background noise causes feedback.");
            commit();
        });
        space(feedbackCard, 16);
        TextView sensVal = text(sensitivity + "%", 14, ACCENT, Typeface.BOLD);
        addSlider(feedbackCard, "Gate sensitivity", sensVal, sensitivity, 100,
                value -> { sensitivity = value; sensVal.setText(value + "%"); commit(); });
        space(page, 22);

        TextView note = text("TIP  Keep the phone away from the speaker to prevent squealing. Bluetooth introduces delay; wired speakers are usually faster.",
                13, MUTED, Typeface.NORMAL);
        note.setLineSpacing(dp(4), 1f);
        note.setPadding(dp(3), dp(3), dp(3), dp(3));
        page.addView(note);
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
            } catch (Exception e) {
                statusLabel.setText("Could not start: " + e.getMessage());
            }
        }
        handler.postDelayed(this::refreshStatus, 200);
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == REQUEST_AUDIO) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) toggleMicrophone();
            else statusLabel.setText("Microphone permission is needed to speak.");
        }
        if (code == REQUEST_BLUETOOTH) refreshStatus();
    }

    private void commit() {
        prefs.edit().putInt("mic", micPercent).putInt("out", outputPercent)
                .putString("effect", selectedEffect).putBoolean("gate", noiseGate)
                .putInt("sensitivity", sensitivity).apply();
        if (MicService.running) startService(settingsIntent(MicService.ACTION_UPDATE));
    }

    private Intent settingsIntent(String action) {
        return new Intent(this, MicService.class).setAction(action)
                .putExtra(MicService.EXTRA_MIC, micPercent / 100f)
                .putExtra(MicService.EXTRA_OUTPUT, outputPercent / 100f)
                .putExtra(MicService.EXTRA_EFFECT, selectedEffect)
                .putExtra(MicService.EXTRA_GATE, noiseGate)
                .putExtra(MicService.EXTRA_SENSITIVITY, sensitivity);
    }

    private void refreshStatus() {
        if (micButton == null || speakerLabel == null || statusLabel == null) return;
        boolean on = MicService.running;
        micButton.setText(on ? "■  STOP MICROPHONE" : "●  START MICROPHONE");
        micButton.setBackground(shape(on ? Color.rgb(242, 118, 118) : ACCENT, 18, null));
        micButton.setTextColor(BG);
        if (MicService.lastError != null && !on) statusLabel.setText(MicService.lastError);
        else statusLabel.setText(on ? "LIVE  ·  Your microphone is active" : "Ready to speak · Start when connected");
        AudioDeviceInfo bt = findBluetoothDevice();
        if (bt != null) {
            String name = "Bluetooth speaker connected";
            try {
                CharSequence product = bt.getProductName();
                if (product != null && product.length() > 0) name = product.toString();
            } catch (SecurityException ignored) { }
            speakerLabel.setText("●  " + name);
            speakerLabel.setTextColor(ACCENT);
        } else {
            speakerLabel.setText("Phone / selected media output");
            speakerLabel.setTextColor(TEXT);
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

    private interface SliderHandler { void accept(int value); }

    private void addSlider(LinearLayout parent, String title, TextView valueView,
                           int initial, int max, SliderHandler changed) {
        LinearLayout label = new LinearLayout(this);
        label.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = text(title, 14, TEXT, Typeface.NORMAL);
        label.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1));
        valueView.setText(initial + "%");
        label.addView(valueView);
        parent.addView(label);
        space(parent, 7);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max);
        seek.setProgress(initial);
        seek.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seek.setThumbTintList(ColorStateList.valueOf(ACCENT));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(SURFACE_2));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) changed.accept(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        parent.addView(seek, new LinearLayout.LayoutParams(-1, dp(30)));
    }

    private void updateChipColors() {
        for (int i = 0; i < effectChips.size(); i++) {
            TextView chip = effectChips.get(i);
            boolean active = EFFECTS[i].equals(selectedEffect);
            chip.setTextColor(active ? BG : TEXT);
            chip.setBackground(shape(active ? ACCENT : SURFACE_2, 18, null));
        }
    }

    private void sectionTitle(LinearLayout page, String heading, String subtitle) {
        page.addView(text(heading, 12, ACCENT, Typeface.BOLD));
        space(page, 5);
        page.addView(text(subtitle, 20, TEXT, Typeface.BOLD));
    }

    private LinearLayout card(LinearLayout page) {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(dp(18), dp(19), dp(18), dp(19));
        result.setBackground(shape(SURFACE, 21, null));
        page.addView(result, new LinearLayout.LayoutParams(-1, -2));
        return result;
    }

    private Button button(String label, boolean filled) {
        Button result = new Button(this);
        result.setAllCaps(false);
        result.setText(label);
        result.setTextSize(13);
        result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        result.setTextColor(filled ? BG : ACCENT);
        result.setBackground(shape(filled ? ACCENT : SURFACE_2, filled ? 18 : 12, null));
        result.setElevation(0);
        return result;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(sp);
        result.setTextColor(color);
        result.setTypeface(Typeface.create("sans-serif", style));
        return result;
    }

    private GradientDrawable shape(int fill, int radius, Integer border) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(fill);
        result.setCornerRadius(dp(radius));
        if (border != null) result.setStroke(dp(1), border);
        return result;
    }

    private void space(LinearLayout parent, int dpHeight) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(dpHeight)));
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
