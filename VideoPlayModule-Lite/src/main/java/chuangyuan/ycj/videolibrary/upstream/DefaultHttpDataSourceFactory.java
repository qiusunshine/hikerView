package chuangyuan.ycj.videolibrary.upstream;

/**
 * 作者：By 15968
 * 日期：On 2021/8/13
 * 时间：At 21:10
 */

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource.BaseFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource.RequestProperties;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Predicate;

public final class DefaultHttpDataSourceFactory extends BaseFactory {
    private final String userAgent;
    private final TransferListener<? super DataSource> listener;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean allowCrossProtocolRedirects;

    public DefaultHttpDataSourceFactory(String userAgent) {
        this(userAgent, (TransferListener)null);
    }

    public DefaultHttpDataSourceFactory(String userAgent, TransferListener<? super DataSource> listener) {
        this(userAgent, listener, 8000, 8000, false);
    }

    public DefaultHttpDataSourceFactory(String userAgent, TransferListener<? super DataSource> listener, int connectTimeoutMillis, int readTimeoutMillis, boolean allowCrossProtocolRedirects) {
        this.userAgent = userAgent;
        this.listener = listener;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
    }

    protected DefaultHttpDataSource createDataSourceInternal(RequestProperties defaultRequestProperties) {
        return new DefaultHttpDataSource(this.userAgent, (Predicate)null, this.listener, this.connectTimeoutMillis, this.readTimeoutMillis, this.allowCrossProtocolRedirects, defaultRequestProperties);
    }
}