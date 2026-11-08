package com.niru.instantmic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import java.util.List;
import java.util.function.IntFunction;

/** A user-started microphone session. Only its worker creates, uses, or releases audio objects. */
public final class MicService extends Service {
    private static final String ACTION_START = "com.niru.instantmic.START";
    private static final String ACTION_STOP = "com.niru.instantmic.STOP";
    private static final String EXTRA_DEVICE = "device";
    private static final String CHANNEL = "instantmic_microphone";
    private static final String TAG = "InstantMic";
    private static final int NOTIFICATION_ID = 101;
    private static final int PCM = AudioFormat.ENCODING_PCM_16BIT;
    private static final int INPUT = AudioFormat.CHANNEL_IN_MONO;
    private static final int OUTPUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int CHUNK_MS = 5;
    private static final int IO_RETRY_MS = 2;

    enum State { IDLE, STARTING, LIVE, STOPPING }
    record Settings(float input, float output, String effect, boolean gate, int sensitivity,
                    boolean echoReduction, boolean noiseReduction) { }

    // The activity and service run in the same process. Only the main thread changes session state.
    static State state = State.IDLE;
    static int error;
    static String activeOutput;
    static volatile float audioLevel;
    private static MicService owner;
    private static volatile Settings settings = new Settings(1f, 1f, AudioProcessor.CLEAN, true, 55, true, true);

