package com.google.android.exoplayer2.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
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

    /**
     * 当前触摸的点数
     */
    private int pointNum = 0;
    //最大的缩放比例
    public static final float SCALE_MAX = 4.0f;
    private static final float SCALE_MIN = 1.0f;

    private double oldDist = 0;
    private double moveDist = 0;
    private float downX = 0;
    private float downY = 0;
    private boolean useTwoFingerTouch = false;
    private long twoFingerMoveTime = 0;

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
        if (useTwoFingerTouch) {
            switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    pointNum = 1;
                    break;
                case MotionEvent.ACTION_UP:
                    pointNum = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (pointNum == 2) {
                        twoFingerMoveTime = System.currentTimeMillis();
                        //移动操作
                        float x2 = (ev.getX(0) + ev.getX(1)) / 2;
                        float y2 = (ev.getY(0) + ev.getY(1)) / 2;
                        float lessX = downX - x2;
                        float lessY = downY - y2;
                        //只有2个手指的时候才有放大缩小的操作
                        moveDist = spacing(ev);
                        double space = moveDist - oldDist;
                        float scale = (float) (getPlayerScaleX() + space / getPlayerWidth());
                        if (scale > SCALE_MIN && scale < SCALE_MAX) {
                            setScale(scale);
                            setSelfPivot(lessX / scale / 2, lessY / scale / 2);
                        } else if (scale < SCALE_MIN) {
                            setScale(SCALE_MIN);
                            setPivot(getPlayerWidth() / 2f, getPlayerHeight() / 2f);
                        } else {
                            setSelfPivot(lessX / getPlayerScaleX() / 2, lessY / getPlayerScaleX() / 2);
                        }
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    //两点按下时的距离
                    oldDist = spacing(ev);
                    pointNum += 1;
                    if (pointNum == 2) {
                        downX = (ev.getX(0) + ev.getX(1)) / 2;
                        downY = (ev.getY(0) + ev.getY(1)) / 2;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    pointNum -= 1;
                    break;
                default:
                    break;
            }
        }
        if (!useController || player == null || ev.getActionMasked() != MotionEvent.ACTION_DOWN || ev.getPointerCount() != 1) {
            return false;
        }
        if (!controllerHideOnTouch) {
            if (!controller.isVisible()) {
                controller.setInAnim();
                maybeShowController(true);
            }
            return false;
        }
        if (autoShowController) {
            if (!controller.isVisible()) {
                controller.setInAnim();
                maybeShowController(true);
            } else if (controllerHideOnTouch && !isShowControllerIndefinitely()) {
                controller.setOutAnim();
            }
        }
        return true;
    }

    /**
     * 触摸使用的移动事件
     *
     * @param lessX x坐标
     * @param lessY y坐标
     */
    private void setSelfPivot(float lessX, float lessY) {
        float setPivotX = 0;
        float setPivotY = 0;
        setPivotX = getPlayerPivotX() + lessX;
        setPivotY = getPlayerPivotY() + lessY;
        Log.e("lawwingLog", "setPivotX:" + setPivotX + "  setPivotY:" + setPivotY
                + "  getWidth:" + getPlayerWidth() + "  getHeight:" + getPlayerHeight());
        if (setPivotX < 0 && setPivotY < 0) {
            setPivotX = 0;
            setPivotY = 0;
        } else if (setPivotX > 0 && setPivotY < 0) {
            setPivotY = 0;
            if (setPivotX > getPlayerWidth()) {
                setPivotX = getPlayerWidth();
            }
        } else if (setPivotX < 0 && setPivotY > 0) {
            setPivotX = 0;
            if (setPivotY > getPlayerHeight()) {
                setPivotY = getPlayerHeight();
            }
        } else {
            if (setPivotX > getPlayerWidth()) {
                setPivotX = getPlayerWidth();
            }
            if (setPivotY > getPlayerHeight()) {
                setPivotY = getPlayerHeight();
            }
        }
        setPivot(setPivotX, setPivotY);
    }

    private int getPlayerWidth() {
        if (getVideoSurfaceView() != null) {
            return getVideoSurfaceView().getWidth();
        }
        return getWidth();
    }

    private int getPlayerHeight() {
        if (getVideoSurfaceView() != null) {
            return getVideoSurfaceView().getHeight();
        }
        return getHeight();
    }

    private float getPlayerPivotX() {
        if (getVideoSurfaceView() != null) {
            return getVideoSurfaceView().getPivotX();
        }
        return getPivotX();
    }

    private float getPlayerPivotY() {
        if (getVideoSurfaceView() != null) {
            return getVideoSurfaceView().getPivotY();
        }
        return getPivotY();
    }

    private float getPlayerScaleX() {
        if (getVideoSurfaceView() != null) {
            return getVideoSurfaceView().getScaleX();
        }
        return getScaleX();
    }

    /**
     * 计算两个点的距离
     *
     * @param event 事件
     * @return 返回数值
     */
    private double spacing(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return Math.sqrt(x * x + y * y);
        } else {
            return 0;
        }
    }

    /**
     * 平移画面，当画面的宽或高大于屏幕宽高时，调用此方法进行平移
     *
     * @param x 坐标x
     * @param y 坐标y
     */
    public void setPivot(float x, float y) {
        if (getVideoSurfaceView() != null) {
            getVideoSurfaceView().setPivotX(x);
            getVideoSurfaceView().setPivotY(y);
        }
    }

    /**
     * 设置放大缩小
     *
     * @param scale 缩放值
     */
    public void setScale(float scale) {
        if (getVideoSurfaceView() != null) {
            getVideoSurfaceView().setScaleX(scale);
            getVideoSurfaceView().setScaleY(scale);
        }
    }

    /**
     * 初始化比例，也就是原始比例
     */
    public void setInitScale() {
        if (getVideoSurfaceView() != null) {
            getVideoSurfaceView().setScaleX(1.0f);
            getVideoSurfaceView().setScaleY(1.0f);
            setPivot(getPlayerWidth() / 2f, getPlayerPivotY() / 2f);
        }
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

    public boolean isUseTwoFingerTouch() {
        return useTwoFingerTouch;
    }

    public void setUseTwoFingerTouch(boolean useTwoFingerTouch) {
        this.useTwoFingerTouch = useTwoFingerTouch;
    }

    public boolean canTouchAfterTowFinger() {
        if(twoFingerMoveTime <= 0){
            return true;
        }
        //刚双指缩放过，不允许滑动手势
        return System.currentTimeMillis() - twoFingerMoveTime > 400;
    }
}
