package bbslezy.video;

import bbslod.LodSettings;
import org.lwjgl.opengl.GL11;

public class LezyVideoSettingsHelper
{
    public static volatile boolean forceCpuOnce = false;

    public static String apply(String params)
    {
        if (params == null || params.isEmpty())
        {
            return params;
        }

        int cqp = LodSettings.videoCqp != null ? LodSettings.videoCqp.get() : 18;
        int codecMode = LodSettings.videoCodec != null ? LodSettings.videoCodec.get() : 0;
        boolean hwAccel = LodSettings.hardwareAcceleration == null || LodSettings.hardwareAcceleration.get();
        int gpuMode = LodSettings.gpuVendor != null ? LodSettings.gpuVendor.get() : 0;

        int detectedGpu = detectGpu(gpuMode);

        if (forceCpuOnce || !hwAccel)
        {
            forceCpuOnce = false;
            return applyCpuEncoding(params, codecMode, cqp);
        }

        return applyGpuEncoding(params, codecMode, detectedGpu, cqp);
    }

    public static boolean isHwAccelUnsupported()
    {
        if (forceCpuOnce)
        {
            return false;
        }

        boolean hwAccel = LodSettings.hardwareAcceleration == null || LodSettings.hardwareAcceleration.get();

        if (!hwAccel)
        {
            return false;
        }

        int codecMode = LodSettings.videoCodec != null ? LodSettings.videoCodec.get() : 0;

        /* VP9 (codecMode == 2) has no hardware encoder in NVIDIA (NVENC), AMD (AMF), or Intel consumer GPUs. */
        if (codecMode == 2)
        {
            return true;
        }

        return false;
    }

    public static String getGpuName()
    {
        int gpuMode = LodSettings.gpuVendor != null ? LodSettings.gpuVendor.get() : 0;
        int detectedGpu = detectGpu(gpuMode);

        switch (detectedGpu)
        {
            case 1: return "NVIDIA (NVENC)";
            case 2: return "AMD (AMF)";
            case 3: return "Intel (QSV)";
            default: return "GPU";
        }
    }

    public static String getCodecName()
    {
        int codecMode = LodSettings.videoCodec != null ? LodSettings.videoCodec.get() : 0;

        switch (codecMode)
        {
            case 1: return "H.265 / HEVC (MP4)";
            case 2: return "VP9 (WebM)";
            default: return "H.264 (MP4)";
        }
    }

    private static int detectGpu(int gpuMode)
    {
        if (gpuMode != 0)
        {
            return gpuMode; // 1 = NVIDIA, 2 = AMD, 3 = Intel
        }

        try
        {
            if (org.lwjgl.opengl.GL.getCapabilities() != null)
            {
                String vendor = (GL11.glGetString(GL11.GL_VENDOR) + " " + GL11.glGetString(GL11.GL_RENDERER)).toLowerCase();

                if (vendor.contains("nvidia") || vendor.contains("geforce") || vendor.contains("quadro"))
                {
                    return 1;
                }

                if (vendor.contains("amd") || vendor.contains("ati") || vendor.contains("radeon"))
                {
                    return 2;
                }

                if (vendor.contains("intel") || vendor.contains("uhd") || vendor.contains("iris") || vendor.contains("arc"))
                {
                    return 3;
                }
            }
        }
        catch (Throwable ignored)
        {}

        return 1; // Default to NVIDIA if cannot query OpenGL string
    }

    private static String applyGpuEncoding(String params, int codecMode, int gpu, int cqp)
    {
        /* Remove CPU-specific tuning params that break hardware encoders */
        params = params.replaceAll("-preset \\S+", "");
        params = params.replaceAll("-tune \\S+", "");
        params = params.replaceAll("-qp \\d+", "");
        params = params.replaceAll("-crf \\d+", "");

        String encoderArgs;

        if (gpu == 1) // NVIDIA NVENC
        {
            if (codecMode == 1)
            {
                encoderArgs = "-c:v hevc_nvenc -preset p4 -cq " + cqp + " -tag:v hvc1";
            }
            else if (codecMode == 2)
            {
                encoderArgs = "-c:v libvpx-vp9 -b:v 0 -deadline realtime -crf " + cqp;
                params = params.replaceAll("%NAME%\\.mp4", "%NAME%.webm");
            }
            else
            {
                encoderArgs = "-c:v h264_nvenc -preset p4 -cq " + cqp;
            }
        }
        else if (gpu == 2) // AMD AMF
        {
            if (codecMode == 1)
            {
                encoderArgs = "-c:v hevc_amf -rc cqp -qp_i " + cqp + " -qp_p " + cqp + " -tag:v hvc1";
            }
            else if (codecMode == 2)
            {
                encoderArgs = "-c:v libvpx-vp9 -b:v 0 -deadline realtime -crf " + cqp;
                params = params.replaceAll("%NAME%\\.mp4", "%NAME%.webm");
            }
            else
            {
                encoderArgs = "-c:v h264_amf -rc cqp -qp_i " + cqp + " -qp_p " + cqp;
            }
        }
        else // Intel QSV
        {
            if (codecMode == 1)
            {
                encoderArgs = "-c:v hevc_qsv -global_quality " + cqp + " -tag:v hvc1";
            }
            else if (codecMode == 2)
            {
                encoderArgs = "-c:v libvpx-vp9 -b:v 0 -deadline realtime -crf " + cqp;
                params = params.replaceAll("%NAME%\\.mp4", "%NAME%.webm");
            }
            else
            {
                encoderArgs = "-c:v h264_qsv -global_quality " + cqp;
            }
        }

        params = params.replaceAll("-c:v \\S+", encoderArgs);

        return params.replaceAll("\\s+", " ").trim();
    }

    private static String applyCpuEncoding(String params, int codecMode, int cqp)
    {
        /* Replace CQP / CRF parameter */
        if (params.contains("-qp "))
        {
            params = params.replaceAll("-qp \\d+", "-qp " + cqp);
        }
        else if (params.contains("-crf "))
        {
            params = params.replaceAll("-crf \\d+", "-crf " + cqp);
        }

        if (codecMode == 1)
        {
            params = params.replaceAll("-c:v \\S+", "-c:v libx265 -tag:v hvc1");
        }
        else if (codecMode == 2)
        {
            params = params.replaceAll("-c:v \\S+", "-c:v libvpx-vp9 -b:v 0");
            params = params.replaceAll("-tune zerolatency", "-deadline realtime");
            params = params.replaceAll("-qp \\d+", "-crf " + cqp);
            params = params.replaceAll("%NAME%\\.mp4", "%NAME%.webm");
        }
        else
        {
            params = params.replaceAll("-c:v \\S+", "-c:v libx264");
        }

        return params.replaceAll("\\s+", " ").trim();
    }
}
