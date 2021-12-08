package chuangyuan.ycj.videolibrary.whole;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;

import chuangyuan.ycj.videolibrary.listener.DataSourceListener;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;
import chuangyuan.ycj.videolibrary.video.MediaSourceBuilder;

/**
 * Created by yangc on 2017/11/11
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated: 实现全多媒体数据类
 */

public class WholeMediaSource extends MediaSourceBuilder {

    public WholeMediaSource(@NonNull Context context) {
        super(context);
    }

    public WholeMediaSource(@NonNull Context context, @Nullable DataSourceListener listener) {
        super(context, listener);
    }

    @Override
    public MediaSource initMediaSource(@NonNull Uri uri) {
        MediaSource mediaSource = initMyMediaSource(uri);
        if (subtitle != null && subtitle.length() > 0) {
            Format textFormat;
            if (subtitle.contains(".vtt")) {
                textFormat = new Format.Builder()
                        .setSampleMimeType(MimeTypes.TEXT_VTT)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build();
            } else if (subtitle.contains(".ass")) {
                textFormat = new Format.Builder()
                        .setSampleMimeType(MimeTypes.TEXT_SSA)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build();
            } else {
                textFormat = new Format.Builder()
                        .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build();
            }
            MediaSource textMediaSource = new SingleSampleMediaSource.Factory(getDataSource())
                    .createMediaSource(Uri.parse(subtitle), textFormat, C.TIME_UNSET);
            return new MergingMediaSource(mediaSource, textMediaSource);
        }
        return mediaSource;
    }

    public MediaSource initMyMediaSource(@NonNull Uri uri) {
        int streamType = VideoPlayUtils.inferContentType(uri);
        switch (streamType) {
            case C.TYPE_SS:
                return new SsMediaSource.Factory(new DefaultSsChunkSource.Factory(getDataSource()), new DefaultDataSourceFactory(context, null,
                        getDataSource()))
                        .setLivePresentationDelayMs(10000)
                        .createMediaSource(uri);

            case C.TYPE_DASH:
                return new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(getDataSource())
                        , new DefaultDataSourceFactory(context, null, getDataSource()))
                        .setLivePresentationDelayMs(10000, false)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(getDataSource())
                        .setExtractorsFactory(new DefaultExtractorsFactory())
                        .setCustomCacheKey(uri.toString())
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(new DefaultHlsDataSourceFactory(getDataSource()))
                        .setAllowChunklessPreparation(true)
                        .setExtractorFactory(new MyHlsExtractorFactory())
                        .createMediaSource(uri);
            default:
                throw new IllegalStateException(":Unsupported type: " + streamType);
        }
    }
}
