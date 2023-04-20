package chuangyuan.ycj.videolibrary.factory;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import java.util.ArrayList;

/**
 * 作者：By 15968
 * 日期：On 2022/6/16
 * 时间：At 18:41
 */

public class MyDefaultRenderersFactory extends DefaultRenderersFactory {

    public MyDefaultRenderersFactory(Context context) {
        super(context);
    }

    public MyDefaultRenderersFactory(Context context, @ExtensionRendererMode int extensionRendererMode){
        super(context, extensionRendererMode);
    }

    @Override
    protected void buildAudioRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, boolean enableDecoderFallback, AudioSink audioSink, Handler eventHandler, AudioRendererEventListener eventListener, ArrayList<Renderer> out) {
        super.buildAudioRenderers(context, extensionRendererMode, mediaCodecSelector, enableDecoderFallback, audioSink, eventHandler, eventListener, out);
        for (int i = 0; i < out.size(); i++) {
            if(out.get(i).getClass().getSimpleName().equals("FfmpegAudioRenderer")){
                Renderer renderer = out.remove(i);
                out.add(0, renderer);
                break;
            }
        }
    }
}