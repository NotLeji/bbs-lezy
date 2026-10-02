package leji.bbslezy.audio;

import mchorse.bbs_mod.audio.Wave;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LezyAudioReverseTest
{
    @Test
    void reverseFrames_mono16bit()
    {
        // 4 frames of 1 channel x 16-bit (2 bytes per frame): [F0, F1, F2, F3]
        byte[] original = new byte[] {
            10, 0,  // frame 0 (value 10)
            20, 0,  // frame 1 (value 20)
            30, 0,  // frame 2 (value 30)
            40, 0   // frame 3 (value 40)
        };
        byte[] data = original.clone();
        Wave wave = new Wave(1, 1, 44100, 16, data);

        LezyAudioReverse.reverseFrames(wave);

        byte[] expected = new byte[] {
            40, 0,
            30, 0,
            20, 0,
            10, 0
        };
        assertArrayEquals(expected, wave.data);

        // Round trip: reversing again should yield original bytes
        LezyAudioReverse.reverseFrames(wave);
        assertArrayEquals(original, wave.data);
    }

    @Test
    void reverseFrames_stereo16bit()
    {
        // 3 frames of 2 channels x 16-bit (4 bytes per frame): [F0, F1, F2]
        byte[] original = new byte[] {
            1, 2, 3, 4,    // frame 0
            5, 6, 7, 8,    // frame 1
            9, 10, 11, 12  // frame 2
        };
        byte[] data = original.clone();
        Wave wave = new Wave(1, 2, 44100, 16, data);
        assertEquals(4, wave.blockAlign);

        LezyAudioReverse.reverseFrames(wave);

        byte[] expected = new byte[] {
            9, 10, 11, 12, // frame 2
            5, 6, 7, 8,    // frame 1
            1, 2, 3, 4     // frame 0
        };
        assertArrayEquals(expected, wave.data);

        // Round trip
        LezyAudioReverse.reverseFrames(wave);
        assertArrayEquals(original, wave.data);
    }

    @Test
    void reverseFrames_emptyAndSingleFrame()
    {
        byte[] empty = new byte[0];
        Wave emptyWave = new Wave(1, 2, 44100, 16, empty);
        LezyAudioReverse.reverseFrames(emptyWave);
        assertEquals(0, emptyWave.data.length);

        byte[] single = new byte[] { 1, 2, 3, 4 };
        Wave singleWave = new Wave(1, 2, 44100, 16, single.clone());
        LezyAudioReverse.reverseFrames(singleWave);
        assertArrayEquals(single, singleWave.data);
    }
}
