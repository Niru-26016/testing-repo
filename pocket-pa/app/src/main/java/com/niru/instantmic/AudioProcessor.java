package com.niru.instantmic;

/**
 * No-internet PCM16 audio processor. All DSP state is owned by one audio thread.
 * The two "pitch" effects use a lightweight dual-window delay-line pitch shifter.
 * It trades sound quality for real-time performance and does not claim studio-grade quality.
 */
public final class AudioProcessor {
    public static final String CLEAN = "Clean";
    public static final String ECHO = "Echo";
    public static final String MEGAPHONE = "Megaphone";
    public static final String TELEPHONE = "Telephone";
    public static final String ROBOT = "Robot";
    public static final String DISTORTION = "Distortion";
    public static final String DEEP = "Deep";
    public static final String HELIUM = "Helium";
    public static final String ALIEN = "Alien";
    public static final String CAVE = "Cave";
    public static final String WHISPER = "Whisper";
    public static final String RADIO = "Radio";
    public static final String MAN = "Man";
    public static final String WOMAN = "Woman";
    public static final String CHILD = "Child";
    public static final String MONSTER = "Monster";
    public static final String GHOST = "Ghost";
    public static final String CHIPMUNK = "Chipmunk";

    public static final String[] EFFECTS = {
            CLEAN, MAN, WOMAN, CHILD, MONSTER, GHOST,
            DEEP, HELIUM, ROBOT, ALIEN, CHIPMUNK, WHISPER,
            ECHO, CAVE, RADIO, TELEPHONE, MEGAPHONE, DISTORTION
    };

    private final int rate;
    private final float[] echoBuffer;
    private final float[] caveBuffer;
    private final float[] pitchBuffer;
    private int echoIndex;
    private int caveIndex;
    private int pitchIndex;
    private final int grainLength;
    private final int minPitchDelay;
    private double phase = 0.25;
    private double lowPass;
    private double highPass;
    private double highPassPrevious;
    private double robotPhase;
    private double radioPhase;
    private final double gateAttack;
    private final double gateRelease;
    private double gateEnvelope = 1.0;
    private long randomState = 0x12345678L;

    public AudioProcessor(int sampleRate) {
        if (sampleRate < 8000 || sampleRate > 192000) {
            throw new IllegalArgumentException("Unsupported sample rate");
        }
        rate = sampleRate;
        gateAttack = 1.0 - Math.exp(-1.0 / (rate * 0.005));
        gateRelease = 1.0 - Math.exp(-1.0 / (rate * 0.090));
        echoBuffer = new float[Math.max(1, (int) (rate * 0.26))];
        caveBuffer = new float[Math.max(1, (int) (rate * 0.77))];
        grainLength = Math.max(128, (int) (rate * 0.045));
        minPitchDelay = Math.max(64, (int) (rate * 0.012));
        pitchBuffer = new float[Math.max(rate / 8, minPitchDelay + grainLength + 16)];
    }

