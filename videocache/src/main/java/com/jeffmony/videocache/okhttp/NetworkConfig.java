package com.jeffmony.videocache.okhttp;

/**
 * 网络访问的设置
 */
public class NetworkConfig {
    private final long mReadTimeout;
    private final long mConnTimeout;
    private final boolean mIgnoreCert;

    public NetworkConfig(long readTimeout, long connTimeout, boolean ignoreCert) {
        mReadTimeout = readTimeout;
        mConnTimeout = connTimeout;
        mIgnoreCert = ignoreCert;
    }

    public long getReadTimeout() { return mReadTimeout; }

    public long getConnTimeout() { return mConnTimeout; }

    public boolean ignoreCert() { return mIgnoreCert; }
}
