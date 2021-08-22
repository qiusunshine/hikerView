package chuangyuan.ycj.videolibrary.upstream;

/**
 * 作者：By 15968
 * 日期：On 2021/8/13
 * 时间：At 21:11
 */

import android.annotation.SuppressLint;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSourceException;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Predicate;
import com.google.android.exoplayer2.util.Util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class DefaultHttpDataSource implements HttpDataSource {
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 8000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 8000;
    private static final String TAG = "DefaultHttpDataSource";
    private static final int MAX_REDIRECTS = 20;
    private static final long MAX_BYTES_TO_DRAIN = 2048L;
    private static final Pattern CONTENT_RANGE_HEADER = Pattern.compile("^bytes (\\d+)-(\\d+)/(\\d+)$");
    private static final AtomicReference<byte[]> skipBufferReference = new AtomicReference();
    private final boolean allowCrossProtocolRedirects;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String userAgent;
    private final Predicate<String> contentTypePredicate;
    private final RequestProperties defaultRequestProperties;
    private final RequestProperties requestProperties;
    private final TransferListener<? super DefaultHttpDataSource> listener;
    private DataSpec dataSpec;
    private HttpURLConnection connection;
    private InputStream inputStream;
    private boolean opened;
    private long bytesToSkip;
    private long bytesToRead;
    private long bytesSkipped;
    private long bytesRead;
    private HttpsUtils.SSLParams sslParams;

    public DefaultHttpDataSource(String userAgent, Predicate<String> contentTypePredicate) {
        this(userAgent, contentTypePredicate, (TransferListener) null);
    }

    public DefaultHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, TransferListener<? super DefaultHttpDataSource> listener) {
        this(userAgent, contentTypePredicate, listener, 8000, 8000);
    }

    public DefaultHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, TransferListener<? super DefaultHttpDataSource> listener, int connectTimeoutMillis, int readTimeoutMillis) {
        this(userAgent, contentTypePredicate, listener, connectTimeoutMillis, readTimeoutMillis, false, (RequestProperties) null);
    }

    public DefaultHttpDataSource(String userAgent, Predicate<String> contentTypePredicate, TransferListener<? super DefaultHttpDataSource> listener, int connectTimeoutMillis, int readTimeoutMillis, boolean allowCrossProtocolRedirects, RequestProperties defaultRequestProperties) {
        this.userAgent = Assertions.checkNotEmpty(userAgent);
        this.contentTypePredicate = contentTypePredicate;
        this.listener = listener;
        this.requestProperties = new RequestProperties();
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
        this.defaultRequestProperties = defaultRequestProperties;
    }

    public Uri getUri() {
        return this.connection == null ? null : Uri.parse(this.connection.getURL().toString());
    }

    public Map<String, List<String>> getResponseHeaders() {
        return this.connection == null ? null : this.connection.getHeaderFields();
    }

    public void setRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        this.requestProperties.set(name, value);
    }

    public void clearRequestProperty(String name) {
        Assertions.checkNotNull(name);
        this.requestProperties.remove(name);
    }

    public void clearAllRequestProperties() {
        this.requestProperties.clear();
    }

    @SuppressLint("WrongConstant")
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        this.dataSpec = dataSpec;
        this.bytesRead = 0L;
        this.bytesSkipped = 0L;

        try {
            this.connection = this.makeConnection(dataSpec);
        } catch (IOException var8) {
            var8.printStackTrace();
            throw new HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), var8, dataSpec, 1);
        }

        int responseCode;
        try {
            responseCode = this.connection.getResponseCode();
        } catch (IOException var7) {
            this.closeConnectionQuietly();
            throw new HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), var7, dataSpec, 1);
        }

        if (responseCode >= 200 && responseCode <= 299) {
            String contentType = this.connection.getContentType();
            if (this.contentTypePredicate != null && !this.contentTypePredicate.evaluate(contentType)) {
                this.closeConnectionQuietly();
                throw new InvalidContentTypeException(contentType, dataSpec);
            } else {
                this.bytesToSkip = responseCode == 200 && dataSpec.position != 0L ? dataSpec.position : 0L;
                if (!dataSpec.isFlagSet(1)) {
                    if (dataSpec.length != -1L) {
                        this.bytesToRead = dataSpec.length;
                    } else {
                        long contentLength = getContentLength(this.connection);
                        this.bytesToRead = contentLength != -1L ? contentLength - this.bytesToSkip : -1L;
                    }
                } else {
                    this.bytesToRead = dataSpec.length;
                }

                try {
                    this.inputStream = this.connection.getInputStream();
                } catch (IOException var6) {
                    this.closeConnectionQuietly();
                    throw new HttpDataSourceException(var6, dataSpec, 1);
                }

                this.opened = true;
                if (this.listener != null) {
                    this.listener.onTransferStart(this, dataSpec);
                }

                return this.bytesToRead;
            }
        } else {
            Map<String, List<String>> headers = this.connection.getHeaderFields();
            this.closeConnectionQuietly();
            InvalidResponseCodeException exception = new InvalidResponseCodeException(responseCode, headers, dataSpec);
            if (responseCode == 416) {
                exception.initCause(new DataSourceException(0));
            }

            throw exception;
        }
    }

    @SuppressLint("WrongConstant")
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
        try {
            this.skipInternal();
            return this.readInternal(buffer, offset, readLength);
        } catch (IOException var5) {
            throw new HttpDataSourceException(var5, this.dataSpec, 2);
        }
    }

    @SuppressLint("WrongConstant")
    public void close() throws HttpDataSourceException {
        try {
            if (this.inputStream != null) {
                maybeTerminateInputStream(this.connection, this.bytesRemaining());

                try {
                    this.inputStream.close();
                } catch (IOException var5) {
                    throw new HttpDataSourceException(var5, this.dataSpec, 3);
                }
            }
        } finally {
            this.inputStream = null;
            this.closeConnectionQuietly();
            if (this.opened) {
                this.opened = false;
                if (this.listener != null) {
                    this.listener.onTransferEnd(this);
                }
            }

        }

    }

    protected final HttpURLConnection getConnection() {
        return this.connection;
    }

    protected final long bytesSkipped() {
        return this.bytesSkipped;
    }

    protected final long bytesRead() {
        return this.bytesRead;
    }

    protected final long bytesRemaining() {
        return this.bytesToRead == -1L ? this.bytesToRead : this.bytesToRead - this.bytesRead;
    }

    private HttpURLConnection makeConnection(DataSpec dataSpec) throws IOException {
        URL url = new URL(dataSpec.uri.toString());
        byte[] postBody = dataSpec.postBody;
        long position = dataSpec.position;
        long length = dataSpec.length;
        @SuppressLint("WrongConstant") boolean allowGzip = dataSpec.isFlagSet(1);
        if (!this.allowCrossProtocolRedirects) {
            return this.makeConnection(url, postBody, position, length, allowGzip, true);
        } else {
            int redirectCount;
            String location;
            for (redirectCount = 0; redirectCount++ <= 20; url = handleRedirect(url, location)) {
                HttpURLConnection connection = this.makeConnection(url, postBody, position, length, allowGzip, false);
                int responseCode = connection.getResponseCode();
                if (responseCode != 300 && responseCode != 301 && responseCode != 302 && responseCode != 303 && (postBody != null || responseCode != 307 && responseCode != 308)) {
                    return connection;
                }

                postBody = null;
                location = connection.getHeaderField("Location");
                connection.disconnect();
            }

            throw new NoRouteToHostException("Too many redirects: " + redirectCount);
        }
    }

    private HttpURLConnection makeConnection(URL url, byte[] postBody, long position, long length, boolean allowGzip, boolean followRedirects) throws IOException {
        if (sslParams == null) {
            sslParams = HttpsUtils.getSslSocketFactory();
        }
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{sslParams.trustManager}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslParams.sSLSocketFactory);
        } catch (Exception e) {
            // should never happen
            e.printStackTrace();
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(this.connectTimeoutMillis);
        connection.setReadTimeout(this.readTimeoutMillis);
        Iterator var10;
        Entry property;
        if (this.defaultRequestProperties != null) {
            var10 = this.defaultRequestProperties.getSnapshot().entrySet().iterator();

            while (var10.hasNext()) {
                property = (Entry) var10.next();
                connection.setRequestProperty((String) property.getKey(), (String) property.getValue());
            }
        }

        var10 = this.requestProperties.getSnapshot().entrySet().iterator();

        while (var10.hasNext()) {
            property = (Entry) var10.next();
            connection.setRequestProperty((String) property.getKey(), (String) property.getValue());
        }

        if (position != 0L || length != -1L) {
            String rangeRequest = "bytes=" + position + "-";
            if (length != -1L) {
                rangeRequest = rangeRequest + (position + length - 1L);
            }

            connection.setRequestProperty("Range", rangeRequest);
        }

        connection.setRequestProperty("User-Agent", this.userAgent);
        if (!allowGzip) {
            connection.setRequestProperty("Accept-Encoding", "identity");
        }

        connection.setInstanceFollowRedirects(followRedirects);
        connection.setDoOutput(postBody != null);
        if (postBody != null) {
            connection.setRequestMethod("POST");
            if (postBody.length == 0) {
                connection.connect();
            } else {
                connection.setFixedLengthStreamingMode(postBody.length);
                connection.connect();
                OutputStream os = connection.getOutputStream();
                os.write(postBody);
                os.close();
            }
        } else {
            connection.connect();
        }
        return connection;
    }

    private static URL handleRedirect(URL originalUrl, String location) throws IOException {
        if (location == null) {
            throw new ProtocolException("Null location redirect");
        } else {
            URL url = new URL(originalUrl, location);
            String protocol = url.getProtocol();
//            if (!"https".equals(protocol) && !"http".equals(protocol)) {
//                throw new ProtocolException("Unsupported protocol redirect: " + protocol);
//            } else {
            return url;
//            }
        }
    }

    private static long getContentLength(HttpURLConnection connection) {
        long contentLength = -1L;
        String contentLengthHeader = connection.getHeaderField("Content-Length");
        if (!TextUtils.isEmpty(contentLengthHeader)) {
            try {
                contentLength = Long.parseLong(contentLengthHeader);
            } catch (NumberFormatException var9) {
                Log.e("DefaultHttpDataSource", "Unexpected Content-Length [" + contentLengthHeader + "]");
            }
        }

        String contentRangeHeader = connection.getHeaderField("Content-Range");
        if (!TextUtils.isEmpty(contentRangeHeader)) {
            Matcher matcher = CONTENT_RANGE_HEADER.matcher(contentRangeHeader);
            if (matcher.find()) {
                try {
                    long contentLengthFromRange = Long.parseLong(matcher.group(2)) - Long.parseLong(matcher.group(1)) + 1L;
                    if (contentLength < 0L) {
                        contentLength = contentLengthFromRange;
                    } else if (contentLength != contentLengthFromRange) {
                        Log.w("DefaultHttpDataSource", "Inconsistent headers [" + contentLengthHeader + "] [" + contentRangeHeader + "]");
                        contentLength = Math.max(contentLength, contentLengthFromRange);
                    }
                } catch (NumberFormatException var8) {
                    Log.e("DefaultHttpDataSource", "Unexpected Content-Range [" + contentRangeHeader + "]");
                }
            }
        }

        return contentLength;
    }

    private void skipInternal() throws IOException {
        if (this.bytesSkipped != this.bytesToSkip) {
            byte[] skipBuffer = (byte[]) skipBufferReference.getAndSet((byte[]) null);
            if (skipBuffer == null) {
                skipBuffer = new byte[4096];
            }

            while (this.bytesSkipped != this.bytesToSkip) {
                int readLength = (int) Math.min(this.bytesToSkip - this.bytesSkipped, (long) skipBuffer.length);
                int read = this.inputStream.read(skipBuffer, 0, readLength);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException();
                }

                if (read == -1) {
                    throw new EOFException();
                }

                this.bytesSkipped += (long) read;
                if (this.listener != null) {
                    this.listener.onBytesTransferred(this, read);
                }
            }

            skipBufferReference.set(skipBuffer);
        }
    }

    private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        } else {
            if (this.bytesToRead != -1L) {
                long bytesRemaining = this.bytesToRead - this.bytesRead;
                if (bytesRemaining == 0L) {
                    return -1;
                }

                readLength = (int) Math.min((long) readLength, bytesRemaining);
            }

            int read = this.inputStream.read(buffer, offset, readLength);
            if (read == -1) {
                if (this.bytesToRead != -1L) {
                    throw new EOFException();
                } else {
                    return -1;
                }
            } else {
                this.bytesRead += (long) read;
                if (this.listener != null) {
                    this.listener.onBytesTransferred(this, read);
                }

                return read;
            }
        }
    }

    private static void maybeTerminateInputStream(HttpURLConnection connection, long bytesRemaining) {
        if (Util.SDK_INT == 19 || Util.SDK_INT == 20) {
            try {
                InputStream inputStream = connection.getInputStream();
                if (bytesRemaining == -1L) {
                    if (inputStream.read() == -1) {
                        return;
                    }
                } else if (bytesRemaining <= 2048L) {
                    return;
                }

                String className = inputStream.getClass().getName();
                if ("com.android.okhttp.internal.http.HttpTransport$ChunkedInputStream".equals(className) || "com.android.okhttp.internal.http.HttpTransport$FixedLengthInputStream".equals(className)) {
                    Class<?> superclass = inputStream.getClass().getSuperclass();
                    Method unexpectedEndOfInput = superclass.getDeclaredMethod("unexpectedEndOfInput");
                    unexpectedEndOfInput.setAccessible(true);
                    unexpectedEndOfInput.invoke(inputStream);
                }
            } catch (Exception var7) {
            }

        }
    }

    private void closeConnectionQuietly() {
        if (this.connection != null) {
            try {
                this.connection.disconnect();
            } catch (Exception var2) {
                Log.e("DefaultHttpDataSource", "Unexpected error while disconnecting", var2);
            }

            this.connection = null;
        }

    }
}
