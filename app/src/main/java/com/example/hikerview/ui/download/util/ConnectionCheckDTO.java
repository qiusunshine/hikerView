package com.example.hikerview.ui.download.util;

import java.net.HttpURLConnection;

/**
 * 作者：By 15968
 * 日期：On 2021/1/3
 * 时间：At 15:21
 */

public class ConnectionCheckDTO {
    private HttpURLConnection connection;

    private long startTime;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }

    public void setConnection(HttpURLConnection connection) {
        this.connection = connection;
    }
}