    private final Handler main = new Handler(Looper.getMainLooper());
    private Session session;
    private boolean foreground;
    private boolean destroyed;
    private boolean receiverRegistered;
    private final BroadcastReceiver audioSafetyReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (session == null) return;
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (state == State.LIVE) requestStop(session, R.string.error_output_lost);
            } else if (AudioManager.ACTION_MICROPHONE_MUTE_CHANGED.equals(intent.getAction())) {
                requestStop(session, R.string.error_microphone_access_changed);
            }
        }
    };

    static void updateSettings(Settings value) {
        settings = value;
    }

    static boolean stopNotificationEnabled(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
        return manager.areNotificationsEnabled()
                && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }

    static void start(Context context, int output) {
        if (state != State.IDLE) return;
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            error = R.string.error_permission;
            return;
        }
        error = 0;
        activeOutput = null;
        state = State.STARTING;
        try {
            if (context.startForegroundService(new Intent(context, MicService.class)
                    .setAction(ACTION_START).putExtra(EXTRA_DEVICE, output)) == null) {
                state = State.IDLE;
                error = R.string.error_start;
            }
        } catch (IllegalStateException | SecurityException e) {
            Log.w(TAG, "Foreground microphone service could not be started", e);
            state = State.IDLE;
            error = R.string.error_start;
        }
    }

    static void stop() {
        if (state != State.LIVE && state != State.STARTING) return;
        if (owner != null && owner.session != null) {
            owner.requestStop(owner.session, 0);
        } else {
            // A queued foreground start still has to promote before it can stop.
            state = State.STOPPING;
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        owner = this;
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            if (session != null) requestStop(session, 0);
            else if (state == State.STARTING) state = State.STOPPING;
            else if (state == State.IDLE) stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (session != null) return START_NOT_STICKY;
        try {
            Notification notification = buildNotification(getString(R.string.notification_active));
            if (Build.VERSION.SDK_INT >= 30) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            foreground = true;
            if (state == State.STOPPING) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                failStart(R.string.error_permission);
                return START_NOT_STICKY;
            }
            registerAudioSafetyReceiver();
            session = new Session(intent.getIntExtra(EXTRA_DEVICE, MediaOutput.AUTOMATIC));
            session.worker.start();
        } catch (RuntimeException e) {
            Log.w(TAG, "Foreground microphone service could not be promoted", e);
            session = null;
            failStart(e instanceof SecurityException ? R.string.error_permission : R.string.error_start);
        }
        return START_NOT_STICKY;
    }

    private void registerAudioSafetyReceiver() {
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= 28) filter.addAction(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(audioSafetyReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(audioSafetyReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void failStart(int message) {
        error = message;
        state = State.STOPPING;
        stopSelf();
    }

    private void requestStop(Session current, int message) {
        if (session != current || current.stopRequested) return;
        error = message;
        state = State.STOPPING;
        current.cancel();
    }

    private void ready(Session current, String output) {
        if (session != current || current.stopRequested) return;
        activeOutput = output;
        state = State.LIVE;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(NOTIFICATION_ID, buildNotification(getString(R.string.notification_live, output)));
            } catch (SecurityException e) {
                Log.w(TAG, "Stop notification could not be updated", e);
            }
        }
    }

    private void finished(Session current, int failure) {
        if (session != current) return;
        if (!current.stopRequested) error = failure;
        session = null;
        activeOutput = null;
        audioLevel = 0f;
        if (destroyed) {
            state = State.IDLE;
        } else {
            state = State.STOPPING;
            stopSelf();
        }
    }

    private Notification buildNotification(String description) {
        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAction = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, MicService.class).setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(description)
                .setContentIntent(openAction)
                .addAction(new Notification.Action.Builder(null, getString(R.string.stop_microphone), stopAction)
                        .build())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setCategory(Notification.CATEGORY_SERVICE);
        if (Build.VERSION.SDK_INT >= 31) builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return builder.build();
    }

    private final class Session {
        private final Thread worker = new Thread(this::stream, "InstantMic-Audio");
        private final AudioManager audioManager = getSystemService(AudioManager.class);
        private final int outputSelection;
        private volatile boolean stopRequested;
        private volatile float focusVolume = 1f;

        Session(int outputSelection) {
            this.outputSelection = outputSelection;
        }

        void cancel() {
            stopRequested = true;
            worker.interrupt();
        }

        private void onAudioFocusChanged(int change) {
            if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                focusVolume = 0.2f;
            } else if (change < 0) {
                requestStop(this, R.string.error_audio_interrupted);
            } else {
                focusVolume = 1f;
            }
        }

        @SuppressLint("MissingPermission") // Checked before promotion and handled if revoked while starting.
        private void stream() {
            AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(MediaOutput.ATTRIBUTES)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(this::onAudioFocusChanged, main)
                    .build();
            AudioTrack speaker = null;
            AudioRecord record = null;
            CaptureEffect echo = new CaptureEffect("Echo cancellation", AcousticEchoCanceler::create);
            CaptureEffect noise = new CaptureEffect("Noise suppression", NoiseSuppressor::create);
            boolean focusGranted = false;
            int failure = 0;
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                if (audioManager.isMicrophoneMute()) {
                    throw new StreamFailure(R.string.error_microphone_access_changed);
                }
                focusGranted = audioManager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                if (!focusGranted) throw new StreamFailure(R.string.error_audio_busy);

                AudioDeviceInfo preferred = MediaOutput.preferred(audioManager,
                        MediaOutput.bluetoothDevices(audioManager), outputSelection);
                if (preferred == null && outputSelection > 0) throw new StreamFailure(R.string.error_output_lost);

                int rate = chooseSampleRate();
                speaker = new AudioTrack.Builder().setAudioAttributes(MediaOutput.ATTRIBUTES)
                        .setAudioFormat(format(rate, OUTPUT))
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(AudioTrack.getMinBufferSize(rate, OUTPUT, PCM))
                        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                        .build();
                if (speaker.getState() != AudioTrack.STATE_INITIALIZED) {
                    throw new StreamFailure(R.string.error_audio_setup);
                }
                if (preferred != null && !speaker.setPreferredDevice(preferred)) {
                    throw new StreamFailure(R.string.error_output_unavailable);
                }

                // Keep app-side chunks short. Bluetooth may buffer additional audio.
                short[] samples = new short[rate * CHUNK_MS / 1000];
                speaker.play();
                List<AudioDeviceInfo> initialRoute = waitForOutput(speaker, preferred, samples);
                String output = describeRoute(initialRoute);

                Settings initial = settings;
                boolean echoAvailable = AcousticEchoCanceler.isAvailable();
                boolean noiseAvailable = NoiseSuppressor.isAvailable();
                boolean echoEnabled = initial.echoReduction();
                boolean noiseEnabled = initial.noiseReduction();
                boolean useCaptureEffects = (echoAvailable && echoEnabled) || (noiseAvailable && noiseEnabled);
                int captureBytes = Math.max(2 * AudioRecord.getMinBufferSize(rate, INPUT, PCM), samples.length * 4);
                if (stopRequested) return;
                if (!useCaptureEffects && Build.VERSION.SDK_INT >= 29) {
                    record = tryLowLatencyMicrophone(rate, captureBytes);
                }
                boolean alreadyRecording = record != null;
                if (!alreadyRecording) {
                    record = openMicrophone(rate, captureBytes, MediaRecorder.AudioSource.MIC);
                    if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                        throw new StreamFailure(R.string.error_audio_setup);
                    }
                    preferBuiltInMicrophone(record);
                }
                int sessionId = record.getAudioSessionId();
                boolean echoConfigured = echoAvailable && echo.configure(sessionId, echoEnabled);
                boolean noiseConfigured = noiseAvailable && noise.configure(sessionId, noiseEnabled);

                if (stopRequested) return;
                if (!alreadyRecording) record.startRecording();
                if (record.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    throw new StreamFailure(R.string.error_microphone_busy);
                }
                checkCapture(record);
                // Some devices expose capture effects only once the microphone path is active.
                if (echoAvailable && echoEnabled && !echoConfigured) echo.configure(sessionId, true);
                if (noiseAvailable && noiseEnabled && !noiseConfigured) noise.configure(sessionId, true);
                main.post(() -> ready(this, output));

                AudioProcessor processor = new AudioProcessor(rate);
                InputFilter inputFilter = new InputFilter(rate);
                long checkInputAt = 0;
                float appliedVolume = 1f;
                while (!stopRequested) {
                    float volume = focusVolume;
                    if (volume != appliedVolume) {
                        speaker.setVolume(volume);
                        appliedVolume = volume;
                    }
                    Settings current = settings;
                    if (echoEnabled != current.echoReduction()) {
                        echoEnabled = current.echoReduction();
                        if (echoAvailable) echo.configure(sessionId, echoEnabled);
                    }
                    if (noiseEnabled != current.noiseReduction()) {
                        noiseEnabled = current.noiseReduction();
                        if (noiseAvailable) noise.configure(sessionId, noiseEnabled);
                    }
                    long now = SystemClock.elapsedRealtime();
                    if (now >= checkInputAt) {
                        checkCapture(record);
                        checkInputAt = now + 250;
                    }
                    int count = record.read(samples, 0, samples.length, AudioRecord.READ_NON_BLOCKING);
                    if (count < 0) throw new StreamFailure(R.string.error_microphone_busy);
                    if (count == 0) {
                        checkOutput(speaker, initialRoute);
                        Thread.sleep(IO_RETRY_MS);
                        continue;
                    }
                    inputFilter.cleanAndMeasure(samples, count);
                    processor.process(samples, count, current.input(), current.output(),
                            current.effect(), current.gate(), current.sensitivity());
                    // Check before each write, including when Android temporarily stops exposing a route.
                    int written = 0;
                    while (!stopRequested && written < count) {
                        checkOutput(speaker, initialRoute);
                        int result = speaker.write(samples, written, count - written, AudioTrack.WRITE_NON_BLOCKING);
                        if (result < 0) throw new StreamFailure(R.string.error_output_lost);
                        if (result == 0) Thread.sleep(IO_RETRY_MS);
                        written += result;
                    }
                }
            } catch (StreamFailure e) {
                failure = e.message;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (SecurityException e) {
                failure = R.string.error_permission;
                Log.w(TAG, "Microphone access was denied", e);
            } catch (RuntimeException e) {
                failure = R.string.error_audio_setup;
                Log.w(TAG, "Live audio stopped", e);
            } finally {
                release(speaker, record, echo, noise);
                if (focusGranted) {
                    try {
                        audioManager.abandonAudioFocusRequest(focus);
                    } catch (RuntimeException e) {
                        Log.w(TAG, "Could not abandon audio focus", e);
                    }
                }
                int result = failure;
                main.post(() -> finished(this, result));
            }
        }

        private AudioRecord tryLowLatencyMicrophone(int rate, int bytes) {
            AudioRecord candidate = null;
            boolean accepted = false;
            try {
                candidate = openMicrophone(rate, bytes, MediaRecorder.AudioSource.VOICE_PERFORMANCE);
                if (candidate.getState() == AudioRecord.STATE_INITIALIZED) {
                    preferBuiltInMicrophone(candidate);
                    candidate.startRecording();
                    if (candidate.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        checkCapture(candidate);
                        accepted = true;
                        return candidate;
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException | StreamFailure e) {
                Log.w(TAG, "Low-latency microphone input unavailable, using standard input", e);
            } finally {
                if (!accepted) releaseMicrophone(candidate);
            }
            return null;
        }

        private void preferBuiltInMicrophone(AudioRecord record) {
            for (AudioDeviceInfo input : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                if (input.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC && record.setPreferredDevice(input)) return;
            }
        }

        private List<AudioDeviceInfo> waitForOutput(AudioTrack speaker, AudioDeviceInfo preferred, short[] silence)
                throws StreamFailure, InterruptedException {
            long deadline = SystemClock.elapsedRealtime() + 2500;
            while (!stopRequested && SystemClock.elapsedRealtime() < deadline) {
                List<AudioDeviceInfo> routed = MediaOutput.routedDevices(speaker);
                if (!routed.isEmpty() && (preferred == null
                        || (routed.size() == 1 && routed.get(0).getId() == preferred.getId()))) {
                    return routed;
                }
                if (speaker.write(silence, 0, silence.length, AudioTrack.WRITE_NON_BLOCKING) < 0) break;
                Thread.sleep(CHUNK_MS);
            }
            throw new StreamFailure(R.string.error_output_unavailable);
        }
    }

    private static int chooseSampleRate() throws StreamFailure {
        int nativeRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
        int first = nativeRate == 44100 ? 44100 : 48000;
        int second = first == 44100 ? 48000 : 44100;
        for (int rate : new int[]{first, second}) {
            if (AudioRecord.getMinBufferSize(rate, INPUT, PCM) > 0
                    && AudioTrack.getMinBufferSize(rate, OUTPUT, PCM) > 0) return rate;
        }
        throw new StreamFailure(R.string.error_audio_setup);
    }

    private static AudioFormat format(int rate, int channelMask) {
        return new AudioFormat.Builder().setEncoding(PCM).setSampleRate(rate)
                .setChannelMask(channelMask).build();
    }

    private static void checkOutput(AudioTrack speaker, List<AudioDeviceInfo> expected) throws StreamFailure {
        List<AudioDeviceInfo> routed = MediaOutput.routedDevices(speaker);
        // Device IDs, not object equality or route order, identify the current sinks.
        if (routed.size() != expected.size()) throw new StreamFailure(R.string.error_output_lost);
        for (AudioDeviceInfo wanted : expected) {
            boolean found = false;
            for (AudioDeviceInfo actual : routed) {
                if (wanted.getId() == actual.getId()) {
                    found = true;
                    break;
                }
            }
            if (!found) throw new StreamFailure(R.string.error_output_lost);
        }
    }

    private String describeRoute(List<AudioDeviceInfo> devices) {
        if (devices.size() > 1) return getString(R.string.output_multiple);
        return MediaOutput.label(this, devices.get(0));
    }

    private static void checkCapture(AudioRecord record) throws StreamFailure {
        AudioDeviceInfo input = record.getRoutedDevice();
        if (input != null && input.getType() != AudioDeviceInfo.TYPE_BUILTIN_MIC) {
            throw new StreamFailure(R.string.error_microphone_route);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            AudioRecordingConfiguration configuration = record.getActiveRecordingConfiguration();
            if (configuration != null && configuration.isClientSilenced()) {
                throw new StreamFailure(R.string.error_microphone_busy);
            }
        }
    }

    @SuppressLint("MissingPermission") // The session verifies RECORD_AUDIO before opening capture.
    private static AudioRecord openMicrophone(int rate, int bytes, int source) {
        return new AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(format(rate, INPUT))
                .setBufferSizeInBytes(bytes)
                .build();
    }

    private static final class CaptureEffect {
        private final String name;
        private final IntFunction<? extends AudioEffect> create;
        private AudioEffect effect;

        CaptureEffect(String name, IntFunction<? extends AudioEffect> create) {
            this.name = name;
            this.create = create;
        }

        boolean configure(int sessionId, boolean enabled) {
            try {
                if (effect == null) effect = create.apply(sessionId);
                return effect != null && (effect.getEnabled() == enabled
                        || effect.setEnabled(enabled) == AudioEffect.SUCCESS);
            } catch (RuntimeException e) {
                Log.w(TAG, name + " could not be configured", e);
                return false;
            }
        }

        void release() {
            if (effect == null) return;
            try {
                effect.release();
            } catch (RuntimeException e) {
                Log.w(TAG, name + " release", e);
            }
        }
    }

    private static final class InputFilter {
        private final double coefficient;
        private double previousInput;
        private double previousOutput;

        InputFilter(int rate) {
            // Remove low-frequency handling and wind rumble without buffering speech.
            coefficient = Math.exp(-2.0 * Math.PI * 85.0 / rate);
        }

        void cleanAndMeasure(short[] samples, int count) {
            double sum = 0.0;
            for (int i = 0; i < count; i++) {
                double input = samples[i];
                previousOutput = coefficient * (previousOutput + input - previousInput);
                previousInput = input;
                samples[i] = (short) Math.round(Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, previousOutput)));
                double value = samples[i] / 32768.0;
                sum += value * value;
            }
            float measured = (float) Math.min(1.0, Math.sqrt(sum / count) * 3.0);
            audioLevel = Math.max(measured, audioLevel * 0.90f);
        }
    }

    private static void release(AudioTrack speaker, AudioRecord record, CaptureEffect echo, CaptureEffect noise) {
        if (speaker != null) {
            // Drop queued audio rather than draining to a newly selected route.
            try { speaker.pause(); speaker.flush(); } catch (RuntimeException e) { Log.w(TAG, "Output cleanup", e); }
            try { speaker.release(); } catch (RuntimeException e) { Log.w(TAG, "Output release", e); }
        }
        echo.release();
        noise.release();
        releaseMicrophone(record);
    }

    private static void releaseMicrophone(AudioRecord record) {
        if (record != null) {
            try {
                if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) record.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "Microphone cleanup", e);
            }
            try { record.release(); } catch (RuntimeException e) { Log.w(TAG, "Microphone release", e); }
        }
    }

    private static final class StreamFailure extends Exception {
        final int message;

        StreamFailure(int message) {
            this.message = message;
        }
    }

    @Override public void onDestroy() {
        destroyed = true;
        if (owner == this) owner = null;
        if (session != null) {
            // In unexpected teardown, hold STOPPING until the old worker releases its audio objects.
            state = State.STOPPING;
            session.cancel();
        } else {
            state = State.IDLE;
        }
        if (receiverRegistered) unregisterReceiver(audioSafetyReceiver);
        if (foreground) stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
