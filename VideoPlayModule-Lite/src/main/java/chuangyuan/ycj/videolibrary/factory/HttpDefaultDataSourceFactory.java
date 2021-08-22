package chuangyuan.ycj.videolibrary.factory;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.util.Map;

import chuangyuan.ycj.videolibrary.upstream.DefaultDataSource;
import chuangyuan.ycj.videolibrary.upstream.DefaultDataSourceFactory;
import chuangyuan.ycj.videolibrary.upstream.DefaultHttpDataSource;
import chuangyuan.ycj.videolibrary.upstream.DefaultHttpDataSourceFactory;

/**
 * 作者：By 15968
 * 日期：On 2020/6/1
 * 时间：At 23:08
 */
public class HttpDefaultDataSourceFactory implements DataSource.Factory {

    private final Context context;
    private final DataSource.Factory baseDataSourceFactory;

    private final static String DEFAULT_UA = "Mozilla/5.0 (Linux; Android 11; Mi 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.152 Mobile Safari/537.36";

    /**
     * Instantiates a new J default data source factory.
     *
     * @param context A context.                for {@link DefaultDataSource}.
     * @see DefaultDataSource#DefaultDataSource(Context, TransferListener, DataSource) DefaultDataSource#DefaultDataSource(Context, TransferListener, DataSource)
     */
    public HttpDefaultDataSourceFactory(Context context) {
        String userAgent = Util.getUserAgent(context, context.getPackageName());
        this.context = context.getApplicationContext();
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);
        this.baseDataSourceFactory = new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
    }

    public HttpDefaultDataSourceFactory(Context context, Map<String, String> headers) {
//        String userAgent = Util.getUserAgent(context, context.getPackageName());
        String userAgent = DEFAULT_UA;
        this.context = context.getApplicationContext();
        if (headers != null) {
            if (headers.containsKey("User-Agent")) {
                userAgent = headers.get("User-Agent");
            } else if (headers.containsKey("user-agent")) {
                userAgent = headers.get("user-agent");
            } else if (headers.containsKey("user-Agent")) {
                userAgent = headers.get("user-Agent");
            }
        }
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);
        if (headers != null && !headers.isEmpty()) {
            for (String s : headers.keySet()) {
                httpDataSourceFactory.getDefaultRequestProperties().set(s, headers.get(s));
            }
        }
        this.baseDataSourceFactory = new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
    }

    @Override
    public DataSource createDataSource() {
        return new DefaultDataSource(context, new DefaultBandwidthMeter(), baseDataSourceFactory.createDataSource());
    }
}
