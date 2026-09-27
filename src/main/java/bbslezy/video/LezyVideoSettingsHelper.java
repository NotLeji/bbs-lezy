package bbslezy.video;

import bbslod.LodSettings;

public class LezyVideoSettingsHelper
{
    public static String apply(String params)
    {
        if (params == null || params.isEmpty())
        {
            return params;
        }

        int cqp = LodSettings.videoCqp != null ? LodSettings.videoCqp.get() : 18;
        String codec = LodSettings.videoCodec != null ? LodSettings.videoCodec.get().toLowerCase().trim() : "h264";

        /* Replace CQP / CRF parameter */
        if (params.contains("-qp "))
        {
            params = params.replaceAll("-qp \\d+", "-qp " + cqp);
        }
        else if (params.contains("-crf "))
        {
            params = params.replaceAll("-crf \\d+", "-crf " + cqp);
        }

        /* Replace video codec */
        if ("h265".equals(codec) || "hevc".equals(codec))
        {
            params = params.replaceAll("-c:v \\S+", "-c:v libx265 -tag:v hvc1");
        }
        else if ("vp9".equals(codec))
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

        return params;
    }
}
