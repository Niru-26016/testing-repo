package com.example.earsplit.stereotest;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHUNK_FRAMES = 1024;
    private static final int BACKGROUND = 0xff0a1426;
    private static final int PANEL = 0xff15243b;
    private static final int WHITE = 0xfff6f8ff;
    private static final int MUTED = 0xffa7b6d2;
    private static final int ACCENT = 0xff81b6ff;

    private AudioManager audioManager;
    private AudioDeviceCallback deviceCallback;
    private TextView connectionText, statusText, leftValue, rightValue;
    private LinearLayout testPanel;
    private SeekBar leftVolume, rightVolume;
    private StereoPlayer player;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        buildScreen();

        deviceCallback = new AudioDeviceCallback() {
            @Override public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                runOnUiThread(() -> refreshConnection());
            }
            @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                runOnUiThread(() -> refreshConnection());
            }
        };
        audioManager.registerAudioDeviceCallback(deviceCallback, new Handler(Looper.getMainLooper()));
        refreshConnection();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(30), dp(20), dp(25));
        scroll.addView(root);
        setContentView(scroll);

        TextView appName = label("EarSplit", 32, WHITE, true);
        root.addView(appName);
        TextView subtitle = label("STEREO EARPHONE TEST · v0.2", 12, ACCENT, true);
        subtitle.setLetterSpacing(0.12f);
        root.addView(subtitle, withTop(0, dp(4)));

        LinearLayout connectionCard = card();
        root.addView(connectionCard, withTop(-1, dp(25)));
        connectionCard.addView(label("OUTPUT CONNECTION", 12, MUTED, true));
        connectionText = label("Checking earphones...", 17, WHITE, true);
        connectionCard.addView(connectionText, withTop(-1, dp(9)));
        connectionCard.addView(label("Compatible with wired headphones and Bluetooth earbuds.", 13, MUTED, false), withTop(-1, dp(7)));

        testPanel = new LinearLayout(this);
        testPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(testPanel, withTop(-1, dp(16)));

        LinearLayout channelCard = card();
        testPanel.addView(channelCard);
        channelCard.addView(label("TWO DIFFERENT TONES", 13, ACCENT, true));
        channelCard.addView(label("LEFT  •  440 Hz", 20, WHITE, true), withTop(-1, dp(12)));
        channelCard.addView(label("RIGHT  •  660 Hz", 20, WHITE, true), withTop(-1, dp(5)));
        channelCard.addView(label("Play both together to check whether each earbud receives only its assigned tone.", 14, MUTED, false), withTop(-1, dp(10)));

        TextView volTitle = label("Left volume", 14, WHITE, true);
        channelCard.addView(volTitle, withTop(-1, dp(19)));
        leftValue = label("55%", 12, MUTED, false);
        channelCard.addView(leftValue);
        leftVolume = new SeekBar(this);
        leftVolume.setMax(100);
        leftVolume.setProgress(55);
        channelCard.addView(leftVolume);

        channelCard.addView(label("Right volume", 14, WHITE, true), withTop(-1, dp(10)));
        rightValue = label("55%", 12, MUTED, false);
        channelCard.addView(rightValue);
        rightVolume = new SeekBar(this);
        rightVolume.setMax(100);
        rightVolume.setProgress(55);
        channelCard.addView(rightVolume);
        leftVolume.setOnSeekBarChangeListener(volumeListener(leftValue));
        rightVolume.setOnSeekBarChangeListener(volumeListener(rightValue));

        testPanel.addView(button("▶  Play BOTH (different tones)", () -> startTest(Mode.BOTH), true), withTop(-1, dp(18)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        testPanel.addView(row, withTop(-1, dp(10)));
        Button leftButton = button("◀  Left only", () -> startTest(Mode.LEFT), false);
        Button rightButton = button("Right only  ▶", () -> startTest(Mode.RIGHT), false);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(54), 1f);
        half.setMargins(0, 0, dp(5), 0);
        row.addView(leftButton, half);
        LinearLayout.LayoutParams halfRight = new LinearLayout.LayoutParams(0, dp(54), 1f);
        halfRight.setMargins(dp(5), 0, 0, 0);
        row.addView(rightButton, halfRight);
        testPanel.addView(button("■  Stop sound", () -> stopTest("Stopped"), false), withTop(-1, dp(10)));

        LinearLayout info = card();
        root.addView(info, withTop(-1, dp(16)));
        info.addView(label("TEST STATUS", 12, MUTED, true));
        statusText = label("Connect your earbuds to begin.", 14, WHITE, false);
        info.addView(statusText, withTop(-1, dp(7)));
        info.addView(label("No microphone, screen sharing, recording, internet, Shizuku, or root access.", 12, MUTED, false), withTop(-1, dp(12)));
        info.addView(label("This test generates audio inside EarSplit; it does not separate Spotify or Instagram yet.", 12, MUTED, false), withTop(-1, dp(6)));
    }

    private SeekBar.OnSeekBarChangeListener volumeListener(TextView text) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text.setText(progress + "%");
                if (player != null) player.setVolumes(leftVolume.getProgress() / 100f, rightVolume.getProgress() / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private void refreshConnection() {
        AudioDeviceInfo output = connectedEarphone();
        if (output == null) {
            connectionText.setText("Not connected — connect earphones");
            connectionText.setTextColor(0xffffbf9c);
            testPanel.setVisibility(View.GONE);
            stopTest("Connect your earbuds or wired headphones to start.");
        } else {
            String name = output.getProductName() == null ? "Earphones" : output.getProductName().toString();
            connectionText.setText("● Connected: " + name);
            connectionText.setTextColor(0xff9de8b7);
            testPanel.setVisibility(View.VISIBLE);
            if (player == null) statusText.setText("Ready. Start at low phone volume.");
        }
    }

    private AudioDeviceInfo connectedEarphone() {
        for (AudioDeviceInfo d : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isEarphone(d.getType())) return d;
        }
        return null;
    }

    private static boolean isEarphone(int type) {
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE) return true;
        return Build.VERSION.SDK_INT >= 31 &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER);
    }

    private void startTest(Mode mode) {
        AudioDeviceInfo device = connectedEarphone();
        if (device == null) {
            refreshConnection();
            return;
        }
        stopPlayer();
        StereoPlayer next = new StereoPlayer(mode, device);
        player = next;
        statusText.setText(mode == Mode.LEFT ? "Playing 440 Hz in LEFT only." :
            mode == Mode.RIGHT ? "Playing 660 Hz in RIGHT only." :
            "Playing LEFT 440 Hz and RIGHT 660 Hz simultaneously.");
        next.setVolumes(leftVolume.getProgress() / 100f, rightVolume.getProgress() / 100f);
        next.start();
    }

    private void stopPlayer() {
        StereoPlayer previous = player;
        player = null;
        if (previous != null) previous.stop();
    }

    private void stopTest(String message) {
        stopPlayer();
        if (statusText != null) statusText.setText(message);
    }

    private void playbackFailed(String message, StereoPlayer originatingPlayer) {
        runOnUiThread(() -> {
            if (player == originatingPlayer) {
                stopTest(message);
            }
        });
    }

    @Override protected void onStop() {
        super.onStop();
        stopTest("Stopped when app went to background.");
    }

    @Override protected void onDestroy() {
        stopPlayer();
        if (audioManager != null && deviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(deviceCallback);
        }
        super.onDestroy();
    }

    private enum Mode { LEFT, RIGHT, BOTH }

    private class StereoPlayer implements Runnable {
        private final Mode mode;
        private final AudioDeviceInfo output;
        private volatile boolean running = false;
        private volatile float leftGain = 0.55f, rightGain = 0.55f;
        private volatile AudioTrack track;
        StereoPlayer(Mode mode, AudioDeviceInfo output) { this.mode = mode; this.output = output; }
        void setVolumes(float left, float right) {
            leftGain = Math.max(0f, Math.min(1f, left));
            rightGain = Math.max(0f, Math.min(1f, right));
        }
        void start() {
            running = true;
            new Thread(this, "EarSplitStereoAudio").start();
        }
        void stop() {
            running = false;
            AudioTrack t = track;
            if (t != null) {
                try { t.pause(); t.flush(); } catch (IllegalStateException ignored) {}
            }
        }
        @Override public void run() {
            AudioTrack local = null;
            try {
                int minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                if (minBytes < 0) throw new IllegalStateException("No stereo PCM output");
                local = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setBufferSizeInBytes(Math.max(minBytes, CHUNK_FRAMES * 4 * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
                if (local.getState() != AudioTrack.STATE_INITIALIZED) {
                    throw new IllegalStateException("Audio output unavailable");
                }
                track = local;
                local.setPreferredDevice(output);
                if (!running) return;
                local.play();

                short[] samples = new short[CHUNK_FRAMES * 2];
                double leftPhase = 0, rightPhase = 0;
                final double leftStep = 2d * Math.PI * 440d / SAMPLE_RATE;
                final double rightStep = 2d * Math.PI * 660d / SAMPLE_RATE;
                long frameIndex = 0;
                int blocks = 0;
                while (running) {
                    if (++blocks % 8 == 0) {
                        AudioDeviceInfo routed = local.getRoutedDevice();
                        if (routed != null && !isEarphone(routed.getType())) {
                            playbackFailed("Stopped: Android routed audio away from earphones.", this);
                            break;
                        }
                    }
                    for (int i = 0; i < CHUNK_FRAMES; i++) {
                        double fadeIn = Math.min(1d, frameIndex / 4800d);
                        float left = (mode == Mode.RIGHT ? 0f : leftGain);
                        float right = (mode == Mode.LEFT ? 0f : rightGain);
                        samples[i * 2] = (short) (Math.sin(leftPhase) * 8500d * fadeIn * left);
                        samples[i * 2 + 1] = (short) (Math.sin(rightPhase) * 8500d * fadeIn * right);
                        leftPhase += leftStep;
                        rightPhase += rightStep;
                        if (leftPhase >= 2d * Math.PI) leftPhase -= 2d * Math.PI;
                        if (rightPhase >= 2d * Math.PI) rightPhase -= 2d * Math.PI;
                        frameIndex++;
                    }
                    int position = 0;
                    while (running && position < samples.length) {
                        int written = local.write(samples, position, samples.length - position,
                            AudioTrack.WRITE_BLOCKING);
                        if (written <= 0) throw new IllegalStateException("Playback write failed: " + written);
                        position += written;
                    }
                }
            } catch (Exception ex) {
                if (running) playbackFailed("Audio test could not start: " + ex.getMessage(), this);
            } finally {
                running = false;
                if (local != null) {
                    try { if (local.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) local.stop(); }
                    catch (IllegalStateException ignored) {}
                    local.release();
                }
                track = null;
            }
        }
    }

    private LinearLayout card() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(dp(17), dp(17), dp(17), dp(17));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL);
        bg.setCornerRadius(dp(18));
        result.setBackground(bg);
        return result;
    }
    private Button button(String text, Runnable action, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? BACKGROUND : WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? ACCENT : 0xff243753);
        bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        b.setOnClickListener(v -> action.run());
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(54));
        return b;
    }
    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private LinearLayout.LayoutParams withTop(int width, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            width < 0 ? LinearLayout.LayoutParams.MATCH_PARENT : width,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = top;
        return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}