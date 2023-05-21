package com.jeffmony.videocache.model;

import java.util.Objects;

public class VideoRange {
    private long mStart;   //分片的起始位置
    private long mEnd;     //分片的结束位置

    public VideoRange(long start, long end) {
        mStart = start;
        mEnd = end;
    }

    public long getStart() {
        return mStart;
    }

    public long getEnd() {
        return mEnd;
    }

    public boolean contains(long position) {
        return mStart <= position && position <= mEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoRange)) return false;
        VideoRange that = (VideoRange) o;
        return mStart == that.mStart && mEnd == that.mEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStart, mEnd);
    }

    public String toString() {
        return "VideoRange[start="+mStart+", end="+mEnd+"]";
    }
}
