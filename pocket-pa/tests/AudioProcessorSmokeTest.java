package com.niru.pocketpa;

/** Runs on JDK with no Android dependency. */
public final class AudioProcessorSmokeTest {
    public static void main(String[] args) {
        for (String mode : AudioProcessor.EFFECTS) {
            AudioProcessor processor = new AudioProcessor(48000);
            long magnitude = 0;
            for (int frame = 0; frame < 55; frame++) {
                short[] samples = new short[480];
                for (int i = 0; i < samples.length; i++) {
                    int index = frame * samples.length + i;
                    samples[i] = (short) (Math.sin(index * 2 * Math.PI * 440 / 48000) * 12000);
                }
                processor.process(samples, samples.length, 1, 1, mode, false, 65);
                if (frame > 5) for (short s : samples) magnitude += Math.abs((long) s);
            }
            if (magnitude < 10000) throw new AssertionError("Preset silenced output: " + mode);
        }
        if (AudioProcessor.EFFECTS.length != 18)
            throw new AssertionError("Expected 18 live presets");
        AudioProcessor processor = new AudioProcessor(48000);
        short[] silence = new short[480];
        processor.process(silence, silence.length, 1, 1, AudioProcessor.CLEAN, true, 50);
        for (short sample : silence) if (sample != 0) throw new AssertionError("Silence not silent");
        boolean caught = false;
        try { processor.process(silence, 481, 1, 1, AudioProcessor.CLEAN, false, 50); }
        catch (IllegalArgumentException expected) { caught = true; }
        if (!caught) throw new AssertionError("Expected invalid buffer rejection");
        caught = false;
        try { processor.process(silence, silence.length, Float.NaN, 1, AudioProcessor.CLEAN, false, 50); }
        catch (IllegalArgumentException expected) { caught = true; }
        if (!caught) throw new AssertionError("Expected NaN gain rejection");
        System.out.println("All " + AudioProcessor.EFFECTS.length + " effects pass: audio, silence, invalid buffer, NaN gain");
    }
}
