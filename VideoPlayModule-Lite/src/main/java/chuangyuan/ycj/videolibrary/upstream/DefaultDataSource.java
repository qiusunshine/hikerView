package chuangyuan.ycj.videolibrary.upstream;

/**
 * 作者：By 15968
 * 日期：On 2021/8/13
 * 时间：At 21:07
 */

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource.RequestProperties;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Predicate;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

public final class DefaultDataSource implements DataSource {
    private static final String TAG = "DefaultDataSource";
    private static final String SCHEME_ASSET = "asset";
    private static final String SCHEME_CONTENT = "content";
    private static final String SCHEME_RTMP = "rtmp";
    private static final String SCHEME_RAW = "rawresource";
    private final Context context;
    private final TransferListener<? super DataSource> listener;
    private final DataSource baseDataSource;
    private DataSource fileDataSource;
    private DataSource assetDataSource;
    private DataSource contentDataSource;
    private DataSource rtmpDataSource;
    private DataSource dataSchemeDataSource;
    private DataSource rawResourceDataSource;
    private DataSource dataSource;

    public DefaultDataSource(Context context, TransferListener<? super DataSource> listener, String userAgent, boolean allowCrossProtocolRedirects) {
        this(context, listener, userAgent, 8000, 8000, allowCrossProtocolRedirects);
    }

    public DefaultDataSource(Context context, TransferListener<? super DataSource> listener, String userAgent, int connectTimeoutMillis, int readTimeoutMillis, boolean allowCrossProtocolRedirects) {
        this(context, listener, new DefaultHttpDataSource(userAgent, (Predicate)null, listener, connectTimeoutMillis, readTimeoutMillis, allowCrossProtocolRedirects, (RequestProperties)null));
    }

    public DefaultDataSource(Context context, TransferListener<? super DataSource> listener, DataSource baseDataSource) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.baseDataSource = (DataSource)Assertions.checkNotNull(baseDataSource);
    }

    public long open(DataSpec dataSpec) throws IOException {
        Assertions.checkState(this.dataSource == null);
        String scheme = dataSpec.uri.getScheme();
        if (Util.isLocalFileUri(dataSpec.uri)) {
            if (dataSpec.uri.getPath().startsWith("/android_asset/")) {
                this.dataSource = this.getAssetDataSource();
            } else {
                this.dataSource = this.getFileDataSource();
            }
        } else if ("asset".equals(scheme)) {
            this.dataSource = this.getAssetDataSource();
        } else if ("content".equals(scheme)) {
            this.dataSource = this.getContentDataSource();
        } else if ("rtmp".equals(scheme)) {
            this.dataSource = this.getRtmpDataSource();
        } else if ("data".equals(scheme)) {
            this.dataSource = this.getDataSchemeDataSource();
        } else if ("rawresource".equals(scheme)) {
            this.dataSource = this.getRawResourceDataSource();
        } else {
            this.dataSource = this.baseDataSource;
        }

        return this.dataSource.open(dataSpec);
    }

    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return this.dataSource.read(buffer, offset, readLength);
    }

    public Uri getUri() {
        return this.dataSource == null ? null : this.dataSource.getUri();
    }

    public void close() throws IOException {
        if (this.dataSource != null) {
            try {
                this.dataSource.close();
            } finally {
                this.dataSource = null;
            }
        }

    }

    private DataSource getFileDataSource() {
        if (this.fileDataSource == null) {
            this.fileDataSource = new FileDataSource(this.listener);
        }

        return this.fileDataSource;
    }

    private DataSource getAssetDataSource() {
        if (this.assetDataSource == null) {
            this.assetDataSource = new AssetDataSource(this.context, this.listener);
        }

        return this.assetDataSource;
    }

    private DataSource getContentDataSource() {
        if (this.contentDataSource == null) {
            this.contentDataSource = new ContentDataSource(this.context, this.listener);
        }

        return this.contentDataSource;
    }

    private DataSource getRtmpDataSource() {
        if (this.rtmpDataSource == null) {
            try {
                Class<?> clazz = Class.forName("com.google.android.exoplayer2.ext.rtmp.RtmpDataSource");
                this.rtmpDataSource = (DataSource)clazz.getConstructor().newInstance();
            } catch (ClassNotFoundException var2) {
                Log.w("DefaultDataSource", "Attempting to play RTMP stream without depending on the RTMP extension");
            } catch (Exception var3) {
                throw new RuntimeException("Error instantiating RTMP extension", var3);
            }

            if (this.rtmpDataSource == null) {
                this.rtmpDataSource = this.baseDataSource;
            }
        }

        return this.rtmpDataSource;
    }

    private DataSource getDataSchemeDataSource() {
        if (this.dataSchemeDataSource == null) {
            this.dataSchemeDataSource = new DataSchemeDataSource();
        }

        return this.dataSchemeDataSource;
    }

    private DataSource getRawResourceDataSource() {
        if (this.rawResourceDataSource == null) {
            this.rawResourceDataSource = new RawResourceDataSource(this.context, this.listener);
        }

        return this.rawResourceDataSource;
    }
}
