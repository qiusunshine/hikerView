package com.google.android.exoplayer2.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * author  yangc
 * date 2018/7/17
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated:
 */
public class ExoPlayerView extends PlayerView {
    private static final String TAG = "ExoPlayerView";
    private final static int DOUBLE_TAP_TIMEOUT = 300;
    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;
    private OnDoubleClickListener doubleClickListener;

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

    public FrameLayout getContentFrameLayout() {
        return contentFrameLayout;
    }

    public AspectRatioFrameLayout getAspectRatioFrameLayout() {
        return contentFrame;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent: " + ev.getAction());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, ev)) {
                if (getDoubleClickListener() != null) {
                    getDoubleClickListener().click();
                }
            }
            mCurrentDownEvent = MotionEvent.obtain(ev);
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            mPreviousUpEvent = MotionEvent.obtain(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!useController || player == null || ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (!controllerHideOnTouch) {
            return false;
        } else if (!controller.isVisible()) {
            controller.setInAnim();
            maybeShowController(true);
        } else if (controllerHideOnTouch) {
            controller.setOutAnim();
        }
        return true;
    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
        if (firstDown == null || firstUp == null) {
            return false;
        }
        if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_TAP_TIMEOUT) {
            return false;
        }
        int deltaX = (int) firstUp.getX() - (int) secondDown.getX();
        int deltaY = (int) firstUp.getY() - (int) secondDown.getY();
        return deltaX * deltaX + deltaY * deltaY < 10000;
    }

    public OnDoubleClickListener getDoubleClickListener() {
        return doubleClickListener;
    }

    public void setDoubleClickListener(OnDoubleClickListener doubleClickListener) {
        this.doubleClickListener = doubleClickListener;
    }

    public interface OnDoubleClickListener {
        void click();
    }


}
