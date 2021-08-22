package com.google.android.exoplayer2.ui;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;

/**
 * author  yangc
 * date 2017/11/26
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated: 动画帮助类
 */

public class AnimUtils {

    public static ViewPropertyAnimatorCompat setOutAnim(View view, boolean ab) {
        return ViewCompat.animate(view).translationY(ab ? view.getHeight() : -view.getHeight())
                .setDuration(500)
                .alpha(0.1f);
    }

    public static ViewPropertyAnimatorCompat setOutAnimX(View view, boolean ab) {
        return ViewCompat.animate(view).translationX(ab ? view.getWidth() : -view.getWidth())
                .setDuration(500)
                .alpha(0.1f);
    }

    public static ViewPropertyAnimatorCompat setOutAnimX(View view, int offset, boolean ab) {
        return ViewCompat.animate(view).translationX(ab ? view.getWidth() + offset : -view.getWidth() - offset)
                .setDuration(500)
                .alpha(0.1f);
    }

    public static ViewPropertyAnimatorCompat setInAnimX(View view) {
        return ViewCompat.animate(view).translationX(0)
                .setDuration(500)
                .alpha(1f);
    }

    public static ViewPropertyAnimatorCompat setInAnim(View view) {
        return ViewCompat.animate(view).translationY(0)
                .alpha(1)
                .setDuration(500);
    }

    /**
     * 动画回调接口
     ***/
    public interface AnimatorListener {
        void show(boolean isIn);
    }

    /**
     * 进度更新回调接口
     ***/
    public interface UpdateProgressListener {
        void updateProgress(long position, long bufferedPosition, long duration);
    }

}