    public void process(short[] pcm, int count, float inputGain, float outputGain,
                        String effect, boolean noiseGate, int sensitivity) {
        if (pcm == null || count < 0 || count > pcm.length) {
            throw new IllegalArgumentException("Invalid audio buffer");
        }
        if (!Float.isFinite(inputGain) || !Float.isFinite(outputGain)) {
            throw new IllegalArgumentException("Nonfinite gain");
        }
        inputGain = clamp(inputGain, 0f, 2f);
        outputGain = clamp(outputGain, 0f, 1.5f);
        sensitivity = Math.max(0, Math.min(100, sensitivity));
        if (count == 0) return;

        double sum = 0;
        for (int i = 0; i < count; i++) {
            double x = (pcm[i] / 32768.0) * inputGain;
            sum += x * x;
        }
        final double rms = Math.sqrt(sum / count);
        final double threshold = 0.006 + (100 - sensitivity) * 0.00034;
        final double target = !noiseGate || rms >= threshold ? 1.0 : 0.0;

        for (int i = 0; i < count; i++) {
            double x = (pcm[i] / 32768.0) * inputGain;
            double y = x;

            // Keep pitch history populated even while a non-pitch preset is selected.
            pitchBuffer[pitchIndex] = (float) x;

            if (TELEPHONE.equals(effect) || MEGAPHONE.equals(effect) || RADIO.equals(effect)) {
                double hpCutoff = RADIO.equals(effect) ? 500 : 350;
                double alphaHp = 1.0 / (1.0 + 2.0 * Math.PI * hpCutoff / rate);
                highPass = alphaHp * (highPass + x - highPassPrevious);
                highPassPrevious = x;
                double cutoff = TELEPHONE.equals(effect) ? 2800 : RADIO.equals(effect) ? 3300 : 4200;
                double alphaLp = (2.0 * Math.PI * cutoff / rate) / (1.0 + 2.0 * Math.PI * cutoff / rate);
                lowPass += alphaLp * (highPass - lowPass);
                y = lowPass;
            }

            if (ECHO.equals(effect)) {
                float delayed = echoBuffer[echoIndex];
                echoBuffer[echoIndex] = (float) clip(y + delayed * 0.29);
                y += delayed * 0.48;
            } else if (CAVE.equals(effect)) {
                float delayed = caveBuffer[caveIndex];
                caveBuffer[caveIndex] = (float) clip(y + delayed * 0.50);
                int tap1 = (caveIndex + caveBuffer.length - rate / 7 + caveBuffer.length) % caveBuffer.length;
                int tap2 = (caveIndex + caveBuffer.length - rate / 3 + caveBuffer.length) % caveBuffer.length;
                y = 0.67 * y + delayed * 0.37 + caveBuffer[tap1] * 0.30 + caveBuffer[tap2] * 0.27;
            } else if (TELEPHONE.equals(effect)) {
                y = Math.tanh(y * 2.2) * 0.90;
            } else if (MEGAPHONE.equals(effect)) {
                y = Math.tanh(y * 4.5) * 0.86;
            } else if (RADIO.equals(effect)) {
                radioPhase = wrapAngle(radioPhase + 2.0 * Math.PI * 18.0 / rate);
                y = Math.tanh(y * 3.2) * (0.89 + 0.11 * Math.sin(radioPhase));
            } else if (ROBOT.equals(effect)) {
                y *= Math.cos(robotPhase);
                robotPhase = wrapAngle(robotPhase + 2.0 * Math.PI * 72.0 / rate);
            } else if (DISTORTION.equals(effect)) {
                y = Math.tanh(y * 7.0) * 0.88;
            } else if (MAN.equals(effect)) {
                y = pitchShift(0.84);
                lowPass += 0.17 * (y - lowPass);
                y = 0.84 * y + 0.16 * lowPass;
            } else if (WOMAN.equals(effect)) {
                y = pitchShift(1.22);
                lowPass += 0.12 * (y - lowPass);
                y = 1.08 * y - 0.08 * lowPass;
            } else if (CHILD.equals(effect)) {
                y = pitchShift(1.48);
                lowPass += 0.09 * (y - lowPass);
                y = 1.07 * y - 0.07 * lowPass;
            } else if (MONSTER.equals(effect)) {
                robotPhase = wrapAngle(robotPhase + 2.0 * Math.PI * 25.0 / rate);
                y = Math.tanh(pitchShift(0.64) * 2.4)
                        * (0.80 + 0.20 * Math.cos(robotPhase));
            } else if (GHOST.equals(effect)) {
                float delayed = caveBuffer[caveIndex];
                caveBuffer[caveIndex] = (float) clip(x + delayed * 0.40);
                y = pitchShift(1.09) * 0.70 + delayed * 0.25;
                radioPhase = wrapAngle(radioPhase + 2.0 * Math.PI * 3.4 / rate);
                y *= 0.79 + 0.21 * Math.sin(radioPhase);
            } else if (CHIPMUNK.equals(effect)) {
                y = pitchShift(1.69);
            } else if (DEEP.equals(effect)) {
                y = pitchShift(0.76);
                // Warm up the lower register slightly.
                lowPass += 0.27 * (y - lowPass);
                y = 0.78 * y + 0.22 * lowPass;
            } else if (HELIUM.equals(effect)) {
                y = pitchShift(1.32);
            } else if (ALIEN.equals(effect)) {
                y = pitchShift(1.12);
                robotPhase = wrapAngle(robotPhase + 2.0 * Math.PI * 37.0 / rate);
                y *= 0.65 + 0.35 * Math.cos(robotPhase);
            } else if (WHISPER.equals(effect)) {
                randomState = (randomState * 1664525L + 1013904223L) & 0xFFFFFFFFL;
                double noise = (randomState / (double) 0xFFFFFFFFL) * 2.0 - 1.0;
                y = 0.38 * y + 1.4 * Math.abs(y) * noise;
            }

            pitchIndex = (pitchIndex + 1) % pitchBuffer.length;
            echoIndex = (echoIndex + 1) % echoBuffer.length;
            caveIndex = (caveIndex + 1) % caveBuffer.length;

            double smoothing = target > gateEnvelope ? gateAttack : gateRelease;
            gateEnvelope += (target - gateEnvelope) * smoothing;
            pcm[i] = (short) Math.round(clip(y * gateEnvelope * outputGain) * 32767.0);
        }
    }

    /** Two overlap-add read heads with Hann windows; lightweight approximate pitch shift. */
    private double pitchShift(double ratio) {
        double first = phase;
        double second = phase + 0.5;
        if (second >= 1.0) second -= 1.0;
        double w1 = Math.pow(Math.sin(Math.PI * first), 2);
        double w2 = Math.pow(Math.sin(Math.PI * second), 2);
        double a = delayedPitch(minPitchDelay + first * grainLength);
        double b = delayedPitch(minPitchDelay + second * grainLength);
        phase += (1.0 - ratio) / grainLength;
        phase -= Math.floor(phase);
        return (a * w1 + b * w2) / Math.max(0.001, w1 + w2);
    }

    private double delayedPitch(double delay) {
        double pos = pitchIndex - delay;
        while (pos < 0) pos += pitchBuffer.length;
        int before = (int) pos;
        int after = (before + 1) % pitchBuffer.length;
        double mix = pos - before;
        return pitchBuffer[before] * (1.0 - mix) + pitchBuffer[after] * mix;
    }

    private static double clip(double value) { return Math.max(-1.0, Math.min(1.0, value)); }
    private static float clamp(float value, float lo, float hi) { return Math.max(lo, Math.min(hi, value)); }
    private static double wrapAngle(double angle) { return angle >= 2.0 * Math.PI ? angle - 2.0 * Math.PI : angle; }
}
