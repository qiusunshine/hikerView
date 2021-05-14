package com.example.hikerview.ui.download.util;

/**
 * Created by xm on 15/6/11.
 */


import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpRequestUtil {

    private static final String TAG = "HttpRequestUtil";

    public static final String defaultCharset = "UTF-8";//"GBK"
    public static final int readTimeout = 10000;//10s
    public static final int connectTimeout = 5000;//5s
    public static final int maxRedirects = 4;//最大重定向次数
    private static AtomicBoolean hasCheckingThread = new AtomicBoolean(false);
    private static final List<ConnectionCheckDTO> checkDTOList = Collections.synchronizedList(new ArrayList<>());

    public static Map<String, String> commonHeaders;

    public final static HostnameVerifier DO_NOT_VERIFY = (hostname, session) -> true;

    static {
        commonHeaders = new HashMap<>();
        commonHeaders.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");
    }

    private static void trustAllHosts() {
        final String TAG = "trustAllHosts";
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Log.i(TAG, "checkClientTrusted");
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Log.i(TAG, "checkServerTrusted");
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        HeadRequestResponse headRequestResponse = performHeadRequest("https://disp.titan.mgtv.com/vod.do?fmt=4&pno=1121&fid=3BBD5FD649B8DEB99DBDE005F7304103&file=/c1/2017/08/30_0/3BBD5FD649B8DEB99DBDE005F7304103_20170830_1_1_644.mp4");
        System.out.println(headRequestResponse.getRealUrl());
        System.out.println(JSON.toJSONString(headRequestResponse.getHeaderMap()));
    }

    public static URLConnection sendGetRequest(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        StringBuilder buf = new StringBuilder();
        //当https使用非443端口会导致获取不到端口
        int port = new URL(url.replace("https://", "http://").replace("HTTPS://", "http://")).getPort();
        URL urlObject = new URL(url);
        buf.append(urlObject.getProtocol()).append("://").append(urlObject.getHost()).append((port == -1 || port == urlObject.getDefaultPort()) ? "" : ":" + port).append(urlObject.getPath());
        String query = urlObject.getQuery();
        if (params == null) {
            params = new HashMap<>();
        }
        boolean isQueryExist = false;
        if (!(query == null || query.length() == 0) || params.size() > 0) {
            buf.append("?");
            isQueryExist = true;
        }
        if (!(query == null || query.length() == 0)) {
            buf.append(query);
            buf.append("&");
        }
        Set<Entry<String, String>> entrys = params.entrySet();
        for (Entry<String, String> entry : entrys) {
            buf.append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), defaultCharset)).append("&");
        }
        if (isQueryExist) {
            buf.deleteCharAt(buf.length() - 1);
        }
        System.out.println("before:" + url);
        System.out.println("after:" + buf.toString());
        urlObject = new URL(buf.toString());
        HttpURLConnection conn = null;
        try {
            if (urlObject.getProtocol().toUpperCase().equals("HTTPS")) {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) urlObject.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) urlObject.openConnection();
            }
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            if (headers != null) {
                entrys = headers.entrySet();
                for (Entry<String, String> entry : entrys) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.getResponseCode();
            return conn;
        } catch (IOException e) {
            if (conn != null) {
                conn.disconnect();
            }
            throw e;
        }
    }

    public static URLConnection sendGetRequest(String url) throws IOException {
        return sendGetRequest(url, null, commonHeaders);
    }

    public static URLConnection sendGetRequest(String url,
                                               Map<String, String> params) throws IOException {
        return sendGetRequest(url, params, commonHeaders);
    }

    public static URLConnection sendPostRequest(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        StringBuilder buf = new StringBuilder();
        if (params == null) {
            params = new HashMap<>();
        }
        Set<Entry<String, String>> entrys = params.entrySet();
        for (Entry<String, String> entry : entrys) {
            buf.append("&").append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), defaultCharset));
        }
        buf.deleteCharAt(0);
        URL urlObject = new URL(url);
        HttpURLConnection conn = null;
        try {
            if (urlObject.getProtocol().toUpperCase().equals("HTTPS")) {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) urlObject.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) urlObject.openConnection();
            }
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            if (headers != null) {
                entrys = headers.entrySet();
                for (Entry<String, String> entry : entrys) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            //System.out.println("buf.toString():"+buf.toString());
            out.write(buf.toString().getBytes(defaultCharset));
            out.flush();
            conn.getResponseCode(); // 为了发送成功
            return conn;
        } catch (IOException e) {
            if (conn != null) {
                conn.disconnect();
            }
            throw e;
        }
    }

    public static URLConnection sendPostRequest(String url,
                                                Map<String, String> params) throws IOException {
        try {
            return sendPostRequest(url, params, commonHeaders);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static URLConnection sendStringPostRequest(String url, String postDataString, Map<String, String> headers) throws IOException {
        if (postDataString == null) {
            postDataString = "";
        }
        Set<Entry<String, String>> entrys;
        URL urlObject = new URL(url);
        HttpURLConnection conn = null;
        try {
            if (urlObject.getProtocol().toUpperCase().equals("HTTPS")) {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) urlObject.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) urlObject.openConnection();
            }
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            if (headers != null) {
                entrys = headers.entrySet();
                for (Entry<String, String> entry : entrys) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            //System.out.println("buf.toString():"+buf.toString());
            out.write(postDataString.getBytes(defaultCharset));
            out.flush();
            conn.getResponseCode(); // 为了发送成功
            return conn;
        } catch (IOException e) {
            if (conn != null) {
                conn.disconnect();
            }
            throw e;
        }
    }

    public static URLConnection sendStringPostRequest(String url, String postDataString) throws IOException {
        try {
            return sendStringPostRequest(url, postDataString, commonHeaders);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getResponseString(URLConnection urlConnection) throws IOException {

        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        StringBuffer resultBuffer = new StringBuffer();
        String tempLine;

        try {
            if (((HttpURLConnection) urlConnection).getResponseCode() >= 300) {
                throw new IOException("HTTP Request is not success, Response code is " + ((HttpURLConnection) urlConnection).getResponseCode());
            }
            inputStream = urlConnection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, defaultCharset);
            reader = new BufferedReader(inputStreamReader);

            while ((tempLine = reader.readLine()) != null) {
                resultBuffer.append(tempLine + "\n");
            }
            return resultBuffer.toString();
        } finally {

            if (reader != null) {
                reader.close();
            }

            if (inputStreamReader != null) {
                inputStreamReader.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }
            ((HttpURLConnection) urlConnection).disconnect();
        }
    }


    public static void save2File(URLConnection urlConnection, String saveFilePath) throws IOException {

        DataInputStream dis = null;
        FileOutputStream fos = null;

        try {
            dis = new DataInputStream(urlConnection.getInputStream());
            //建立一个新的文件
            fos = new FileOutputStream(new File(saveFilePath));
            byte[] buffer = new byte[1024];
            int length;
            //开始填充数据
            while ((length = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } finally {
            if (dis != null) {
                dis.close();
            }
            if (fos != null) {
                fos.close();
            }
            ((HttpsURLConnection) urlConnection).disconnect();
        }
    }

    public static HeadRequestResponse performHeadRequest(String url) throws IOException {
        return performHeadRequest(url, commonHeaders);
    }

    public static HeadRequestResponse performHeadRequest(String url, Map<String, String> headers) throws IOException {
        return performHeadRequestForRedirects(url, headers, 0);
    }

    private static HeadRequestResponse performHeadRequestForRedirects(String url, Map<String, String> headers, int redirectCount) throws IOException {
        URL urlObject = new URL(url);
        HttpURLConnection conn = null;
        try {
            if (urlObject.getProtocol().toUpperCase().equals("HTTPS")) {
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) urlObject.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                URLConnection urlConnection = urlObject.openConnection();
                if (urlConnection instanceof HttpURLConnection) {
                    conn = (HttpURLConnection) urlConnection;
                } else {
                    return null;
                }
            }
//            addTimeoutChecking(conn);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            if (headers != null) {
                Set<Entry<String, String>> entrySet = headers.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            Map<String, List<String>> headerFields = conn.getHeaderFields();
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            if (responseCode == 302) {
                if (redirectCount >= maxRedirects) {
                    return new HeadRequestResponse(url, new HashMap<String, List<String>>());
                } else {
                    String location = headerFields.get("Location").get(0);
                    return performHeadRequestForRedirects(location, headers, redirectCount + 1);
                }
            } else {
                return new HeadRequestResponse(url, headerFields);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void addTimeoutChecking(HttpURLConnection conn) {
        ConnectionCheckDTO checkDTO = new ConnectionCheckDTO();
        checkDTO.setConnection(conn);
        checkDTO.setStartTime(System.currentTimeMillis());
        synchronized (checkDTOList) {
            checkDTOList.add(checkDTO);
        }
        if (!hasCheckingThread.get()) {
            hasCheckingThread.set(true);
            new Thread(() -> {
                while (!checkDTOList.isEmpty()) {
                    synchronized (checkDTOList) {
                        long now = System.currentTimeMillis();
                        for (Iterator<ConnectionCheckDTO> iterator = checkDTOList.iterator(); iterator.hasNext(); ) {
                            ConnectionCheckDTO next = iterator.next();
                            if (now - next.getStartTime() > 10 * 1000) {
                                //10秒超时，强制断开链接
                                if (next.getConnection() != null) {
                                    try {
                                        next.getConnection().disconnect();
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                }
                                iterator.remove();
                            }
                        }
                        if (checkDTOList.isEmpty()) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                hasCheckingThread.set(false);
            }).start();
        }
    }

    public static class HeadRequestResponse {
        private String realUrl;
        private Map<String, List<String>> headerMap;

        public HeadRequestResponse() {
        }

        public HeadRequestResponse(String realUrl, Map<String, List<String>> headerMap) {
            this.realUrl = realUrl;
            this.headerMap = headerMap;
        }

        public String getRealUrl() {
            return realUrl;
        }

        public void setRealUrl(String realUrl) {
            this.realUrl = realUrl;
        }

        public Map<String, List<String>> getHeaderMap() {
            return headerMap;
        }

        public void setHeaderMap(Map<String, List<String>> headerMap) {
            this.headerMap = headerMap;
        }
    }

}