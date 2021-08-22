package com.google.android.exoplayer2.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * author  yangc
 * date 2018/7/17
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated:
 */
public class ExoPlayerView extends PlayerView {
    private static final String TAG = "ExoPlayerView";
    /**
     * 自动管理还是手动管理
     */
    private boolean autoShowController = true;
    private List<OnTouchListener> touchListeners;

    public ExoPlayerView(Context context) {
        this(context, null);
    }

    public ExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PlayerControlView getControllerView() {
        return controller;
    }

    public void addTouchListener(OnTouchListener touchListener) {
        if (touchListeners == null) {
            touchListeners = new ArrayList<>();
        }
        touchListeners.add(touchListener);
    }

    public FrameLayout getContentFrameLayout() {
        return contentFrameLayout;
    }

    public AspectRatioFrameLayout getAspectRatioFrameLayout() {
        return contentFrame;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (touchListeners != null && !touchListeners.isEmpty()) {
            for (OnTouchListener touchListener : touchListeners) {
                touchListener.onTouch(this, ev);
            }
        }
        if (!useController || player == null || ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (!controllerHideOnTouch) {
            return false;
        }
        if (autoShowController) {
            if (!controller.isVisible()) {
                controller.setInAnim();
                maybeShowController(true);
            } else if (controllerHideOnTouch) {
                controller.setOutAnim();
            }
        }
        return true;
    }

    public void reverseController() {
        if (!useController || player == null) {
            return;
        }
        if (!controllerHideOnTouch) {
            return;
        }
        if (!controller.isVisible()) {
            controller.setInAnim();
            maybeShowController(true);
        } else if (controllerHideOnTouch) {
            controller.setOutAnim();
        }
    }

    public boolean isAutoShowController() {
        return autoShowController;
    }

    public void setAutoShowController(boolean autoShowController) {
        this.autoShowController = autoShowController;
    }
}
