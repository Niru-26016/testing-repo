package com.niru.pocketpa;

/**
 * Platform-independent streaming PCM processor. Audio samples are normalized
 * to [-1, 1], transformed, optionally gated, and limited to signed PCM16.
 * All mutable state is owned by the single audio thread.
 */
public final class AudioProcessor {
    public static final String CLEAN = "Clean";
    public static final String ECHO = "Echo";
    public static final String MEGAPHONE = "Megaphone";
    public static final String TELEPHONE = "Telephone";
    public static final String ROBOT = "Robot";
    public static final String DISTORTION = "Distortion";

    private final int rate;
    private final float[] echoBuffer;
    private int echoIndex;
    private double lowPass;
    private double highPass;
    private double highPassPrevious;
    private double robotPhase;
    private double gateEnvelope = 1.0;

    public AudioProcessor(int sampleRate) {
        if (sampleRate < 8000) throw new IllegalArgumentException("Invalid sample rate");
        rate = sampleRate;
        echoBuffer = new float[Math.max(1, (int) (rate * 0.24))];
    }

    /** Processes {@code count} samples in place. Never stores or uploads audio. */
    public void process(short[] pcm, int count, float inputGain, float outputGain,
                        String effect, boolean noiseGate, int sensitivity) {
        if (pcm == null || count < 0 || count > pcm.length) {
            throw new IllegalArgumentException("Invalid audio buffer");
        }
        inputGain = clamp(inputGain, 0f, 2f);
        outputGain = clamp(outputGain, 0f, 1.5f);
        sensitivity = Math.max(0, Math.min(100, sensitivity));

        double sum = 0;
        for (int i = 0; i < count; i++) {
            double x = (pcm[i] / 32768.0) * inputGain;
            sum += x * x;
        }
        double rms = Math.sqrt(sum / Math.max(1, count));
        // Higher sensitivity opens on quieter sounds.
        double threshold = 0.006 + (100 - sensitivity) * 0.00034;
        double target = !noiseGate || rms >= threshold ? 1.0 : 0.0;

        for (int i = 0; i < count; i++) {
            double x = (pcm[i] / 32768.0) * inputGain;
            double filtered;
            if (TELEPHONE.equals(effect) || MEGAPHONE.equals(effect)) {
                // Approximate 350 Hz HP + 3000 Hz LP; leave state continuous across buffers.
                double alphaHp = 1.0 / (1.0 + (2.0 * Math.PI * 350.0 / rate));
                highPass = alphaHp * (highPass + x - highPassPrevious);
                highPassPrevious = x;
                double lpCutoff = TELEPHONE.equals(effect) ? 2800.0 : 4200.0;
                double alphaLp = (2.0 * Math.PI * lpCutoff / rate) /
                        (1.0 + 2.0 * Math.PI * lpCutoff / rate);
                lowPass += alphaLp * (highPass - lowPass);
                filtered = lowPass;
            } else {
                filtered = x;
            }

            if (ECHO.equals(effect)) {
                float delayed = echoBuffer[echoIndex];
                echoBuffer[echoIndex] = (float) clamp((float) (filtered + delayed * 0.30), -1f, 1f);
                echoIndex = (echoIndex + 1) % echoBuffer.length;
                filtered += delayed * 0.45;
            } else if (TELEPHONE.equals(effect)) {
                filtered = Math.tanh(filtered * 2.2) * 0.9;
            } else if (MEGAPHONE.equals(effect)) {
                filtered = Math.tanh(filtered * 4.5) * 0.86;
            } else if (ROBOT.equals(effect)) {
                filtered *= Math.cos(robotPhase);
                robotPhase += 2.0 * Math.PI * 72.0 / rate;
                if (robotPhase >= 2.0 * Math.PI) robotPhase -= 2.0 * Math.PI;
            } else if (DISTORTION.equals(effect)) {
                filtered = Math.tanh(filtered * 7.0) * 0.88;
            }

            // 12ms attack and 90ms release, independent of audio buffer size.
            double time = target > gateEnvelope ? 0.012 : 0.090;
            double smoothing = 1.0 - Math.exp(-1.0 / (rate * time));
            gateEnvelope += (target - gateEnvelope) * smoothing;
            filtered *= gateEnvelope * outputGain;
            double limited = Math.max(-1.0, Math.min(1.0, filtered));
            pcm[i] = (short) Math.round(limited * 32767.0);
        }
    }

    private static float clamp(float x, float lo, float hi) {
        return Math.max(lo, Math.min(hi, x));
    }
}
