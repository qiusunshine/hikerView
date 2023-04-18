package com.google.android.exoplayer2.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.view.ViewPropertyAnimatorListener;

import com.google.android.exoplayer2.C;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * author  yangc
 * date 2018/7/17
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated:
 */
public class ExoPlayerControlView extends PlayerControlView {
    private static final String TAG = "ExoPlayerControlView";
    /******自己定义方法hide*******/
    @DrawableRes
    int icFullscreenSelector = R.drawable.ic_fullscreen_selector;
    private final AppCompatCheckBox exoFullscreen;
    private final TextView videoSwitchText;
    private final TextView controlsTitleText;
    private final View exoControllerBottom;
    private View exoControllerTop;
    private AnimUtils.AnimatorListener animatorListener;
    private final CopyOnWriteArraySet<AnimUtils.UpdateProgressListener> listenerCopyOnWriteArraySet;

    public ExoPlayerControlView(Context context) {
        this(context, null);
    }

    public ExoPlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExoPlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public ExoPlayerControlView(Context context, AttributeSet attrs, int defStyleAttr, AttributeSet playbackAttrs) {
        super(context, attrs, defStyleAttr, playbackAttrs);
        listenerCopyOnWriteArraySet = new CopyOnWriteArraySet<>();
        if (playbackAttrs != null) {
            TypedArray a =
                    context
                            .getTheme()
                            .obtainStyledAttributes(playbackAttrs, R.styleable.PlayerControlView, 0, 0);
            try {
                icFullscreenSelector = a.getResourceId(R.styleable.PlayerControlView_player_fullscreen_image_selector, icFullscreenSelector);
            } finally {
                a.recycle();
            }
        }
        /*我控件布局*/
        exoFullscreen = findViewById(R.id.exo_video_fullscreen);
        videoSwitchText = findViewById(R.id.exo_video_switch);
        controlsTitleText = findViewById(R.id.exo_controls_title);
        exoControllerBottom = findViewById(R.id.exo_controller_bottom);
        exoControllerTop = findViewById(R.id.exo_controller_top);
        if (exoControllerTop == null) {
            exoControllerTop = controlsTitleText;
        }
        if (exoFullscreen != null) {
            exoFullscreen.setButtonDrawable(icFullscreenSelector);
        }
        controlsTitleText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        controlsTitleText.setSingleLine(true);
        controlsTitleText.setSelected(true);
        controlsTitleText.setFocusable(true);
        controlsTitleText.setFocusableInTouchMode(true);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
        releaseAnim();
    }

    @Override
    public void hide() {
        if (isVisible()) {
            setVisibility(GONE);
            if (!visibilityListeners.isEmpty()) {
                for (VisibilityListener visibilityListener : visibilityListeners) {
                    visibilityListener.onVisibilityChange(getVisibility());
                }
            }
            removeCallbacks(hideAction);
            hideAtMs = C.TIME_UNSET;
        }
    }

    /**
     * 设置标题
     *
     * @param title 内容
     **/
    public void setTitle(@NonNull String title) {
        controlsTitleText.setText(title);
    }

    /**
     * Hides the controller.
     */
    public void hideNo() {
        if (isVisible()) {
            setVisibility(GONE);
            removeCallbacks(updateProgressAction);
            removeCallbacks(hideAction);
            hideAtMs = C.TIME_UNSET;
        }
    }

    public void showNo() {
        updateAll();
        requestPlayPauseFocus();
        controlDispatcher.dispatchSetPlayWhenReady(player, false);
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
        controlsTitleText.setAlpha(1f);
        controlsTitleText.setTranslationY(0);
        if (!isVisible()) {
            setVisibility(VISIBLE);
        }
    }

    public View getPlayButton() {
        return playButton;
    }

    public AppCompatCheckBox getExoFullscreen() {
        return exoFullscreen;
    }

    public TextView getSwitchText() {
        return videoSwitchText;
    }

    public View getExoControllerTop() {
        return exoControllerTop;
    }

    public TimeBar getTimeBar() {
        return timeBar;
    }

    /**
     * 设置全屏按钮样式
     *
     * @param icFullscreenStyle 全屏按钮样式
     **/
    public void setFullscreenStyle(@DrawableRes int icFullscreenStyle) {
        this.icFullscreenSelector = icFullscreenStyle;
        if (getExoFullscreen() != null) {
            getExoFullscreen().setButtonDrawable(icFullscreenStyle);
        }
    }

    public int getIcFullscreenSelector() {
        return icFullscreenSelector;
    }


    public void releaseAnim() {
        if (exoControllerTop != null && exoControllerTop.animate() != null) {
            exoControllerTop.animate().cancel();
        }
        if (exoControllerBottom != null && exoControllerBottom.animate() != null) {
            exoControllerBottom.animate().cancel();
        }
        listenerCopyOnWriteArraySet.clear();
    }

    /**
     * 设置移动谈出动画
     **/
    @Override
    public void setOutAnim() {
        Log.d(TAG, "setOutAnim: ");
        if (controlsTitleText != null && exoControllerBottom != null) {
            if (animatorListener != null) {
                animatorListener.show(false);
            }
            AnimUtils.setOutAnim(exoControllerBottom, true).start();
            AnimUtils.setOutAnim(exoControllerTop, false)
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {
                        }

                        @Override
                        public void onAnimationEnd(View view) {
                            if (view != null) {
                                hide();
                            }
                        }

                        @Override
                        public void onAnimationCancel(View view) {
                        }
                    })
                    .start();
        } else {
            hide();
        }
    }

    @Override
    protected void updateProgress() {
        super.updateProgress();
        if (getPlayer() != null) {
            for (AnimUtils.UpdateProgressListener updateProgressListener : listenerCopyOnWriteArraySet) {
                updateProgressListener.updateProgress(getPlayer().getCurrentPosition(), getPlayer().getBufferedPosition(), getPlayer().getDuration());
            }
        }
    }

    /**
     * 设置移动谈入动画
     **/
    public void setInAnim() {
        Log.d(TAG, "setInAnim: ");
        if (controlsTitleText != null && exoControllerBottom != null) {
            if (animatorListener != null) {
                animatorListener.show(true);
            }
            AnimUtils.setInAnim(exoControllerTop).setListener(null).start();
            AnimUtils.setInAnim(exoControllerBottom).start();
        }
    }

    /***
     * 设置动画回调
     * @param animatorListener animatorListener
     * ***/
    public void setAnimatorListener(AnimUtils.AnimatorListener animatorListener) {
        this.animatorListener = animatorListener;
    }

    /***
     * 设置进度回调
     * @param updateProgressListener updateProgressListener
     * ***/
    public void addUpdateProgressListener(@NonNull AnimUtils.UpdateProgressListener updateProgressListener) {
        listenerCopyOnWriteArraySet.add(updateProgressListener);
    }

    /***
     * 移除设置进度回调
     * @param updateProgressListener updateProgressListener
     * ***/
    public void removeUpdateProgressListener(@NonNull AnimUtils.UpdateProgressListener updateProgressListener) {
        listenerCopyOnWriteArraySet.remove(updateProgressListener);
    }

}
