package com.example.hikerview.event;


import com.example.hikerview.ui.browser.model.DetectedMediaResult;

/**
 * Created by xm on 17/8/21.
 */
public class FindVideoEvent {
    private String title;
    private DetectedMediaResult mediaResult;

    public FindVideoEvent(String title) {
        this.title = title;
    }

    public FindVideoEvent() {
    }

    public FindVideoEvent(String title, DetectedMediaResult mediaResult) {
        this.title = title;
        this.mediaResult = mediaResult;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DetectedMediaResult getMediaResult() {
        return mediaResult;
    }

    public void setMediaResult(DetectedMediaResult mediaResult) {
        this.mediaResult = mediaResult;
    }
}
