package chuangyuan.ycj.videolibrary.whole;


import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.hls.HlsDataSourceFactory;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist.Variant;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist.Part;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist.RenditionReport;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist.Segment;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParser.DeltaUpdateException;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistParserFactory;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistTracker;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.FallbackOptions;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.FallbackSelection;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.LoadErrorInfo;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.upstream.Loader.LoadErrorAction;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import com.google.android.exoplayer2.upstream.ParsingLoadable.Parser;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MyHlsPlaylistTracker implements HlsPlaylistTracker, Loader.Callback<ParsingLoadable<HlsPlaylist>> {
    private static final String TAG = "MyHlsPlaylistTracker";
    public static final Factory FACTORY = MyHlsPlaylistTracker::new;
    public static final double DEFAULT_PLAYLIST_STUCK_TARGET_DURATION_COEFFICIENT = 3.5D;
    private final HlsDataSourceFactory dataSourceFactory;
    private final HlsPlaylistParserFactory playlistParserFactory;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private final HashMap<Uri, MediaPlaylistBundle> playlistBundles;
    private final CopyOnWriteArrayList<PlaylistEventListener> listeners;
    private final double playlistStuckTargetDurationCoefficient;
    @Nullable
    private MediaSourceEventListener.EventDispatcher eventDispatcher;
    @Nullable
    private Loader initialPlaylistLoader;
    @Nullable
    private Handler playlistRefreshHandler;
    @Nullable
    private PrimaryPlaylistListener primaryPlaylistListener;
    @Nullable
    private HlsMasterPlaylist masterPlaylist;
    @Nullable
    private Uri primaryMediaPlaylistUrl;
    @Nullable
    private HlsMediaPlaylist primaryMediaPlaylistSnapshot;
    private boolean isLive;
    private long initialStartTimeUs;

    public MyHlsPlaylistTracker(HlsDataSourceFactory dataSourceFactory, LoadErrorHandlingPolicy loadErrorHandlingPolicy, HlsPlaylistParserFactory playlistParserFactory) {
        this(dataSourceFactory, loadErrorHandlingPolicy, playlistParserFactory, 3.5D);
    }

    public MyHlsPlaylistTracker(HlsDataSourceFactory dataSourceFactory, LoadErrorHandlingPolicy loadErrorHandlingPolicy, HlsPlaylistParserFactory playlistParserFactory, double playlistStuckTargetDurationCoefficient) {
        this.dataSourceFactory = dataSourceFactory;
        this.playlistParserFactory = playlistParserFactory;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.playlistStuckTargetDurationCoefficient = playlistStuckTargetDurationCoefficient;
        this.listeners = new CopyOnWriteArrayList();
        this.playlistBundles = new HashMap();
        this.initialStartTimeUs = -9223372036854775807L;
    }

    public void start(Uri initialPlaylistUri, MediaSourceEventListener.EventDispatcher eventDispatcher, PrimaryPlaylistListener primaryPlaylistListener) {
        this.playlistRefreshHandler = Util.createHandlerForCurrentLooper();
        this.eventDispatcher = eventDispatcher;
        this.primaryPlaylistListener = primaryPlaylistListener;
        ParsingLoadable<HlsPlaylist> masterPlaylistLoadable = new ParsingLoadable(this.dataSourceFactory.createDataSource(4), initialPlaylistUri, 4, this.playlistParserFactory.createPlaylistParser());
        Assertions.checkState(this.initialPlaylistLoader == null);
        this.initialPlaylistLoader = new Loader("MyHlsPlaylistTracker:MasterPlaylist");
        long elapsedRealtime = this.initialPlaylistLoader.startLoading(masterPlaylistLoadable, this, this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(masterPlaylistLoadable.type));
        eventDispatcher.loadStarted(new LoadEventInfo(masterPlaylistLoadable.loadTaskId, masterPlaylistLoadable.dataSpec, elapsedRealtime), masterPlaylistLoadable.type);
    }

    public void stop() {
        this.primaryMediaPlaylistUrl = null;
        this.primaryMediaPlaylistSnapshot = null;
        this.masterPlaylist = null;
        this.initialStartTimeUs = -9223372036854775807L;
        this.initialPlaylistLoader.release();
        this.initialPlaylistLoader = null;
        Iterator var1 = this.playlistBundles.values().iterator();

        while (var1.hasNext()) {
            MediaPlaylistBundle bundle = (MediaPlaylistBundle) var1.next();
            bundle.release();
        }

        this.playlistRefreshHandler.removeCallbacksAndMessages((Object) null);
        this.playlistRefreshHandler = null;
        this.playlistBundles.clear();
    }

    public void addListener(PlaylistEventListener listener) {
        Assertions.checkNotNull(listener);
        this.listeners.add(listener);
    }

    public void removeListener(PlaylistEventListener listener) {
        this.listeners.remove(listener);
    }

    @Nullable
    public HlsMasterPlaylist getMasterPlaylist() {
        return this.masterPlaylist;
    }

    @Nullable
    public HlsMediaPlaylist getPlaylistSnapshot(Uri url, boolean isForPlayback) {
        HlsMediaPlaylist snapshot = ((MediaPlaylistBundle) this.playlistBundles.get(url)).getPlaylistSnapshot();
        if (snapshot != null && isForPlayback) {
            this.maybeSetPrimaryUrl(url);
        }

        return snapshot;
    }

    public long getInitialStartTimeUs() {
        return this.initialStartTimeUs;
    }

    public boolean isSnapshotValid(Uri url) {
        return ((MediaPlaylistBundle) this.playlistBundles.get(url)).isSnapshotValid();
    }

    public void maybeThrowPrimaryPlaylistRefreshError() throws IOException {
        if (!isLive) {
            Log.e(TAG, "maybeThrowPrimaryPlaylistRefreshError: ");
            return;
        }
        if (this.initialPlaylistLoader != null) {
            this.initialPlaylistLoader.maybeThrowError();
        }

        if (this.primaryMediaPlaylistUrl != null) {
            this.maybeThrowPlaylistRefreshError(this.primaryMediaPlaylistUrl);
        }

    }

    public void maybeThrowPlaylistRefreshError(Uri url) throws IOException {
        if (!isLive) {
            Log.e(TAG, "maybeThrowPlaylistRefreshError: ");
            return;
        }
        ((MediaPlaylistBundle) this.playlistBundles.get(url)).maybeThrowPlaylistRefreshError();
    }

    public void refreshPlaylist(Uri url) {
        ((MediaPlaylistBundle) this.playlistBundles.get(url)).loadPlaylist();
    }

    public boolean isLive() {
        return this.isLive;
    }

    public boolean excludeMediaPlaylist(Uri playlistUrl, long exclusionDurationMs) {
        MediaPlaylistBundle bundle = (MediaPlaylistBundle) this.playlistBundles.get(playlistUrl);
        if (bundle != null) {
            return !bundle.excludePlaylist(exclusionDurationMs);
        } else {
            return false;
        }
    }

    public void onLoadCompleted(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs) {
        HlsPlaylist result = (HlsPlaylist) loadable.getResult();
        boolean isMediaPlaylist = result instanceof HlsMediaPlaylist;
        HlsMasterPlaylist masterPlaylist;
        if (isMediaPlaylist) {
            masterPlaylist = HlsMasterPlaylist.createSingleVariantMasterPlaylist(result.baseUri);
        } else {
            masterPlaylist = (HlsMasterPlaylist) result;
        }

        this.masterPlaylist = masterPlaylist;
        this.primaryMediaPlaylistUrl = ((Variant) masterPlaylist.variants.get(0)).url;
        this.listeners.add(new FirstPrimaryMediaPlaylistListener());
        this.createBundles(masterPlaylist.mediaPlaylistUrls);
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        MediaPlaylistBundle primaryBundle = (MediaPlaylistBundle) this.playlistBundles.get(this.primaryMediaPlaylistUrl);
        if (isMediaPlaylist) {
            primaryBundle.processLoadedPlaylist((HlsMediaPlaylist) result, loadEventInfo);
        } else {
            primaryBundle.loadPlaylist();
        }

        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.eventDispatcher.loadCompleted(loadEventInfo, 4);
    }

    public void onLoadCanceled(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        this.eventDispatcher.loadCanceled(loadEventInfo, 4);
    }

    public LoadErrorAction onLoadError(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
        long retryDelayMs = this.loadErrorHandlingPolicy.getRetryDelayMsFor(new LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount));
        boolean isFatal = retryDelayMs == -9223372036854775807L;
        this.eventDispatcher.loadError(loadEventInfo, loadable.type, error, isFatal);
        if (isFatal) {
            this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }

        return isFatal ? Loader.DONT_RETRY_FATAL : Loader.createRetryAction(false, retryDelayMs);
    }

    private boolean maybeSelectNewPrimaryUrl() {
        List<HlsMasterPlaylist.Variant> variants = this.masterPlaylist.variants;
        int variantsSize = variants.size();
        long currentTimeMs = SystemClock.elapsedRealtime();

        for (int i = 0; i < variantsSize; ++i) {
            MediaPlaylistBundle bundle = (MediaPlaylistBundle) Assertions.checkNotNull((MediaPlaylistBundle) this.playlistBundles.get(((Variant) variants.get(i)).url));
            if (currentTimeMs > bundle.excludeUntilMs) {
                this.primaryMediaPlaylistUrl = bundle.playlistUrl;
                bundle.loadPlaylistInternal(this.getRequestUriForPrimaryChange(this.primaryMediaPlaylistUrl));
                return true;
            }
        }

        return false;
    }

    private void maybeSetPrimaryUrl(Uri url) {
        if (!url.equals(this.primaryMediaPlaylistUrl) && this.isVariantUrl(url) && (this.primaryMediaPlaylistSnapshot == null || !this.primaryMediaPlaylistSnapshot.hasEndTag)) {
            this.primaryMediaPlaylistUrl = url;
            MediaPlaylistBundle newPrimaryBundle = (MediaPlaylistBundle) this.playlistBundles.get(this.primaryMediaPlaylistUrl);
            HlsMediaPlaylist newPrimarySnapshot = newPrimaryBundle.playlistSnapshot;
            if (newPrimarySnapshot != null && newPrimarySnapshot.hasEndTag) {
                this.primaryMediaPlaylistSnapshot = newPrimarySnapshot;
                this.primaryPlaylistListener.onPrimaryPlaylistRefreshed(newPrimarySnapshot);
            } else {
                newPrimaryBundle.loadPlaylistInternal(this.getRequestUriForPrimaryChange(url));
            }

        }
    }

    private Uri getRequestUriForPrimaryChange(Uri newPrimaryPlaylistUri) {
        if (this.primaryMediaPlaylistSnapshot != null && this.primaryMediaPlaylistSnapshot.serverControl.canBlockReload) {
            RenditionReport renditionReport = (RenditionReport) this.primaryMediaPlaylistSnapshot.renditionReports.get(newPrimaryPlaylistUri);
            if (renditionReport != null) {
                Uri.Builder uriBuilder = newPrimaryPlaylistUri.buildUpon();
                uriBuilder.appendQueryParameter("_HLS_msn", String.valueOf(renditionReport.lastMediaSequence));
                if (renditionReport.lastPartIndex != -1) {
                    uriBuilder.appendQueryParameter("_HLS_part", String.valueOf(renditionReport.lastPartIndex));
                }

                return uriBuilder.build();
            }
        }

        return newPrimaryPlaylistUri;
    }

    private boolean isVariantUrl(Uri playlistUrl) {
        List<Variant> variants = this.masterPlaylist.variants;

        for (int i = 0; i < variants.size(); ++i) {
            if (playlistUrl.equals(((Variant) variants.get(i)).url)) {
                return true;
            }
        }

        return false;
    }

    private void createBundles(List<Uri> urls) {
        int listSize = urls.size();

        for (int i = 0; i < listSize; ++i) {
            Uri url = (Uri) urls.get(i);
            MediaPlaylistBundle bundle = new MediaPlaylistBundle(url);
            this.playlistBundles.put(url, bundle);
        }

    }

    private void onPlaylistUpdated(Uri url, HlsMediaPlaylist newSnapshot) {
        if (url.equals(this.primaryMediaPlaylistUrl)) {
            if (this.primaryMediaPlaylistSnapshot == null) {
                this.isLive = !newSnapshot.hasEndTag;
                this.initialStartTimeUs = newSnapshot.startTimeUs;
            }

            this.primaryMediaPlaylistSnapshot = newSnapshot;
            this.primaryPlaylistListener.onPrimaryPlaylistRefreshed(newSnapshot);
        }

        Iterator var3 = this.listeners.iterator();

        while (var3.hasNext()) {
            PlaylistEventListener listener = (PlaylistEventListener) var3.next();
            listener.onPlaylistChanged();
        }

    }

    private boolean notifyPlaylistError(Uri playlistUrl, LoadErrorInfo loadErrorInfo, boolean forceRetry) {
        boolean anyExclusionFailed = false;

        PlaylistEventListener listener;
        for (Iterator var5 = this.listeners.iterator(); var5.hasNext(); anyExclusionFailed |= !listener.onPlaylistError(playlistUrl, loadErrorInfo, forceRetry)) {
            listener = (PlaylistEventListener) var5.next();
        }

        return anyExclusionFailed;
    }

    private HlsMediaPlaylist getLatestPlaylistSnapshot(@Nullable HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        if (!loadedPlaylist.isNewerThan(oldPlaylist)) {
            return loadedPlaylist.hasEndTag ? oldPlaylist.copyWithEndTag() : oldPlaylist;
        } else {
            long startTimeUs = this.getLoadedPlaylistStartTimeUs(oldPlaylist, loadedPlaylist);
            int discontinuitySequence = this.getLoadedPlaylistDiscontinuitySequence(oldPlaylist, loadedPlaylist);
            return loadedPlaylist.copyWith(startTimeUs, discontinuitySequence);
        }
    }

    private long getLoadedPlaylistStartTimeUs(@Nullable HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        if (loadedPlaylist.hasProgramDateTime) {
            return loadedPlaylist.startTimeUs;
        } else {
            long primarySnapshotStartTimeUs = this.primaryMediaPlaylistSnapshot != null ? this.primaryMediaPlaylistSnapshot.startTimeUs : 0L;
            if (oldPlaylist == null) {
                return primarySnapshotStartTimeUs;
            } else {
                int oldPlaylistSize = oldPlaylist.segments.size();
                Segment firstOldOverlappingSegment = getFirstOldOverlappingSegment(oldPlaylist, loadedPlaylist);
                if (firstOldOverlappingSegment != null) {
                    return oldPlaylist.startTimeUs + firstOldOverlappingSegment.relativeStartTimeUs;
                } else {
                    return (long) oldPlaylistSize == loadedPlaylist.mediaSequence - oldPlaylist.mediaSequence ? oldPlaylist.getEndTimeUs() : primarySnapshotStartTimeUs;
                }
            }
        }
    }

    private int getLoadedPlaylistDiscontinuitySequence(@Nullable HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        if (loadedPlaylist.hasDiscontinuitySequence) {
            return loadedPlaylist.discontinuitySequence;
        } else {
            int primaryUrlDiscontinuitySequence = this.primaryMediaPlaylistSnapshot != null ? this.primaryMediaPlaylistSnapshot.discontinuitySequence : 0;
            if (oldPlaylist == null) {
                return primaryUrlDiscontinuitySequence;
            } else {
                Segment firstOldOverlappingSegment = getFirstOldOverlappingSegment(oldPlaylist, loadedPlaylist);
                return firstOldOverlappingSegment != null ? oldPlaylist.discontinuitySequence + firstOldOverlappingSegment.relativeDiscontinuitySequence - ((Segment) loadedPlaylist.segments.get(0)).relativeDiscontinuitySequence : primaryUrlDiscontinuitySequence;
            }
        }
    }

    private static Segment getFirstOldOverlappingSegment(HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
        int mediaSequenceOffset = (int) (loadedPlaylist.mediaSequence - oldPlaylist.mediaSequence);
        List<Segment> oldSegments = oldPlaylist.segments;
        return mediaSequenceOffset < oldSegments.size() ? (Segment) oldSegments.get(mediaSequenceOffset) : null;
    }

    private class FirstPrimaryMediaPlaylistListener implements PlaylistEventListener {
        private FirstPrimaryMediaPlaylistListener() {
        }

        public void onPlaylistChanged() {
            MyHlsPlaylistTracker.this.listeners.remove(this);
        }

        public boolean onPlaylistError(Uri url, LoadErrorInfo loadErrorInfo, boolean forceRetry) {
            if (MyHlsPlaylistTracker.this.primaryMediaPlaylistSnapshot == null) {
                long nowMs = SystemClock.elapsedRealtime();
                int variantExclusionCounter = 0;
                List<Variant> variants = ((HlsMasterPlaylist) Util.castNonNull(MyHlsPlaylistTracker.this.masterPlaylist)).variants;

                for (int i = 0; i < variants.size(); ++i) {
                    MediaPlaylistBundle mediaPlaylistBundle = (MediaPlaylistBundle) MyHlsPlaylistTracker.this.playlistBundles.get(((Variant) variants.get(i)).url);
                    if (mediaPlaylistBundle != null && nowMs < mediaPlaylistBundle.excludeUntilMs) {
                        ++variantExclusionCounter;
                    }
                }

                FallbackOptions fallbackOptions = new FallbackOptions(1, 0, MyHlsPlaylistTracker.this.masterPlaylist.variants.size(), variantExclusionCounter);
                FallbackSelection fallbackSelection = MyHlsPlaylistTracker.this.loadErrorHandlingPolicy.getFallbackSelectionFor(fallbackOptions, loadErrorInfo);
                if (fallbackSelection != null && fallbackSelection.type == 2) {
                    MediaPlaylistBundle mediaPlaylistBundlex = (MediaPlaylistBundle) MyHlsPlaylistTracker.this.playlistBundles.get(url);
                    if (mediaPlaylistBundlex != null) {
                        mediaPlaylistBundlex.excludePlaylist(fallbackSelection.exclusionDurationMs);
                    }
                }
            }

            return false;
        }
    }

    private final class MediaPlaylistBundle implements Loader.Callback<ParsingLoadable<HlsPlaylist>> {
        private static final String BLOCK_MSN_PARAM = "_HLS_msn";
        private static final String BLOCK_PART_PARAM = "_HLS_part";
        private static final String SKIP_PARAM = "_HLS_skip";
        private final Uri playlistUrl;
        private final Loader mediaPlaylistLoader;
        private final DataSource mediaPlaylistDataSource;
        @Nullable
        private HlsMediaPlaylist playlistSnapshot;
        private long lastSnapshotLoadMs;
        private long lastSnapshotChangeMs;
        private long earliestNextLoadTimeMs;
        private long excludeUntilMs;
        private boolean loadPending;
        @Nullable
        private IOException playlistError;

        public MediaPlaylistBundle(Uri playlistUrl) {
            this.playlistUrl = playlistUrl;
            this.mediaPlaylistLoader = new Loader("MyHlsPlaylistTracker:MediaPlaylist");
            this.mediaPlaylistDataSource = MyHlsPlaylistTracker.this.dataSourceFactory.createDataSource(4);
        }

        @Nullable
        public HlsMediaPlaylist getPlaylistSnapshot() {
            return this.playlistSnapshot;
        }

        public boolean isSnapshotValid() {
            if (this.playlistSnapshot == null) {
                return false;
            } else {
                long currentTimeMs = SystemClock.elapsedRealtime();
                long snapshotValidityDurationMs = Math.max(30000L, C.usToMs(this.playlistSnapshot.durationUs));
                return this.playlistSnapshot.hasEndTag || this.playlistSnapshot.playlistType == 2 || this.playlistSnapshot.playlistType == 1 || this.lastSnapshotLoadMs + snapshotValidityDurationMs > currentTimeMs;
            }
        }

        public void loadPlaylist() {
            this.loadPlaylistInternal(this.playlistUrl);
        }

        public void maybeThrowPlaylistRefreshError() throws IOException {
            if (!isLive) {
                Log.e(TAG, "maybeThrowPlaylistRefreshError: ");
                return;
            }
            this.mediaPlaylistLoader.maybeThrowError();
            if (this.playlistError != null) {
                throw this.playlistError;
            }
        }

        public void release() {
            this.mediaPlaylistLoader.release();
        }

        public void onLoadCompleted(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs) {
            HlsPlaylist result = (HlsPlaylist) loadable.getResult();
            LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
            if (result instanceof HlsMediaPlaylist) {
                this.processLoadedPlaylist((HlsMediaPlaylist) result, loadEventInfo);
                MyHlsPlaylistTracker.this.eventDispatcher.loadCompleted(loadEventInfo, 4);
            } else {
                this.playlistError = ParserException.createForMalformedManifest("Loaded playlist has unexpected type.", (Throwable) null);
                MyHlsPlaylistTracker.this.eventDispatcher.loadError(loadEventInfo, 4, this.playlistError, true);
            }

            MyHlsPlaylistTracker.this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }

        public void onLoadCanceled(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
            MyHlsPlaylistTracker.this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
            MyHlsPlaylistTracker.this.eventDispatcher.loadCanceled(loadEventInfo, 4);
        }

        public LoadErrorAction onLoadError(ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(loadable.loadTaskId, loadable.dataSpec, loadable.getUri(), loadable.getResponseHeaders(), elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
            boolean isBlockingRequest = loadable.getUri().getQueryParameter("_HLS_msn") != null;
            boolean deltaUpdateFailed = error instanceof DeltaUpdateException;
            if (isBlockingRequest || deltaUpdateFailed) {
                int responseCode = 2147483647;
                if (error instanceof InvalidResponseCodeException) {
                    responseCode = ((InvalidResponseCodeException) error).responseCode;
                }

                if (deltaUpdateFailed || responseCode == 400 || responseCode == 503) {
                    this.earliestNextLoadTimeMs = SystemClock.elapsedRealtime();
                    this.loadPlaylist();
                    ((MediaSourceEventListener.EventDispatcher) Util.castNonNull(MyHlsPlaylistTracker.this.eventDispatcher)).loadError(loadEventInfo, loadable.type, error, true);
                    return Loader.DONT_RETRY;
                }
            }

            MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
            LoadErrorInfo loadErrorInfo = new LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount);
            boolean exclusionFailed = MyHlsPlaylistTracker.this.notifyPlaylistError(this.playlistUrl, loadErrorInfo, false);
            LoadErrorAction loadErrorAction;
            if (exclusionFailed) {
                long retryDelay = MyHlsPlaylistTracker.this.loadErrorHandlingPolicy.getRetryDelayMsFor(loadErrorInfo);
                loadErrorAction = retryDelay != -9223372036854775807L ? Loader.createRetryAction(false, retryDelay) : Loader.DONT_RETRY_FATAL;
            } else {
                loadErrorAction = Loader.DONT_RETRY;
            }

            boolean wasCanceled = !loadErrorAction.isRetry();
            MyHlsPlaylistTracker.this.eventDispatcher.loadError(loadEventInfo, loadable.type, error, wasCanceled);
            if (wasCanceled) {
                MyHlsPlaylistTracker.this.loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
            }

            return loadErrorAction;
        }

        private void loadPlaylistInternal(final Uri playlistRequestUri) {
            this.excludeUntilMs = 0L;
            if (!this.loadPending && !this.mediaPlaylistLoader.isLoading() && !this.mediaPlaylistLoader.hasFatalError()) {
                long currentTimeMs = SystemClock.elapsedRealtime();
                if (currentTimeMs < this.earliestNextLoadTimeMs) {
                    this.loadPending = true;
                    MyHlsPlaylistTracker.this.playlistRefreshHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MediaPlaylistBundle.this.loadPending = false;
                            MediaPlaylistBundle.this.loadPlaylistImmediately(playlistRequestUri);
                        }
                    }, this.earliestNextLoadTimeMs - currentTimeMs);
                } else {
                    this.loadPlaylistImmediately(playlistRequestUri);
                }

            }
        }

        private void loadPlaylistImmediately(Uri playlistRequestUri) {
            Parser<HlsPlaylist> mediaPlaylistParser = MyHlsPlaylistTracker.this.playlistParserFactory.createPlaylistParser(MyHlsPlaylistTracker.this.masterPlaylist, this.playlistSnapshot);
            ParsingLoadable<HlsPlaylist> mediaPlaylistLoadable = new ParsingLoadable(this.mediaPlaylistDataSource, playlistRequestUri, 4, mediaPlaylistParser);
            long elapsedRealtime = this.mediaPlaylistLoader.startLoading(mediaPlaylistLoadable, this, MyHlsPlaylistTracker.this.loadErrorHandlingPolicy.getMinimumLoadableRetryCount(mediaPlaylistLoadable.type));
            MyHlsPlaylistTracker.this.eventDispatcher.loadStarted(new LoadEventInfo(mediaPlaylistLoadable.loadTaskId, mediaPlaylistLoadable.dataSpec, elapsedRealtime), mediaPlaylistLoadable.type);
        }

        private void processLoadedPlaylist(HlsMediaPlaylist loadedPlaylist, LoadEventInfo loadEventInfo) {
            HlsMediaPlaylist oldPlaylist = this.playlistSnapshot;
            long currentTimeMs = SystemClock.elapsedRealtime();
            this.lastSnapshotLoadMs = currentTimeMs;
            this.playlistSnapshot = MyHlsPlaylistTracker.this.getLatestPlaylistSnapshot(oldPlaylist, loadedPlaylist);
            if (this.playlistSnapshot != oldPlaylist) {
                this.playlistError = null;
                this.lastSnapshotChangeMs = currentTimeMs;
                MyHlsPlaylistTracker.this.onPlaylistUpdated(this.playlistUrl, this.playlistSnapshot);
            } else if (!this.playlistSnapshot.hasEndTag) {
                boolean forceRetry = false;
                IOException playlistError = null;
                if (loadedPlaylist.mediaSequence + (long) loadedPlaylist.segments.size() < this.playlistSnapshot.mediaSequence) {
                    forceRetry = true;
                    playlistError = new PlaylistResetException(this.playlistUrl);
                } else if ((double) (currentTimeMs - this.lastSnapshotChangeMs) > (double) C.usToMs(this.playlistSnapshot.targetDurationUs) * MyHlsPlaylistTracker.this.playlistStuckTargetDurationCoefficient) {
                    playlistError = new PlaylistStuckException(this.playlistUrl);
                }

                if (playlistError != null) {
                    this.playlistError = (IOException) playlistError;
                    MyHlsPlaylistTracker.this.notifyPlaylistError(this.playlistUrl, new LoadErrorInfo(loadEventInfo, new MediaLoadData(4), (IOException) playlistError, 1), forceRetry);
                }
            }

            long durationUntilNextLoadUs = 0L;
            if (!this.playlistSnapshot.serverControl.canBlockReload) {
                durationUntilNextLoadUs = this.playlistSnapshot != oldPlaylist ? this.playlistSnapshot.targetDurationUs : this.playlistSnapshot.targetDurationUs / 2L;
            }

            this.earliestNextLoadTimeMs = currentTimeMs + C.usToMs(durationUntilNextLoadUs);
            boolean scheduleLoad = this.playlistSnapshot.partTargetDurationUs != -9223372036854775807L || this.playlistUrl.equals(MyHlsPlaylistTracker.this.primaryMediaPlaylistUrl);
            if (scheduleLoad && !this.playlistSnapshot.hasEndTag) {
                this.loadPlaylistInternal(this.getMediaPlaylistUriForReload());
            }

        }

        private Uri getMediaPlaylistUriForReload() {
            if (this.playlistSnapshot != null && (this.playlistSnapshot.serverControl.skipUntilUs != -9223372036854775807L || this.playlistSnapshot.serverControl.canBlockReload)) {
                Uri.Builder uriBuilder = this.playlistUrl.buildUpon();
                if (this.playlistSnapshot.serverControl.canBlockReload) {
                    long targetMediaSequence = this.playlistSnapshot.mediaSequence + (long) this.playlistSnapshot.segments.size();
                    uriBuilder.appendQueryParameter("_HLS_msn", String.valueOf(targetMediaSequence));
                    if (this.playlistSnapshot.partTargetDurationUs != -9223372036854775807L) {
                        List<Part> trailingParts = this.playlistSnapshot.trailingParts;
                        int targetPartIndex = trailingParts.size();
                        if (!trailingParts.isEmpty() && ((Part) Iterables.getLast(trailingParts)).isPreload) {
                            --targetPartIndex;
                        }

                        uriBuilder.appendQueryParameter("_HLS_part", String.valueOf(targetPartIndex));
                    }
                }

                if (this.playlistSnapshot.serverControl.skipUntilUs != -9223372036854775807L) {
                    uriBuilder.appendQueryParameter("_HLS_skip", this.playlistSnapshot.serverControl.canSkipDateRanges ? "v2" : "YES");
                }

                return uriBuilder.build();
            } else {
                return this.playlistUrl;
            }
        }

        private boolean excludePlaylist(long exclusionDurationMs) {
            this.excludeUntilMs = SystemClock.elapsedRealtime() + exclusionDurationMs;
            return this.playlistUrl.equals(MyHlsPlaylistTracker.this.primaryMediaPlaylistUrl) && !MyHlsPlaylistTracker.this.maybeSelectNewPrimaryUrl();
        }
    }
}
