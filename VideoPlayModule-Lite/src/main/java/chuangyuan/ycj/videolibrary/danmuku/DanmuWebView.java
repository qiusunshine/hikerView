package chuangyuan.ycj.videolibrary.danmuku;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 作者：By 15968
 * 日期：On 2021/11/15
 * 时间：At 21:37
 */

public class DanmuWebView extends WebView {
    public DanmuWebView(@NonNull Context context) {
        super(context);
    }

    public DanmuWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DanmuWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean dispatchGenericFocusedEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}