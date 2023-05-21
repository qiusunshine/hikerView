package com.jeffmony.videocache.utils;


import com.jeffmony.videocache.okhttp.CustomTrustManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author jeffmony
 *
 * HttpURLConnection 通用类
 */

public class HttpUtils {

    private static final String TAG = "HttpUtils";
    public static final int MAX_RETRY_COUNT = 100;
    public static final int MAX_REDIRECT = 5;
    public static final int RESPONSE_200 = 200;
    public static final int RESPONSE_206 = 206;
    public static final int RESPONSE_503 = 503;

    public static HttpURLConnection getConnection(String videoUrl, Map<String, String> headers) throws IOException {
        return getConnection(videoUrl, headers, false);
    }

    private static HttpURLConnection getConnection(String videoUrl, Map<String, String> headers, boolean shouldIgnoreCertErrors) throws IOException {
        URL url = new URL(videoUrl);
        int redirectCount = 0;
        while (redirectCount < MAX_REDIRECT) {
            try {
                HttpURLConnection connection = makeConnection(url, headers, true);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MULT_CHOICE ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    url = handleRedirect(url, location);
                    redirectCount++;
                } else {
                    return connection;
                }
            } catch (IOException e) {
                if ((e instanceof SSLHandshakeException || e instanceof SSLPeerUnverifiedException) && !shouldIgnoreCertErrors) {
                    //这种情况下需要信任证书重试
                    return getConnection(videoUrl, headers, true);
                } else {
                    throw e;
                }
            }
        }
        throw new NoRouteToHostException("Too many redirects: " + redirectCount);
    }

    public static URL handleRedirect(URL originalUrl, String location)
            throws IOException {
        if (location == null) {
            throw new ProtocolException("Null location redirect");
        }
        URL url = new URL(originalUrl, location);
        String protocol = url.getProtocol();
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new ProtocolException("Unsupported protocol redirect: " + protocol);
        }
        return url;
    }

    public static X509TrustManager UnSafeTrustManager = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
    public static SSLParams getSslSocketFactory() {
        return getSslSocketFactoryBase((X509TrustManager)null, (InputStream)null, (String)null);
    }
    private static HttpURLConnection makeConnection(URL url, Map<String, String> headers, boolean shouldIgnoreCertErrors) throws IOException {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{getSslSocketFactory().trustManager}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(getSslSocketFactory().sSLSocketFactory);
        } catch (Exception e) {
            // should never happen
            e.printStackTrace();
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);   //因为我们内部已经做了重定向的功能,不需要在connection内部再做了.
        connection.setConnectTimeout(ProxyCacheUtils.getConfig().getConnTimeOut());
        connection.setReadTimeout(ProxyCacheUtils.getConfig().getReadTimeOut());
//        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestMethod("GET");
        if (headers != null) {
            for (Map.Entry<String, String> item : headers.entrySet()) {
                connection.setRequestProperty(item.getKey(), item.getValue());
            }
        }
        connection.connect();
        return connection;
    }

    public static void trustAllCert(HttpsURLConnection httpsURLConnection) {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            if (sslContext != null) {
                sslContext.init(null, new TrustManager[]{new CustomTrustManager()}, null);
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"SSLContext init failed");
        }
        // Cannot do ssl checkl.
        if (sslContext == null) {
            return;
        }
        // Trust the cert.
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        httpsURLConnection.setHostnameVerifier(hostnameVerifier);
        httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
    }

    public static void closeConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    private static SSLParams getSslSocketFactoryBase(X509TrustManager trustManager, InputStream bksFile, String password, InputStream... certificates) {
        SSLParams sslParams = new SSLParams();

        try {
            KeyManager[] keyManagers = prepareKeyManager(bksFile, password);
            TrustManager[] trustManagers = prepareTrustManager(certificates);
            X509TrustManager manager;
            if (trustManager != null) {
                manager = trustManager;
            } else if (trustManagers != null) {
                manager = chooseTrustManager(trustManagers);
            } else {
                manager = UnSafeTrustManager;
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, new TrustManager[]{manager}, (SecureRandom)null);
            sslParams.sSLSocketFactory = sslContext.getSocketFactory();
            sslParams.trustManager = manager;
            return sslParams;
        } catch (NoSuchAlgorithmException var9) {
            throw new AssertionError(var9);
        } catch (KeyManagementException var10) {
            throw new AssertionError(var10);
        }
    }

    private static KeyManager[] prepareKeyManager(InputStream bksFile, String password) {
        try {
            if (bksFile != null && password != null) {
                KeyStore clientKeyStore = KeyStore.getInstance("BKS");
                clientKeyStore.load(bksFile, password.toCharArray());
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(clientKeyStore, password.toCharArray());
                return kmf.getKeyManagers();
            } else {
                return null;
            }
        } catch (Exception var4) {
            var4.printStackTrace();
            return null;
        }
    }

    private static TrustManager[] prepareTrustManager(InputStream... certificates) {
        if (certificates != null && certificates.length > 0) {
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load((KeyStore.LoadStoreParameter)null);
                int index = 0;
                InputStream[] var4 = certificates;
                int var5 = certificates.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    InputStream certStream = var4[var6];
                    String certificateAlias = Integer.toString(index++);
                    Certificate cert = certificateFactory.generateCertificate(certStream);
                    keyStore.setCertificateEntry(certificateAlias, cert);

                    try {
                        if (certStream != null) {
                            certStream.close();
                        }
                    } catch (IOException var11) {
                        var11.printStackTrace();
                    }
                }

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                return tmf.getTrustManagers();
            } catch (Exception var12) {
                var12.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private static X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        TrustManager[] var1 = trustManagers;
        int var2 = trustManagers.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            TrustManager trustManager = var1[var3];
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager)trustManager;
            }
        }

        return null;
    }

    public static class SSLParams {
        public SSLSocketFactory sSLSocketFactory;
        public X509TrustManager trustManager;

        public SSLParams() {
        }
    }

    public static HostnameVerifier UnSafeHostnameVerifier = (hostname, session) -> true;
}
