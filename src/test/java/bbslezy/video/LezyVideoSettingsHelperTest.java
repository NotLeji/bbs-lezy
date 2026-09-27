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

    @Test
    void testVp9UnsupportedOnNvencAndAmf()
    {
        LodSettings.hardwareAcceleration.set(true);
        LodSettings.videoCodec.set(2); // VP9

        LodSettings.gpuVendor.set(1); // NVIDIA
        assertTrue(LezyVideoSettingsHelper.isHwAccelUnsupported(), "VP9 should be flagged unsupported on NVENC");

        LodSettings.gpuVendor.set(2); // AMD
        assertTrue(LezyVideoSettingsHelper.isHwAccelUnsupported(), "VP9 should be flagged unsupported on AMF");

        LodSettings.videoCodec.set(0); // H.264
        assertFalse(LezyVideoSettingsHelper.isHwAccelUnsupported(), "H.264 should be supported on AMF");

        LodSettings.videoCodec.set(2); // VP9
        LodSettings.hardwareAcceleration.set(false); // Hardware accel OFF
        assertFalse(LezyVideoSettingsHelper.isHwAccelUnsupported(), "When HW accel is off, should not flag unsupported");
    }

    @Test
    void testForceCpuOnceOverridesAndResets()
    {
        String defaultParams = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

        LodSettings.hardwareAcceleration.set(true);
        LodSettings.gpuVendor.set(1); // NVIDIA
        LodSettings.videoCodec.set(2); // VP9
        LodSettings.videoCqp.set(20);

        LezyVideoSettingsHelper.forceCpuOnce = true;
        assertFalse(LezyVideoSettingsHelper.isHwAccelUnsupported(), "forceCpuOnce should bypass unsupported check");

        String result = LezyVideoSettingsHelper.apply(defaultParams);
        assertTrue(result.contains("-c:v libvpx-vp9"), "Should encode via CPU libvpx-vp9");
        assertTrue(result.contains("-crf 20"), "Should use -crf 20 for VP9");
        assertFalse(LezyVideoSettingsHelper.forceCpuOnce, "forceCpuOnce must be reset to false after apply");
    }

    @Test
    void testGetGpuAndCodecNames()
    {
        LodSettings.gpuVendor.set(1);
        assertEquals("NVIDIA (NVENC)", LezyVideoSettingsHelper.getGpuName());

        LodSettings.gpuVendor.set(2);
        assertEquals("AMD (AMF)", LezyVideoSettingsHelper.getGpuName());

        LodSettings.gpuVendor.set(3);
        assertEquals("Intel (QSV)", LezyVideoSettingsHelper.getGpuName());

        LodSettings.videoCodec.set(0);
        assertEquals("H.264 (MP4)", LezyVideoSettingsHelper.getCodecName());

        LodSettings.videoCodec.set(1);
        assertEquals("H.265 / HEVC (MP4)", LezyVideoSettingsHelper.getCodecName());

        LodSettings.videoCodec.set(2);
        assertEquals("VP9 (WebM)", LezyVideoSettingsHelper.getCodecName());
    }
}
