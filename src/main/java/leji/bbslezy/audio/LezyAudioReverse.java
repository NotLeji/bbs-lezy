package leji.bbslezy.audio;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.audio.AudioReader;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LezyAudioReverse
{
    public static final String KEY_REVERSE = "bbslezy_reverse";
    public static final String KEY_VOLUME_CHANNEL = "bbslezy_volume";
    public static final String REVERSED_SOURCE = "bbslezy_rev";

    private static final Map<Link, SoundBuffer> REVERSED = new ConcurrentHashMap<>();

    public static Link toReversedLink(Link link)
    {
        if (link == null)
        {
            return null;
        }

        return new Link(REVERSED_SOURCE, link.source + "/" + link.path);
    }

    public static Link fromReversedLink(Link link)
    {
        if (link == null || !REVERSED_SOURCE.equals(link.source))
        {
            return link;
        }

        int idx = link.path.indexOf('/');

        if (idx < 0)
        {
            return Link.assets(link.path);
        }

        return new Link(link.path.substring(0, idx), link.path.substring(idx + 1));
    }

    public static KeyframeChannel<Double> getVolumeChannel(AudioClip clip)
    {
        if (clip == null)
        {
            return null;
        }

        try
        {
            BaseValue value = ((ValueGroup) (Object) clip).get(KEY_VOLUME_CHANNEL);

            if (value instanceof KeyframeChannel<?> channel)
            {
                return (KeyframeChannel<Double>) channel;
            }
        }
        catch (Throwable ignored)
        {}

        return null;
    }

    public static float computeGain(AudioClip clip, float tickTime)
    {
        if (clip == null)
        {
            return 1F;
        }

        float gain = clip.volume.get();

        KeyframeChannel<Double> volumeChannel = getVolumeChannel(clip);

        if (volumeChannel != null && !volumeChannel.isEmpty())
        {
            gain = (float) (double) volumeChannel.interpolate(tickTime);
        }
        else if (clip.envelope != null && clip.envelope.keyframes.get())
        {
            gain *= clip.envelope.factorEnabled(clip.duration.get(), tickTime);
        }

        return Math.max(0F, gain);
    }

    public static boolean isEnabled(AudioClip clip)
    {
        if (clip == null)
        {
            return false;
        }

        try
        {
            BaseValue value = ((ValueGroup) (Object) clip).get(KEY_REVERSE);

            if (value instanceof ValueBoolean bool)
            {
                return bool.get();
            }
        }
        catch (Throwable ignored)
        {}

        return false;
    }

    public static void setEnabled(AudioClip clip, boolean enabled)
    {
        if (clip == null)
        {
            return;
        }

        try
        {
            BaseValue value = ((ValueGroup) (Object) clip).get(KEY_REVERSE);

            if (value instanceof ValueBoolean bool)
            {
                bool.set(enabled);
            }
        }
        catch (Throwable ignored)
        {}
    }

    public static SoundBuffer getReversed(Link link)
    {
        if (link == null)
        {
            return null;
        }

        return REVERSED.computeIfAbsent(link, (l) ->
        {
            try
            {
                Link originalLink = REVERSED_SOURCE.equals(l.source) ? fromReversedLink(l) : l;
                Wave wave = AudioReader.read(BBSMod.getProvider(), originalLink);

                if (wave != null)
                {
                    reverseFrames(wave);

                    return new SoundBuffer(l, wave, null);
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }

            return null;
        });
    }

    public static void reverseFrames(Wave wave)
    {
        if (wave == null || wave.data == null || wave.blockAlign <= 0)
        {
            return;
        }

        int frame = wave.blockAlign;
        byte[] d = wave.data;
        int numFrames = d.length / frame;

        byte[] temp = new byte[frame];

        for (int i = 0; i < numFrames / 2; i++)
        {
            int leftOffset = i * frame;
            int rightOffset = (numFrames - 1 - i) * frame;

            System.arraycopy(d, leftOffset, temp, 0, frame);
            System.arraycopy(d, rightOffset, d, leftOffset, frame);
            System.arraycopy(temp, 0, d, rightOffset, frame);
        }
    }

    public static void mixClip(Wave finalWave, ByteBuffer buffer, AudioClip clip, Wave wave, float tick, float shift, float duration, float baseGain)
    {
        if (finalWave == null || clip == null || wave == null)
        {
            return;
        }

        try
        {
            Wave work = new Wave(
                wave.audioFormat,
                wave.numChannels,
                wave.sampleRate,
                wave.byteRate,
                wave.blockAlign,
                wave.bitsPerSample,
                wave.data.clone()
            );

            KeyframeChannel<Double> volumeChannel = getVolumeChannel(clip);
            boolean hasVolumeKeyframes = volumeChannel != null && !volumeChannel.isEmpty();
            boolean hasEnvelopeKeyframes = clip.envelope != null && clip.envelope.keyframes.get();
            boolean hasGainCurve = hasVolumeKeyframes || hasEnvelopeKeyframes;
            boolean reverse = isEnabled(clip);

            if (hasGainCurve && work.getBytesPerSample() == 2)
            {
                byte[] data = work.data;
                int channels = work.numChannels;
                int sampleRate = work.sampleRate;
                int frameBytes = work.blockAlign;
                int totalFrames = data.length / frameBytes;

                for (int f = 0; f < totalFrames; f++)
                {
                    float clipTick = f * 20F / (float) sampleRate;
                    float gain;

                    if (hasVolumeKeyframes)
                    {
                        gain = (float) (double) volumeChannel.interpolate(clipTick);
                    }
                    else
                    {
                        gain = clip.volume.get() * clip.envelope.factorEnabled(clip.duration.get(), clipTick);
                    }

                    gain = Math.max(0F, gain);

                    for (int ch = 0; ch < channels; ch++)
                    {
                        int byteIdx = f * frameBytes + ch * 2;
                        short sample = (short) ((data[byteIdx] & 0xFF) | (data[byteIdx + 1] << 8));
                        int scaled = Math.round(sample * gain);
                        scaled = Math.max(-32768, Math.min(32767, scaled));

                        data[byteIdx] = (byte) (scaled & 0xFF);
                        data[byteIdx + 1] = (byte) ((scaled >> 8) & 0xFF);
                    }
                }
            }

            float finalShift = shift;

            if (reverse)
            {
                reverseFrames(work);
                finalShift = wave.getDuration() - shift - duration;
            }

            float finalGain = hasGainCurve ? 1F : baseGain;
            finalWave.add(buffer, work, tick, finalShift, duration, finalGain);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    public static void clear()
    {
        for (SoundBuffer buffer : REVERSED.values())
        {
            if (buffer != null)
            {
                try
                {
                    buffer.delete();
                }
                catch (Throwable ignored)
                {}
            }
        }

        REVERSED.clear();
    }
}
