package com.jeffmony.videocache.utils;

import com.jeffmony.videocache.model.VideoRange;

public class VideoRangeUtils {

    /**
     * 确定position和VideoRange的关系
     * 1:   pos
     *            |----------------|
     *
     * 2:               pos
     *            |----------------|
     *
     * 3:                               pos
     *            |----------------|
     *
     * @param range
     * @param position
     * @return
     */
    public static int determineVideoRangeByPosition(VideoRange range, long position) {
        if (position < range.getStart()) {
            return 1;
        }
        if (position >= range.getStart() && position <= range.getEnd()) {
            return 2;
        }
        return 3;
    }

    /**
     * range1是否包含range2
     * @param range1
     * @param range2
     * @return
     */
    public static boolean containsVideoRange(VideoRange range1, VideoRange range2) {
        return range1.getStart() <= range2.getStart() && range1.getEnd() >= range2.getEnd();
    }

    public static int compareVideoRange(VideoRange range1, VideoRange range2) {
        if (range1.getEnd() < range2.getStart()) {
            return 1;
        } else if (range1.getStart() > range2.getEnd()) {
            return 2;
        }
        return -1;
    }



    /**
     * 两个VideoRange 之间的关系
     * 1: |----|
     *            |--------|
     *
     * 2: |----|
     *         |--------|
     *
     * 3: |------|
     *         |--------|
     *
     * 4: |-------------|
     *         |--------|
     *
     * 5: |-----------------|
     *          |--------|
     *
     * 6:       |----|
     *          |--------|
     *
     * 7:       |--------|
     *          |--------|
     *
     * 8:       |------------|
     *          |--------|
     *
     * 9:          |--|
     *          |--------|
     *
     *10:          |-----|
     *          |--------|
     *
     *11:           |-------|
     *          |--------|
     *
     *12:                |-------|
     *          |--------|
     *
     *13:                     |-------|
     *          |--------|
     *
     * @param range1
     * @param range2
     */
    public static int getVideoRangeRelationShip(VideoRange range1, VideoRange range2) {
        if (range1.getStart() < range2.getStart()) {
            if (range1.getEnd() < range2.getStart()) {
                return 1;
            }
            if (range1.getEnd() == range2.getStart()) {
                return 2;
            }
            if (range1.getEnd() < range2.getEnd()) {
                return 3;
            }
            if (range1.getEnd() == range2.getEnd()) {
                return 4;
            }
            return 5;
        } else if (range1.getStart() == range2.getStart()) {
            if (range1.getEnd() < range2.getEnd()) {
                return 6;
            }
            if (range1.getEnd() == range2.getEnd()) {
                return 7;
            }
            return 8;
        } else {
            if (range1.getEnd() < range2.getEnd()) {
                return 9;
            }
            if (range1.getEnd() == range2.getEnd()) {
                return 10;
            }
            if (range1.getEnd() > range2.getEnd()) {
                return 11;
            }
            if (range1.getStart() == range2.getEnd()) {
                return 12;
            }
            return 13;
        }
    }
}
