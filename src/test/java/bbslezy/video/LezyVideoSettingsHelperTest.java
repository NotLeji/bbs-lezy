package bbslezy.video;

import bbslod.LodSettings;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
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
        LodSettings.hardwareAcceleration = new ValueBoolean("hardware_acceleration", true);
        LodSettings.gpuVendor = new ValueInt("gpu_vendor", 0, 0, 3);
    }

    @Test
    void testNvencGpuAcceleration()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.hardwareAcceleration.set(true);
        LodSettings.gpuVendor.set(1); // NVIDIA NVENC
        LodSettings.videoCodec.set(0);  // H.264
        LodSettings.videoCqp.set(22);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v h264_nvenc"), "Should use h264_nvenc");
        assertTrue(result.contains("-cq 22"), "NVENC should use -cq parameter");
        assertFalse(result.contains("-preset ultrafast"), "Should strip CPU preset");
    }

    @Test
    void testNvencHevcGpuAcceleration()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.hardwareAcceleration.set(true);
        LodSettings.gpuVendor.set(1); // NVIDIA NVENC
        LodSettings.videoCodec.set(1);  // H.265 / HEVC
        LodSettings.videoCqp.set(20);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v hevc_nvenc"), "Should use hevc_nvenc");
        assertTrue(result.contains("-tag:v hvc1"), "HEVC should have mp4 hvc1 tag");
        assertTrue(result.contains("-cq 20"), "Should use CQP 20");
    }

    @Test
    void testAmfGpuAcceleration()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.hardwareAcceleration.set(true);
        LodSettings.gpuVendor.set(2); // AMD AMF
        LodSettings.videoCodec.set(0);  // H.264
        LodSettings.videoCqp.set(24);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v h264_amf"), "Should use h264_amf");
        assertTrue(result.contains("-rc cqp"), "AMF should use -rc cqp");
        assertTrue(result.contains("-qp_i 24"), "AMF should use -qp_i");
    }

    @Test
    void testIntelQsvGpuAcceleration()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.hardwareAcceleration.set(true);
        LodSettings.gpuVendor.set(3); // Intel QSV
        LodSettings.videoCodec.set(0);  // H.264
        LodSettings.videoCqp.set(19);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v h264_qsv"), "Should use h264_qsv");
        assertTrue(result.contains("-global_quality 19"), "QSV should use -global_quality");
    }

    @Test
    void testCpuSoftwareFallback()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.hardwareAcceleration.set(false); // CPU software encoding
        LodSettings.videoCodec.set(0);
        LodSettings.videoCqp.set(21);

        String result = LezyVideoSettingsHelper.apply(defaultParams);

        assertTrue(result.contains("-c:v libx264"), "Should use CPU libx264");
        assertTrue(result.contains("-qp 21"), "Should replace -qp with 21");
    }
}
