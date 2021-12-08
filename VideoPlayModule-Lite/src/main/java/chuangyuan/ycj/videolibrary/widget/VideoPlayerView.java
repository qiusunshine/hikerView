package chuangyuan.ycj.videolibrary.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Parcelable;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.AnimUtils;
import com.google.android.exoplayer2.ui.PlayerControlView;

import java.util.ArrayList;
import java.util.List;

import chuangyuan.ycj.videolibrary.R;
import chuangyuan.ycj.videolibrary.listener.ExoPlayerViewListener;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;
import chuangyuan.ycj.videolibrary.video.ExoDataBean;
import chuangyuan.ycj.videolibrary.video.ExoUserPlayer;
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager;

/**
 * author yangc
 * date 2017/7/21
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated: 视频播放video
 */
@TargetApi(16)
public final class VideoPlayerView extends BaseView {

    public boolean isNowVerticalFullScreen() {
        return isNowVerticalFullScreen;
    }

    private boolean isNowVerticalFullScreen = false;
    private OnLayoutChangeListener onLayoutChangeListener;

    private List<View> bottomAnimateViews = new ArrayList<>();

    private List<View> topAnimateViews = new ArrayList<>();

    private View rightAnimateView;

    /**
     * Instantiates a new Video player view.
     *
     * @param context the context
     */
    public VideoPlayerView(Context context) {
        this(context, null);
    }

    /**
     * Instantiates a new Video player view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public VideoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Instantiates a new Video player view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        intiClickView();
    }

    @Override
    public void setShowVideoSwitch(boolean showVideoSwitch) {
        super.setShowVideoSwitch(showVideoSwitch);
        if (isLand && !isNowVerticalFullScreen) {
            //横屏全屏才刷新
            getSwitchText().setVisibility(showVideoSwitch ? VISIBLE : GONE);
        }
    }

    /***
     * 初始化点击事件
     * **/
    private void intiClickView() {
        if (playerView.findViewById(R.id.exo_player_replay_btn_id) != null) {
            playerView.findViewById(R.id.exo_player_replay_btn_id).setOnClickListener(onClickListener);
        }
        if (playerView.findViewById(R.id.exo_player_error_btn_id) != null) {
            playerView.findViewById(R.id.exo_player_error_btn_id).setOnClickListener(onClickListener);
        }
        if (playerView.findViewById(R.id.exo_player_btn_hint_btn_id) != null) {
            playerView.findViewById(R.id.exo_player_btn_hint_btn_id).setOnClickListener(onClickListener);
        }
        getSwitchText().setOnClickListener(onClickListener);
        exoControlsBack.setOnClickListener(onClickListener);
        playerView.findViewById(R.id.exo_video_fullscreen).setOnClickListener(onClickListener);
        if (isListPlayer && !isLand) {
            exoControlsBack.setVisibility(GONE);
        }
        playerView.setControllerVisibilityListener(visibilityListener);
        controllerView.setAnimatorListener(animatorListener);
    }

