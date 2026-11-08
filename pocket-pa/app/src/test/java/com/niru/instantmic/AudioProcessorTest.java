package com.niru.instantmic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class AudioProcessorTest {
    private static final int RATE = 48_000;
    private static final String[] EXPECTED_PRESETS = {
            "Clean", "Man", "Woman", "Child", "Monster", "Ghost", "Deep", "Helium",
            "Robot", "Alien", "Chipmunk", "Whisper", "Echo", "Cave", "Radio",
            "Telephone", "Megaphone", "Distortion"
    };

    @Test
    public void allPresetsProduceDistinctAudioAndRespectSilence() {
        assertArrayEquals(EXPECTED_PRESETS, AudioProcessor.EFFECTS);
        for (int rate : new int[]{16_000, 44_100, RATE}) {
            Set<Integer> fingerprints = new HashSet<>();
            for (String effect : AudioProcessor.EFFECTS) {
                short[] audio = new short[rate];
                short[] silence = new short[rate / 2];
                new AudioProcessor(rate).process(silence, silence.length, 1, 1, effect, true, 65);
                assertArrayEquals(effect + " generated audio from silence", new short[silence.length], silence);

                for (int i = 0; i < audio.length; i++) {
                    double t = (double) i / rate;
                    audio[i] = (short) (Math.sin(t * 2 * Math.PI * 440) * 9000
                            + Math.sin(t * 2 * Math.PI * 703) * 3000);
                }
                new AudioProcessor(rate).process(audio, audio.length, 1, 1, effect, false, 65);
                long magnitude = 0;
                for (short sample : audio) magnitude += Math.abs((int) sample);
                assertTrue(effect + " unexpectedly silenced the input at " + rate, magnitude > rate * 100L);
                assertTrue(effect + " matches a different preset at " + rate,
                        fingerprints.add(Arrays.hashCode(audio)));
            }
        }
    }

    @Test
    public void cleanAppliesGainClampsPeaksAndLeavesUnusedFramesAlone() {
        AudioProcessor processor = new AudioProcessor(RATE);
        short[] audio = {16_000, -16_000, 99};
        processor.process(audio, 2, 0.5f, 0.5f, AudioProcessor.CLEAN, false, 50);
        assertArrayEquals(new short[]{4000, -4000, 99}, audio);

        short[] peaks = {Short.MAX_VALUE, Short.MIN_VALUE};
        processor.process(peaks, 2, 100, 100, AudioProcessor.CLEAN, false, 50);
        assertArrayEquals(new short[]{Short.MAX_VALUE, -Short.MAX_VALUE}, peaks);
        processor.process(peaks, 2, -1, 1, AudioProcessor.CLEAN, false, 50);
        assertArrayEquals(new short[2], peaks);
    }

    @Test
    public void noiseGateAttenuatesQuietAudioAndReopensForSpeech() {
        AudioProcessor processor = new AudioProcessor(RATE);
        short[] audio = new short[RATE];
        Arrays.fill(audio, (short) 100);
        processor.process(audio, audio.length, 1, 1, AudioProcessor.CLEAN, true, 65);
        assertTrue("Quiet sound should fade out", Math.abs(audio[audio.length - 1]) <= 1);

        Arrays.fill(audio, (short) 10_000);
        processor.process(audio, audio.length, 1, 1, AudioProcessor.CLEAN, true, 65);
        assertTrue("Loud sound should open the gate", audio[audio.length - 1] >= 9990);
    }

    @Test
    public void echoRetainsHistoryAcrossAudioFrames() {
        AudioProcessor processor = new AudioProcessor(RATE);
        short[] firstFrame = new short[480];
        firstFrame[0] = 12_000;
        processor.process(firstFrame, firstFrame.length, 1, 1, AudioProcessor.ECHO, false, 65);
        assertEquals(12_000, firstFrame[0]);

        int delay = (int) (RATE * 0.26);
        short[] followingFrames = new short[delay];
        processor.process(followingFrames, followingFrames.length, 1, 1, AudioProcessor.ECHO, false, 65);
        int echoIndex = delay - firstFrame.length;
        assertEquals(0, followingFrames[echoIndex - 1]);
        assertEquals(5760, followingFrames[echoIndex]);
    }

    @Test
    public void invalidInputFailsBeforeMutatingAudio() {
        AudioProcessor processor = new AudioProcessor(RATE);
        short[] audio = {1, 2, 3};
        assertThrows(IllegalArgumentException.class,
                () -> processor.process(null, 0, 1, 1, AudioProcessor.CLEAN, false, 50));
        assertThrows(IllegalArgumentException.class,
                () -> processor.process(audio, -1, 1, 1, AudioProcessor.CLEAN, false, 50));
        assertThrows(IllegalArgumentException.class,
                () -> processor.process(audio, 4, 1, 1, AudioProcessor.CLEAN, false, 50));
        assertThrows(IllegalArgumentException.class,
                () -> processor.process(audio, 3, Float.NaN, 1, AudioProcessor.CLEAN, false, 50));
        assertThrows(IllegalArgumentException.class,
                () -> processor.process(audio, 3, 1, Float.POSITIVE_INFINITY, AudioProcessor.CLEAN, false, 50));
        assertThrows(IllegalArgumentException.class, () -> new AudioProcessor(7999));
        assertThrows(IllegalArgumentException.class, () -> new AudioProcessor(192_001));
        processor.process(audio, 0, 1, 1, AudioProcessor.CLEAN, false, 50);
        assertArrayEquals(new short[]{1, 2, 3}, audio);
    }
}
