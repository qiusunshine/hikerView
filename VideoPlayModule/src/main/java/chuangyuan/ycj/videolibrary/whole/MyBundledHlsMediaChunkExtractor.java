package chuangyuan.ycj.videolibrary.whole;

import androidx.annotation.VisibleForTesting;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor;
import com.google.android.exoplayer2.extractor.ts.Ac4Extractor;
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.source.hls.HlsMediaChunkExtractor;
import com.google.android.exoplayer2.source.hls.WebvttExtractor;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.TimestampAdjuster;

import java.io.IOException;

/**
 * 作者：By 15968
 * 日期：On 2021/10/6
 * 时间：At 19:16
 */

public class MyBundledHlsMediaChunkExtractor implements HlsMediaChunkExtractor {
    private static final PositionHolder POSITION_HOLDER = new PositionHolder();
    @VisibleForTesting
    final Extractor extractor;
    private final Format masterPlaylistFormat;
    private final TimestampAdjuster timestampAdjuster;

    public MyBundledHlsMediaChunkExtractor(Extractor extractor, Format masterPlaylistFormat, TimestampAdjuster timestampAdjuster) {
        this.extractor = extractor;
        this.masterPlaylistFormat = masterPlaylistFormat;
        this.timestampAdjuster = timestampAdjuster;
    }

    @Override
    public void init(ExtractorOutput extractorOutput) {
        extractor.init(extractorOutput);
    }

    @Override
    public boolean read(ExtractorInput extractorInput) throws IOException {
        try {
            return this.extractor.read(extractorInput, POSITION_HOLDER) == Extractor.RESULT_CONTINUE;
        } catch (IOException e) {
            e.printStackTrace();
            if(e instanceof com.google.android.exoplayer2.ParserException){
                return false;
            }
            throw e;
        }
    }

    public boolean isPackedAudioExtractor() {
        return extractor instanceof AdtsExtractor
                || extractor instanceof Ac3Extractor
                || extractor instanceof Ac4Extractor
                || extractor instanceof Mp3Extractor;
    }

    @Override
    public boolean isReusable() {
        return extractor instanceof TsExtractor || extractor instanceof FragmentedMp4Extractor;
    }

    @Override
    public HlsMediaChunkExtractor recreate() {
        Assertions.checkState(!isReusable());
        Extractor newExtractorInstance;
        if (extractor instanceof WebvttExtractor) {
            newExtractorInstance = new WebvttExtractor(masterPlaylistFormat.language, timestampAdjuster);
        } else if (extractor instanceof AdtsExtractor) {
            newExtractorInstance = new AdtsExtractor();
        } else if (extractor instanceof Ac3Extractor) {
            newExtractorInstance = new Ac3Extractor();
        } else if (extractor instanceof Ac4Extractor) {
            newExtractorInstance = new Ac4Extractor();
        } else if (extractor instanceof Mp3Extractor) {
            newExtractorInstance = new Mp3Extractor();
        } else {
            throw new IllegalStateException(
                    "Unexpected extractor type for recreation: " + extractor.getClass().getSimpleName());
        }
        return new MyBundledHlsMediaChunkExtractor(
                newExtractorInstance, masterPlaylistFormat, timestampAdjuster);
    }

    public void onTruncatedSegmentParsed() {
        this.extractor.seek(0L, 0L);
    }
}
