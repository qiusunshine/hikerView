package com.jeffmony.videocache.common;

/**
 * @author jeffmony
 */

public class VideoCacheException extends Exception {

    private String mMsg;

    public VideoCacheException(String message) {
        super(message);
        mMsg = message;
    }

    public VideoCacheException(String message, Throwable cause) {
        super(message, cause);
        mMsg = message;
    }

    public VideoCacheException(Throwable cause) {
        super(cause);
    }

    public String getMsg() {
        return mMsg;
    }
}
