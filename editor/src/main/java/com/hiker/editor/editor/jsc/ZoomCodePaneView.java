package com.hiker.editor.editor.jsc;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * 作者：By 15968
 * 日期：On 2019/11/2
 * 时间：At 23:33
 */
public class ZoomCodePaneView extends LinearLayout {
    private static final String TAG = "ZoomCodePaneView";
    /**
     * 最小字体
     */
    public static final float MIN_TEXT_SIZE = 0.5f;

    /**
     * 最大子图
     */
    public static final float MAX_TEXT_SIZE = 1.5f;

    /**
     * 缩放比例
     */
    float scale = 1.0f;

    /**
     * 缩放比例跨度
     */
    float scaleGas = 0.1f;

    /**
     * 设置字体大小
     */
    float textSize;

    private EditText editText;

    /**
     * 记录按下第二个点距第一个点的距离
     */
    float oldDist;

    // -----------------------------------------------
    private static final int NONE = 0;// 空
    private static final int DRAG = 1;// 按下第一个点
    private static final int ZOOM = 2;// 按下第二个点

    /**
     * 屏幕上点的数量
     */
    private int mode = NONE;

    public ZoomCodePaneView(Context context) {
        super(context);
    }

    public ZoomCodePaneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(CodePane codePane, float scaleGas) {
        this.scaleGas = scaleGas;
        this.editText = codePane.getCodeText();
        textSize = editText.getTextSize();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return ev.getPointerCount() == 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() != 2) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDist = spacing(event);
                if (newDist - oldDist > 10) {
                    zoomOut();
                    oldDist = newDist;
                }
                if (oldDist - newDist > 10) {
                    zoomIn();
                    oldDist = newDist;
                }
                break;
        }
        return false;
    }

    /**
     * 求出2个触点间的 距离
     *
     * @param event
     * @return
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 放大
     */
    protected void zoomOut() {
        scale += scaleGas;
        if (scale > MAX_TEXT_SIZE) {
            scale = MAX_TEXT_SIZE;
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * scale);
    }

    /**
     * 缩小
     */
    protected void zoomIn() {
        scale -= scaleGas;
        if (scale < MIN_TEXT_SIZE) {
            scale = MIN_TEXT_SIZE;
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * scale);
    }

}
