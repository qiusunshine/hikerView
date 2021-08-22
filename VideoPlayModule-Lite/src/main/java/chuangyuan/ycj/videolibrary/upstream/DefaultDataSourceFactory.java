package chuangyuan.ycj.videolibrary.upstream;

/**
 * 作者：By 15968
 * 日期：On 2021/8/13
 * 时间：At 21:06
 */

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.TransferListener;

public final class DefaultDataSourceFactory implements Factory {
    private final Context context;
    private final TransferListener<? super DataSource> listener;
    private final Factory baseDataSourceFactory;

    public DefaultDataSourceFactory(Context context, String userAgent) {
        this(context, (String)userAgent, (TransferListener)null);
    }

    public DefaultDataSourceFactory(Context context, String userAgent, TransferListener<? super DataSource> listener) {
        this(context, (TransferListener)listener, (Factory)(new DefaultHttpDataSourceFactory(userAgent, listener)));
    }

    public DefaultDataSourceFactory(Context context, TransferListener<? super DataSource> listener, Factory baseDataSourceFactory) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.baseDataSourceFactory = baseDataSourceFactory;
    }

    public DefaultDataSource createDataSource() {
        return new DefaultDataSource(this.context, this.listener, this.baseDataSourceFactory.createDataSource());
    }
}