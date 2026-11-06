package com.niru.pocketpa;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

/** Foreground service holding the device's microphone while the screen is locked. */
public final class MicService extends Service {
    public static final String ACTION_START = "com.niru.pocketpa.START";
    public static final String ACTION_UPDATE = "com.niru.pocketpa.UPDATE";
    public static final String ACTION_STOP = "com.niru.pocketpa.STOP";
    public static final String EXTRA_MIC = "mic";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_EFFECT = "effect";
    public static final String EXTRA_GATE = "gate";
    public static final String EXTRA_SENSITIVITY = "sensitivity";

    public static volatile boolean running = false;
    public static volatile String lastError = null;
    private static volatile float inputGain = 1.0f;
    private static volatile float outputGain = 1.0f;
    private static volatile String effect = AudioProcessor.CLEAN;
    private static volatile boolean gate = false;
    private static volatile int sensitivity = 55;

    private static final int RATE = 48000;
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL = "pocketpa_microphone";
    private Thread audioThread;
    private volatile boolean active;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Live microphone", NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            active = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action) || ACTION_UPDATE.equals(action)) {
            updateConfiguration(intent);
            if (ACTION_START.equals(action) && !active) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    lastError = "Microphone permission is required.";
                    stopSelf();
                    return START_NOT_STICKY;
                }
                try {
                    startForeground(NOTIFICATION_ID, buildNotification());
                    active = true;
                    running = true;
                    lastError = null;
                    audioThread = new Thread(this::streamMic, "PocketPA-Audio");
                    audioThread.start();
                } catch (Exception e) {
                    lastError = "Unable to start microphone: " + e.getMessage();
                    Log.e("PocketPA", "Starting foreground microphone failed", e);
                    active = false;
                    running = false;
                    stopSelf();
                }
            }
        }
        return START_NOT_STICKY;
    }

    private static void updateConfiguration(Intent intent) {
        inputGain = Math.max(0f, Math.min(2f, intent.getFloatExtra(EXTRA_MIC, inputGain)));
        outputGain = Math.max(0f, Math.min(1.5f, intent.getFloatExtra(EXTRA_OUTPUT, outputGain)));
        String newEffect = intent.getStringExtra(EXTRA_EFFECT);
        if (newEffect != null) effect = newEffect;
        gate = intent.getBooleanExtra(EXTRA_GATE, gate);
        sensitivity = Math.max(0, Math.min(100, intent.getIntExtra(EXTRA_SENSITIVITY, sensitivity)));
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAction = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, MicService.class).setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(com.niru.pocketpa.R.drawable.ic_mic)
                .setContentTitle("Pocket PA · microphone live")
                .setContentText("Your voice is being sent to the media speaker")
                .setContentIntent(openAction)
                .addAction(android.R.drawable.ic_media_pause, "Stop microphone", stopAction)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void streamMic() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        AudioRecord record = null;
        AudioTrack speaker = null;
        try {
            int inFormat = AudioFormat.CHANNEL_IN_MONO;
            int outFormat = AudioFormat.CHANNEL_OUT_MONO;
            int encoding = AudioFormat.ENCODING_PCM_16BIT;
            int minIn = AudioRecord.getMinBufferSize(RATE, inFormat, encoding);
            int minOut = AudioTrack.getMinBufferSize(RATE, outFormat, encoding);
            if (minIn <= 0 || minOut <= 0) throw new IllegalStateException("PCM 48 kHz is unavailable");

            AudioFormat recordFormat = new AudioFormat.Builder()
                    .setEncoding(encoding).setSampleRate(RATE).setChannelMask(inFormat).build();
            record = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(recordFormat)
                    .setBufferSizeInBytes(Math.max(minIn * 2, 4096))
                    .build();
            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("Microphone could not initialize");
            }

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            speaker = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(encoding)
                            .setSampleRate(RATE).setChannelMask(outFormat).build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(Math.max(minOut * 2, 4096))
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build();
            if (speaker.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new IllegalStateException("Speaker audio could not initialize");
            }

            // Prefer a paired A2DP/BLE media output. Android otherwise routes USAGE_MEDIA.
            AudioDeviceInfo preferred = findBluetoothMediaOutput();
            if (preferred != null) {
                try { speaker.setPreferredDevice(preferred); }
                catch (SecurityException ignored) { /* Media route remains system default. */ }
            }

            AudioProcessor processor = new AudioProcessor(RATE);
            short[] samples = new short[480]; // 10ms PCM chunks; codec buffering still applies.
            record.startRecording();
            speaker.play();
            while (active && !Thread.currentThread().isInterrupted()) {
                int count = record.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                if (count == AudioRecord.ERROR_DEAD_OBJECT) {
                    throw new IllegalStateException("Microphone was disconnected");
                }
                if (count < 0) throw new IllegalStateException("Mic error: " + count);
                if (count == 0) continue;
                processor.process(samples, count, inputGain, outputGain, effect, gate, sensitivity);
                int written = 0;
                while (active && written < count) {
                    int result = speaker.write(samples, written, count - written, AudioTrack.WRITE_BLOCKING);
                    if (result < 0) throw new IllegalStateException("Speaker error: " + result);
                    if (result == 0) break;
                    written += result;
                }
            }
        } catch (Exception e) {
            if (active) {
                lastError = "Audio stopped: " + e.getMessage();
                Log.e("PocketPA", "Mic streaming stopped unexpectedly", e);
            }
        } finally {
            active = false;
            running = false;
            if (record != null) {
                try { if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) record.stop(); }
                catch (Exception ignored) { }
                record.release();
            }
            if (speaker != null) {
                try { if (speaker.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) speaker.stop(); }
                catch (Exception ignored) { }
                speaker.release();
            }
            stopSelf();
        }
    }

    /** Return the live media output or null if Android should choose default. */
    public AudioDeviceInfo findBluetoothMediaOutput() {
        try {
            AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (manager == null) return null;
            for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                type == AudioDeviceInfo.TYPE_BLE_SPEAKER))) {
                    return device;
                }
            }
        } catch (SecurityException ignored) { }
        return null;
    }

    @Override public void onDestroy() {
        active = false;
        running = false;
        if (audioThread != null) audioThread.interrupt();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
