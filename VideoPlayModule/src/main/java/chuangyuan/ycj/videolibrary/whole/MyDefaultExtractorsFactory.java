package chuangyuan.ycj.videolibrary.whole;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.amr.AmrExtractor;
import com.google.android.exoplayer2.extractor.flac.FlacExtractor;
import com.google.android.exoplayer2.extractor.flv.FlvExtractor;
import com.google.android.exoplayer2.extractor.jpeg.JpegExtractor;
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer2.extractor.ogg.OggExtractor;
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor;
import com.google.android.exoplayer2.extractor.ts.Ac4Extractor;
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.extractor.wav.WavExtractor;
import com.google.android.exoplayer2.util.FileTypes;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class MyDefaultExtractorsFactory implements ExtractorsFactory {
    private static final int[] DEFAULT_EXTRACTOR_ORDER = new int[]{5, 4, 12, 8, 3, 10, 9, 11, 6, 2, 0, 1, 7, 14};
    @Nullable
    private static final Constructor<? extends Extractor> FLAC_EXTENSION_EXTRACTOR_CONSTRUCTOR;
    private boolean constantBitrateSeekingEnabled;
    private int adtsFlags;
    private int amrFlags;
    private int flacFlags;
    private int matroskaFlags;
    private int mp4Flags;
    private int fragmentedMp4Flags;
    private int mp3Flags;
    private int tsMode = 1;
    private int tsFlags;
    private int tsTimestampSearchBytes = 112800;

    public MyDefaultExtractorsFactory() {
    }

    public synchronized MyDefaultExtractorsFactory setConstantBitrateSeekingEnabled(boolean constantBitrateSeekingEnabled) {
        this.constantBitrateSeekingEnabled = constantBitrateSeekingEnabled;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setAdtsExtractorFlags(int flags) {
        this.adtsFlags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setAmrExtractorFlags(int flags) {
        this.amrFlags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setFlacExtractorFlags(int flags) {
        this.flacFlags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setMatroskaExtractorFlags(int flags) {
        this.matroskaFlags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setMp4ExtractorFlags(int flags) {
        this.mp4Flags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setFragmentedMp4ExtractorFlags(int flags) {
        this.fragmentedMp4Flags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setMp3ExtractorFlags(int flags) {
        this.mp3Flags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setTsExtractorMode(int mode) {
        this.tsMode = mode;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setTsExtractorFlags(int flags) {
        this.tsFlags = flags;
        return this;
    }

    public synchronized MyDefaultExtractorsFactory setTsExtractorTimestampSearchBytes(int timestampSearchBytes) {
        this.tsTimestampSearchBytes = timestampSearchBytes;
        return this;
    }

    public synchronized Extractor[] createExtractors() {
        return this.createExtractors(Uri.EMPTY, new HashMap());
    }

    public synchronized Extractor[] createExtractors(Uri uri, Map<String, List<String>> responseHeaders) {
        List<Extractor> extractors = new ArrayList(14);
        List<String> contentTypes = (List)responseHeaders.get("Content-Type");
        String mimeType = contentTypes != null && !contentTypes.isEmpty() ? (String)contentTypes.get(0) : null;
        if("image/png".equals(mimeType)){
            //强制处理为ts
            responseHeaders.put("Content-Type", new ArrayList<>(Collections.singletonList("video/mp2t")));
        }
        int responseHeadersInferredFileType = FileTypes.inferFileTypeFromResponseHeaders(responseHeaders);
        if (responseHeadersInferredFileType != -1) {
            this.addExtractorsForFileType(responseHeadersInferredFileType, extractors);
        }

        int uriInferredFileType = FileTypes.inferFileTypeFromUri(uri);
        if (uriInferredFileType != -1 && uriInferredFileType != responseHeadersInferredFileType) {
            this.addExtractorsForFileType(uriInferredFileType, extractors);
        }

        int[] var6 = DEFAULT_EXTRACTOR_ORDER;
        int var7 = var6.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            int fileType = var6[var8];
            if (fileType != responseHeadersInferredFileType && fileType != uriInferredFileType) {
                this.addExtractorsForFileType(fileType, extractors);
            }
        }

        return (Extractor[])extractors.toArray(new Extractor[extractors.size()]);
    }

    private void addExtractorsForFileType(int fileType, List<Extractor> extractors) {
        switch(fileType) {
            case -1:
            case 13:
            default:
                break;
            case 0:
                extractors.add(new Ac3Extractor());
                break;
            case 1:
                extractors.add(new Ac4Extractor());
                break;
            case 2:
                extractors.add(new AdtsExtractor(this.adtsFlags | (this.constantBitrateSeekingEnabled ? 1 : 0)));
                break;
            case 3:
                extractors.add(new AmrExtractor(this.amrFlags | (this.constantBitrateSeekingEnabled ? 1 : 0)));
                break;
            case 4:
                if (FLAC_EXTENSION_EXTRACTOR_CONSTRUCTOR != null) {
                    try {
                        extractors.add((Extractor)FLAC_EXTENSION_EXTRACTOR_CONSTRUCTOR.newInstance(this.flacFlags));
                    } catch (Exception var4) {
                        throw new IllegalStateException("Unexpected error creating FLAC extractor", var4);
                    }
                } else {
                    extractors.add(new FlacExtractor(this.flacFlags));
                }
                break;
            case 5:
                extractors.add(new FlvExtractor());
                break;
            case 6:
                extractors.add(new MatroskaExtractor(this.matroskaFlags));
                break;
            case 7:
                extractors.add(new Mp3Extractor(this.mp3Flags | (this.constantBitrateSeekingEnabled ? 1 : 0)));
                break;
            case 8:
                extractors.add(new FragmentedMp4Extractor(this.fragmentedMp4Flags));
                extractors.add(new Mp4Extractor(this.mp4Flags));
                break;
            case 9:
                extractors.add(new OggExtractor());
                break;
            case 10:
                extractors.add(new PsExtractor());
                break;
            case 11:
                extractors.add(new TsExtractor(this.tsMode, this.tsFlags, this.tsTimestampSearchBytes));
                break;
            case 12:
                extractors.add(new WavExtractor());
                break;
            case 14:
                extractors.add(new JpegExtractor());
        }

    }

    static {
        Constructor flacExtensionExtractorConstructor = null;

        try {
            boolean isFlacNativeLibraryAvailable = Boolean.TRUE.equals(Class.forName("com.google.android.exoplayer2.ext.flac.FlacLibrary").getMethod("isAvailable").invoke((Object)null));
            if (isFlacNativeLibraryAvailable) {
                flacExtensionExtractorConstructor = Class.forName("com.google.android.exoplayer2.ext.flac.FlacExtractor").asSubclass(Extractor.class).getConstructor(Integer.TYPE);
            }
        } catch (ClassNotFoundException var2) {
        } catch (Exception var3) {
            throw new RuntimeException("Error instantiating FLAC extension", var3);
        }

        FLAC_EXTENSION_EXTRACTOR_CONSTRUCTOR = flacExtensionExtractorConstructor;
    }
}