    /***
     * 销毁处理
     * **/
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activity != null && activity.isFinishing()) {
            removeAllViews();
            animatorListener = null;
            exoPlayerViewListener = null;
            onClickListener = null;
            visibilityListener = null;
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        ExoDataBean bean = new ExoDataBean(superState);
        bean.setLand(isLand);
        bean.setSetSystemUiVisibility(setSystemUiVisibility);
        bean.setSwitchIndex(switchIndex);
        bean.setNameSwitch(getNameSwitch());
        return superState;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isLand) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activity.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (state instanceof ExoDataBean) {
            ExoDataBean bean = (ExoDataBean) state;
            if (bean.getNameSwitch() != null) {
                setNameSwitch(bean.getNameSwitch());
            }
            isLand = bean.isLand();
            setSystemUiVisibility = bean.getSetSystemUiVisibility();
            switchIndex = bean.getSwitchIndex();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        boolean is = isListPlayer && getPlay() != null;
        if (is) {
            ExoUserPlayer manualPlayer = VideoPlayerManager.getInstance().getVideoPlayer();
            if (manualPlayer != null && getPlay().toString().equals(manualPlayer.toString())) {
                manualPlayer.reset();
            }
        } else {
            onDestroy();
        }
    }


    /***
     * 设置是横屏,竖屏
     *
     * @param newConfig 旋转对象
     */
    public void doOnConfigurationChanged(int newConfig) {
        //横屏
        fullScreen(newConfig == Configuration.ORIENTATION_LANDSCAPE, true);
        if (getOnLayoutChangeListener() != null) {
            getOnLayoutChangeListener().change(newConfig == Configuration.ORIENTATION_LANDSCAPE ? Layout.LAND : Layout.VERTICAL);
        }
        scaleLayout(newConfig);
    }

    private void fullScreen(boolean fullScreen, boolean checkLand) {
        fullScreen(fullScreen, checkLand, false);
    }

    private void fullScreen(boolean fullScreen, boolean checkLand, boolean verticalLand) {
        if (fullScreen) {
            if (checkLand) {
                if (isLand) {
                    return;
                }
                isLand = true;
            }
            VideoPlayUtils.hideActionBar(getContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activity.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
            //判断是否开启多线路支持
            if (isShowVideoSwitch && !verticalLand) {
                TextView switchText = getSwitchText();
                switchText.setVisibility(VISIBLE);
                if (!getNameSwitch().isEmpty()) {
                    switchText.setText(getNameSwitch().get(switchIndex));
                }
            }
            mLockControlView.setLockCheck(false);
            //列表显示
            showListBack(VISIBLE);
            //显示锁屏按钮
            showLockState(VISIBLE);
            //显更改全屏按钮选中，自动旋转屏幕
            getExoFullscreen().setChecked(true);
        } else {//竖屏
            if (checkLand) {
                if (!isLand) {
                    return;
                }
                isLand = false;
            }
            activity.getWindow().getDecorView().setSystemUiVisibility(setSystemUiVisibility);
            VideoPlayUtils.showActionBar(activity);
            //多线路支持隐藏
            getSwitchText().setVisibility(GONE);
            //列表播放
            showListBack(GONE);
            //隐藏锁屏按钮移除
            showLockState(GONE);
            //更改全屏按钮选中，自动旋转屏幕
            getExoFullscreen().setChecked(false);
        }
    }

    /***
     * 列表显示返回按钮
     * @param visibility visibility
     * **/
    private void showListBack(int visibility) {
        if (isListPlayer()) {
            if (visibility == VISIBLE) {
                exoControlsBack.setVisibility(VISIBLE);
                getPaddingLeft = controllerView.getExoControllerTop().getPaddingLeft();
                controllerView.getExoControllerTop().
                        setPadding(VideoPlayUtils.dip2px(getContext(), 35), 0, 0, 0);
            } else {
                controllerView.getExoControllerTop().setPadding(getPaddingLeft, 0, 0, 0);
            }
            showBackView(visibility, false);
        }
    }

    /**
     * 播放监听事件
     ***/
    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (mExoPlayerListener == null) return false;
                if (mExoPlayerListener.getClickListener() != null) {
                    mExoPlayerListener.getClickListener().onClick(v);
                } else {
                    mExoPlayerListener.startPlayers();
                }
            }
            return false;
        }
    };

    /***
     * 获取监听事件  此方法不是外部调用。
     * @return ComponentListener component listener
     */
    public ExoPlayerViewListener getComponentListener() {
        return exoPlayerViewListener;
    }

    /****
     * 监听返回键
     ***/
    public void exitFullView() {
        if (isNowVerticalFullScreen) {
            verticalFullScreen();
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getExoFullscreen().setChecked(false);
        doOnConfigurationChanged(Configuration.ORIENTATION_PORTRAIT);
    }

    /***
     * 显示隐藏全屏按钮
     *
     * @param visibility 状态
     */
    public void showFullscreenTempView(int visibility) {
        AppCompatCheckBox compatCheckBox = playerView.findViewById(R.id.sexo_video_fullscreen);
        compatCheckBox.setVisibility(visibility);
        compatCheckBox.setButtonDrawable(controllerView.getIcFullscreenSelector());
        compatCheckBox.setOnClickListener(onClickListener);
    }

    /****
     * 重置
     * ***/
    public void resets() {
        mLockControlView.removeCallback();
        if (exoLoadingLayout != null) {
            exoLoadingLayout.setVisibility(GONE);
        }
        if (mActionControlView != null) {
            mActionControlView.hideAllView();
        }
        getPlaybackControlView().hideNo();
        getPlaybackControlView().showNo();
        exoPlayerViewListener.showPreview(VISIBLE, false);
        showPreViewLayout(VISIBLE);

    }


    /****
     * 控制类显示隐藏监听
     ***/
    private PlayerControlView.VisibilityListener visibilityListener = new PlayerControlView.VisibilityListener() {
        @Override
        public void onVisibilityChange(int visibility) {
            showBackView(visibility, false);
            showLockState(visibility);
            if (belowView != null && visibility == View.GONE) {
                belowView.dismissBelowView();
            }
        }
    };
    /****
     * 动画监听
     ***/
    private AnimUtils.AnimatorListener animatorListener = new AnimUtils.AnimatorListener() {
        @Override
        public void show(boolean isIn) {
//            mLockControlView.updateLockCheckBox(isIn);
            if (isIn) {
                if (isLand) {
//                    showLockState(VISIBLE);
                }
                AnimUtils.setInAnim(exoControlsBack).start();
                for (View topAnimateView : topAnimateViews) {
                    AnimUtils.setInAnim(topAnimateView).start();
                }
                for (View bottomAnimateView : bottomAnimateViews) {
                    AnimUtils.setInAnim(bottomAnimateView).start();
                }
                if (rightAnimateView != null) {
                    AnimUtils.setInAnimX(rightAnimateView).start();
                }
            } else {
                AnimUtils.setOutAnim(exoControlsBack, false).start();
                for (View topAnimateView : topAnimateViews) {
                    AnimUtils.setOutAnim(topAnimateView, false).start();
                }
                for (View bottomAnimateView : bottomAnimateViews) {
                    AnimUtils.setOutAnim(bottomAnimateView, true).start();
                }
                if (rightAnimateView != null) {
                    AnimUtils.setOutAnimX(rightAnimateView, true).start();
                }
            }

        }
    };

    public void enterFullScreen() {
        if (isNowVerticalFullScreen) {
            verticalFullScreen();
            return;
        }
        //切竖屏portrait screen
        if (VideoPlayUtils.getOrientation(getContext()) == Configuration.ORIENTATION_LANDSCAPE) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            doOnConfigurationChanged(Configuration.ORIENTATION_PORTRAIT);
            //切横屏landscape
        } else if (VideoPlayUtils.getOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            doOnConfigurationChanged(Configuration.ORIENTATION_LANDSCAPE);
        }
    }

    /***
     * 点击事件监听
     */
    private View.OnClickListener onClickListener = new View.OnClickListener() {

        public void onClick(View v) {

            if (v.getId() == R.id.exo_video_fullscreen || v.getId() == R.id.sexo_video_fullscreen) {
                enterFullScreen();
            } else if (v.getId() == R.id.exo_controls_back) {
                activity.onBackPressed();
            } else if (v.getId() == R.id.exo_player_error_btn_id) {
                if (VideoPlayUtils.isNetworkAvailable(getContext())) {
                    showErrorState(View.GONE);
                    if (mExoPlayerListener != null) {
                        mExoPlayerListener.onCreatePlayers();
                    }

                } else {
                    Toast.makeText(getContext(), R.string.net_network_no_hint, Toast.LENGTH_SHORT).show();
                }
            } else if (v.getId() == R.id.exo_player_replay_btn_id) {
                if (VideoPlayUtils.isNetworkAvailable(getContext())) {
                    showReplay(View.GONE);
                    showBottomView(GONE, null);
                    if (mExoPlayerListener != null) {
                        mExoPlayerListener.onCreatePlayers();
                    }
                } else {
                    Toast.makeText(getContext(), R.string.net_network_no_hint, Toast.LENGTH_SHORT).show();
                }
                //切换
            } else if (v.getId() == R.id.exo_video_switch) {
                if (belowView == null) {
                    belowView = new BelowView(getContext(), getNameSwitch(), getSwitchIndex());
                    belowView.setOnItemClickListener((position, name) -> {
                        if (mExoPlayerListener != null) {
                            mExoPlayerListener.switchUri(position);
                        }
                        switchIndex = position;
                        getSwitchText().setText(name);
                        belowView.dismissBelowView();
                    });
                } else {
                    belowView.setList(getNameSwitch(), getSwitchIndex());
                }
                belowView.showBelowView(v, true, getSwitchIndex());
                //提示播放
            } else if (v.getId() == R.id.exo_player_btn_hint_btn_id) {
                showBtnContinueHint(View.GONE);
                Log.d(TAG, "onClick: mExoPlayerListener != null===>" + (mExoPlayerListener != null));
                if (mExoPlayerListener != null) {
                    if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                        mExoPlayerListener.getPlay().setStartOrPause(true);
                    } else {
                        mExoPlayerListener.playVideoUri();
                        mExoPlayerListener.getPlay().setStartOrPause(true);
                    }
                }
            }
        }
    };

    /**
     * 控制类监听类
     **/
    private ExoPlayerViewListener exoPlayerViewListener = new ExoPlayerViewListener() {

        @Override
        public void hideAlertDialog() {
            hideDialog();
        }

        @Override
        public void showAlertDialog() {
            showDialog();
        }

        @Override
        public void showHidePro(int visibility) {
            getTimeBar().setVisibility(visibility);
        }

        @Override
        public void showLoadStateView(int visibility) {
            showLoadState(visibility);
        }

        @Override
        public void showReplayView(int visibility) {
            if (playerView != null && playerView.getVideoSurfaceView() instanceof TextureView) {
                TextureView surfaceView = (TextureView) playerView.getVideoSurfaceView();
                showBottomView(VISIBLE, surfaceView.getBitmap());
            }
            showReplay(visibility);
        }

        @Override
        public void showErrorStateView(int visibility) {
            showErrorState(visibility);
        }

        @Override
        public void showNetSpeed(@NonNull final String netSpeed) {
            playerView.post(new Runnable() {
                @Override
                public void run() {
                    if (videoLoadingShowText != null) {
                        videoLoadingShowText.setText(netSpeed);
                    }
                }
            });
        }

        @Override
        public void onConfigurationChanged(int newConfig) {
            doOnConfigurationChanged(newConfig);
        }

        @Override
        public void showGestureView(int visibility) {
            mGestureControlView.showGesture(visibility);
        }

        @Override
        public void setTimePosition(@NonNull SpannableString seekTime) {
            mGestureControlView.setTimePosition(seekTime);
        }

        @Override
        public void setVolumePosition(int mMaxVolume, int currIndex) {
            mGestureControlView.setVolumePosition(mMaxVolume, currIndex);
        }

        @Override
        public void setBrightnessPosition(int mMaxVolume, int currIndex) {
            mGestureControlView.setBrightnessPosition(mMaxVolume, currIndex);
        }

        @Override
        public void next() {
            //todo controllerView.next();
        }

        @Override
        public void previous() {
            //todo controllerView.previous();
        }

        @Override
        public void hideController(boolean isShowFulls) {
            if (isShowFulls) {
                showFullscreenTempView(VISIBLE);
            }
            getPlaybackControlView().setOutAnim();
            setControllerHideOnTouch(false);
        }

        @Override
        public void showController(boolean isShowFulls) {
            if (isShowFulls) {
                showFullscreenTempView(GONE);
            }
            playerView.showController();
            getPlaybackControlView().setInAnim();
            setControllerHideOnTouch(true);
        }

        @Override
        public void setControllerHideOnTouch(boolean onTouch) {
            playerView.setControllerHideOnTouch(onTouch);
        }

        @Override
        public void showPreview(int visibility, boolean isPlayer) {
            if (!isPlayer) {
                showPreViewLayout(visibility);
                showBottomView(GONE, null);
                //  getPreviewImage().setVisibility(visibility);
            } else {
                if (exoPreviewPlayBtn != null) {
                    exoPreviewPlayBtn.setVisibility(GONE);
                }
            }
        }

        @Override
        public void setPlayerBtnOnTouch(boolean isTouch) {
            if (isTouch) {
                getPlaybackControlView().getPlayButton().setOnTouchListener(onTouchListener);
                if (exoPreviewPlayBtn != null) {
                    exoPreviewPlayBtn.setOnTouchListener(onTouchListener);
                }
            } else {
                getPlaybackControlView().getPlayButton().setOnTouchListener(null);
                if (exoPreviewPlayBtn != null) {
                    exoPreviewPlayBtn.setOnTouchListener(null);
                }
            }

        }

        @Override
        public void reset() {
            resets();
        }

        @Override
        public int getHeight() {
            return playerView == null ? 0 : playerView.getHeight();
        }

        @Override
        public int getWidth() {
            return playerView == null ? 0 : playerView.getWidth();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void setPlatViewOnTouchListener(OnTouchListener listener) {
            playerView.setOnTouchListener(listener);
        }

        @Override
        public void setShowWitch(boolean showVideoSwitch) {
            setShowVideoSwitch(showVideoSwitch);
        }

        @Override
        public void setSeekBarOpenSeek(boolean isOpenSeek) {
            getTimeBar().setOpenSeek(isOpenSeek);
        }

        @Override
        public boolean isList() {
            return isListPlayer();
        }

        @Override
        public void setPlayer(@NonNull SimpleExoPlayer player) {
            playerView.setPlayer(player);
        }

        @Override
        public boolean isLoadingShow() {
            return isLoadingLayoutShow();
        }

        @Override
        public void setOpenSeek(boolean openSeek) {
            getTimeBar().setOpenSeek(openSeek);
        }

        @Override
        public void exitFull() {
            exitFullView();
        }

        @Override
        public boolean isLock() {
            return mLockControlView.isLock();
        }

        @Override
        public void setSwitchName(@NonNull List<String> name, int switchIndex) {
            VideoPlayerView.this.setSwitchName(name, switchIndex);
        }

        @Override
        public void showLongPress(float speedNow, boolean stopped) {
            mGestureControlView.showTempFast(speedNow);
        }

    };


    public void verticalFullScreen() {
        if (isNowVerticalFullScreen) {
            if (getOnLayoutChangeListener() != null) {
                getOnLayoutChangeListener().change(Layout.VERTICAL);
            }
            fullScreen(false, false);
            setLand(false);
            scaleVerticalLayout();
            isNowVerticalFullScreen = false;
        } else {
            if (getOnLayoutChangeListener() != null) {
                getOnLayoutChangeListener().change(Layout.VERTICAL_LAND);
            }
            fullScreen(true, false, true);
            setLand(true);
            scaleVerticalLayout();
            isNowVerticalFullScreen = true;
        }
    }

    public OnLayoutChangeListener getOnLayoutChangeListener() {
        return onLayoutChangeListener;
    }

    public void setOnLayoutChangeListener(OnLayoutChangeListener onLayoutChangeListener) {
        this.onLayoutChangeListener = onLayoutChangeListener;
    }

    public List<View> getBottomAnimateViews() {
        return bottomAnimateViews;
    }

    public void setBottomAnimateViews(List<View> bottomAnimateViews) {
        this.bottomAnimateViews = bottomAnimateViews;
    }

    public List<View> getTopAnimateViews() {
        return topAnimateViews;
    }

    public void setTopAnimateViews(List<View> topAnimateViews) {
        this.topAnimateViews = topAnimateViews;
    }

    public View getRightAnimateView() {
        return rightAnimateView;
    }

    public void setRightAnimateView(View rightAnimateView) {
        this.rightAnimateView = rightAnimateView;
    }

    public enum Layout {
        VERTICAL,
        LAND,
        VERTICAL_LAND
    }

    public interface OnLayoutChangeListener {
        void change(Layout layout);
    }
}
