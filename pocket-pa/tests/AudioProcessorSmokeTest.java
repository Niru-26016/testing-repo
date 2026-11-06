package com.niru.pocketpa;

public final class AudioProcessorSmokeTest {
    public static void main(String[] args) {
        AudioProcessor processor = new AudioProcessor(48000);
        short[] samples = new short[480];
        String[] modes = {AudioProcessor.CLEAN, AudioProcessor.ECHO,
                AudioProcessor.MEGAPHONE, AudioProcessor.TELEPHONE,
                AudioProcessor.ROBOT, AudioProcessor.DISTORTION};
        for (String mode : modes) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) (Math.sin(i * 2 * Math.PI * 440 / 48000) * 12000);
            }
            processor.process(samples, samples.length, 1, 1, mode, false, 65);
            long magnitude = 0;
            for (short s : samples) magnitude += Math.abs((long) s);
            if (magnitude == 0) throw new AssertionError("Effect silenced output: " + mode);
        }
        short[] silence = new short[480];
        processor.process(silence, silence.length, 1, 1, AudioProcessor.CLEAN, true, 50);
        for (short s : silence) if (s != 0) throw new AssertionError("Silence not silent");
        boolean caught = false;
        try { processor.process(samples, 481, 1, 1, AudioProcessor.CLEAN, false, 50); }
        catch (IllegalArgumentException expected) { caught = true; }
        if (!caught) throw new AssertionError("Invalid buffer was accepted");
        System.out.println("AudioProcessor tests passed: 6 effects, silence, buffer validation");
    }
}
