package bbslezy.video;

import bbslod.LodSettings;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LezyVideoSettingsHelperTest
{
    @BeforeAll
    static void setUp()
    {
        LodSettings.videoCqp = new ValueInt("video_cqp", 18, 0, 51);
        LodSettings.videoCodec = new ValueInt("video_codec", 0, 0, 2);
    }

    @Test
    void testCqpReplacement()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.videoCqp.set(23);
        LodSettings.videoCodec.set(0);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-qp 23"), "Should replace -qp 18 with -qp 23");
        assertTrue(result.contains("-c:v libx264"), "Should use libx264 codec");
    }

    @Test
    void testH265CodecReplacement()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.videoCqp.set(20);
        LodSettings.videoCodec.set(1);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v libx265"), "Should replace codec with libx265");
        assertTrue(result.contains("-qp 20"), "Should replace CQP with 20");
    }

    @Test
    void testVp9CodecReplacement()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.videoCqp.set(30);
        LodSettings.videoCodec.set(2);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v libvpx-vp9"), "Should replace codec with libvpx-vp9");
        assertTrue(result.contains("-crf 30"), "VP9 should use -crf");
        assertTrue(result.contains("%NAME%.webm"), "VP9 should output .webm");
    }
}
