package chuangyuan.ycj.videolibrary.factory;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.HashMap;
import java.util.Map;

import chuangyuan.ycj.videolibrary.upstream.DefaultDataSource;
import chuangyuan.ycj.videolibrary.upstream.DefaultDataSourceFactory;
import chuangyuan.ycj.videolibrary.upstream.DefaultHttpDataSource;

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
     */
    public HttpDefaultDataSourceFactory(Context context) {
        String userAgent = Util.getUserAgent(context, context.getPackageName());
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", userAgent);
        this.context = context.getApplicationContext();
        this.baseDataSourceFactory = init(context, headers);
    }

    public HttpDefaultDataSourceFactory(Context context, Map<String, String> headers) {
        this.context = context.getApplicationContext();
        this.baseDataSourceFactory = init(context, headers);
    }

    private DataSource.Factory init(Context context, Map<String, String> headers) {
//        String userAgent = Util.getUserAgent(context, context.getPackageName());
        String userAgent = DEFAULT_UA;
        if (headers != null) {
            if (headers.containsKey("User-Agent")) {
                userAgent = headers.get("User-Agent");
            } else if (headers.containsKey("user-agent")) {
                userAgent = headers.get("user-agent");
            } else if (headers.containsKey("user-Agent")) {
                userAgent = headers.get("user-Agent");
            }
        }
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true);
        if (headers != null) {
            httpDataSourceFactory.setDefaultRequestProperties(headers);
        }
        return new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
    }

    @Override
    public DataSource createDataSource() {
        return baseDataSourceFactory.createDataSource();
    }
}
