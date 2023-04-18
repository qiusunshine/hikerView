package chuangyuan.ycj.videolibrary.widget;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.ExoPlayerControlView;
import com.google.android.exoplayer2.ui.ExoPlayerView;
import com.google.android.exoplayer2.ui.TimeBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chuangyuan.ycj.videolibrary.R;
import chuangyuan.ycj.videolibrary.danmuku.BiliDanmukuParser;
import chuangyuan.ycj.videolibrary.danmuku.DanamakuAdapter;
import chuangyuan.ycj.videolibrary.danmuku.DanmuWebView;
import chuangyuan.ycj.videolibrary.danmuku.JSONDanmukuParser;
import chuangyuan.ycj.videolibrary.listener.ExoPlayerListener;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;
import chuangyuan.ycj.videolibrary.video.ExoUserPlayer;
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * author  yangc
 * date 2017/11/24
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated: 父类view 存放控件方法
 */
public abstract class BaseView extends FrameLayout {
    /*** The constant TAG.***/
    public static final String TAG = VideoPlayerView.class.getName();
    final Activity activity;
    /***播放view*/
    protected final ExoPlayerView playerView;
    private static final int ANIM_DURATION = 400;
    /*** 加载速度显示*/
    protected TextView videoLoadingShowText, custom_loading_text;
    /***视频加载页,错误页,进度控件,锁屏按布局,自定义预览布局,提示布局,播放按钮*/
    protected View exoLoadingLayout, exoPlayPreviewLayout, exoPreviewPlayBtn, exoBarrageLayout, customLoadingLayout;
    /***水印,封面图占位,显示音频和亮度布图*/
    protected ImageView exoPlayWatermark, exoPreviewImage, exoPreviewBottomImage;

    /***手势管理布局view***/
    protected final GestureControlView mGestureControlView;
    /***意图管理布局view***/
    protected final ActionControlView mActionControlView;

    public LockControlView getmLockControlView() {
        return mLockControlView;
    }

    /*** 锁屏管理布局***/
    protected final LockControlView mLockControlView;
    /***锁屏管理布局***/
    protected final ExoPlayerControlView controllerView;
    /***切换*/
    protected BelowView belowView;
    /***流量提示框***/
    protected AlertDialog alertDialog;
    private boolean networkNotify = true;
    protected ExoPlayerListener mExoPlayerListener;

    public AppCompatImageView getExoControlsBack() {
        return exoControlsBack;
    }

    /***返回按钮*/
    protected AppCompatImageView exoControlsBack;

    public void setLand(boolean land) {
        isLand = land;
    }

    /***是否在上面,是否横屏,是否列表播放 默认false,是否切换按钮*/
    protected boolean isLand;
    protected boolean isListPlayer;

    public boolean isShowVideoSwitch() {
        return isShowVideoSwitch;
    }

    protected boolean isShowVideoSwitch;
    protected boolean isVerticalFullScreen;
    protected boolean isPipMode;

    protected boolean isLandLayout;

    private boolean networkNotifyUseDialog = false;

    public SeekListener getSeekListener() {
        return seekListener;
    }

    public void setSeekListener(SeekListener seekListener) {
        this.seekListener = seekListener;
    }

    private SeekListener seekListener;

    /**
     * 流量提示时是否使用弹窗
     *
     * @param networkNotifyUseDialog
     */
    public void setNetworkNotifyUseDialog(boolean networkNotifyUseDialog) {
        this.networkNotifyUseDialog = networkNotifyUseDialog;
    }

    /**
     * 在屏幕中央展示提示信息
     *
     * @param text
     */
    public void showNotice(String text) {
        if (mGestureControlView != null) {
            mGestureControlView.showNotice(text);
        }
    }

    public String getNotice() {
        if (mGestureControlView != null) {
            return mGestureControlView.getNotice();
        } else {
            return null;
        }
    }

    public boolean isLand() {
        return isLand;
    }

    public boolean isLandLayout() {
        return isLandLayout;
    }

    public void setLandLayout(boolean landLayout) {
        isLandLayout = landLayout;
    }

    /**
     * 是否显示返回按钮
     **/
    private boolean isShowBack = true;
    /***标题左间距*/
    protected int getPaddingLeft;
    private ArrayList<String> nameSwitch;
    /***多分辨率,默认Ui布局样式横屏后还原处理***/
    protected int switchIndex, setSystemUiVisibility = 0;
    /*** The Ic back image.***/
    @DrawableRes
    private int icBackImage = R.drawable.ic_exo_back;


    private BaseDanmakuParser parser;//解析器对象
    private IDanmakuView danmakuView;//弹幕view
    private DanmakuContext danmakuContext;

    public boolean isDanmaKuShow() {
        return danmaKuShow;
    }

    private boolean danmaKuShow = false;
    private WebView danmuWebView;

    public boolean isUseDanmuWebView() {
        return useDanmuWebView;
    }

    private boolean useDanmuWebView = false;
    private boolean danmuDestroyed = false;
    private int danmuLineCount = 5;

    protected ViewGroup danmuViewContainer;


    public ViewGroup getDanmuViewContainer() {
        if (danmuViewContainer == null) {
            return playerView;
        }
        return danmuViewContainer;
    }

    public void setDanmuViewContainer(ViewGroup danmuViewContainer) {
        this.danmuViewContainer = danmuViewContainer;
    }

    /**
     * Instantiates a new Base view.
     *
     * @param context the context
     */
    public BaseView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Instantiates a new Base view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public BaseView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Instantiates a new Base view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public BaseView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        activity = (Activity) getContext();
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        playerView = new ExoPlayerView(getContext(), attrs, defStyleAttr);
        controllerView = (ExoPlayerControlView) playerView.getControllerView();
        mGestureControlView = new GestureControlView(getContext(), attrs, defStyleAttr);
        mActionControlView = new ActionControlView(getContext(), attrs, defStyleAttr, playerView);
        mLockControlView = new LockControlView(getContext(), attrs, defStyleAttr, this);
        addView(playerView, params);
        int userWatermark = 0;
        int defaultArtworkId = 0;
        int loadId = R.layout.simple_exo_play_load;
        int preViewLayoutId = 0;
        int barrageLayoutId = 0;
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.VideoPlayerView, 0, 0);
            try {
                icBackImage = a.getResourceId(R.styleable.VideoPlayerView_player_back_image, icBackImage);
                userWatermark = a.getResourceId(R.styleable.VideoPlayerView_user_watermark, 0);
                isListPlayer = a.getBoolean(R.styleable.VideoPlayerView_player_list, false);
                defaultArtworkId = a.getResourceId(R.styleable.VideoPlayerView_default_artwork, defaultArtworkId);
                loadId = a.getResourceId(R.styleable.VideoPlayerView_player_load_layout_id, loadId);
                preViewLayoutId = a.getResourceId(R.styleable.VideoPlayerView_player_preview_layout_id, preViewLayoutId);
                barrageLayoutId = a.getResourceId(R.styleable.VideoPlayerView_player_barrage_layout_id, barrageLayoutId);
                int playerViewId = a.getResourceId(R.styleable.VideoPlayerView_controller_layout_id, R.layout.simple_exo_playback_control_view);
                if (preViewLayoutId == 0 && (playerViewId == R.layout.simple_exo_playback_list_view || playerViewId == R.layout.simple_exo_playback_top_view)) {
                    preViewLayoutId = R.layout.exo_default_preview_layout;
                }
            } finally {
                a.recycle();
            }
        }
        if (barrageLayoutId != 0) {
            exoBarrageLayout = inflate(context, barrageLayoutId, null);
        }
        exoLoadingLayout = inflate(context, loadId, null);
        customLoadingLayout = inflate(context, R.layout.simple_exo_custom_load, null);
        if (preViewLayoutId != 0) {
            exoPlayPreviewLayout = inflate(context, preViewLayoutId, null);
        }
        intiView();
        initWatermark(userWatermark, defaultArtworkId);
    }


    /**
     * Inti view.
     */
    private void intiView() {
        exoControlsBack = new AppCompatImageView(getContext());
        exoControlsBack.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int ss = VideoPlayUtils.dip2px(getContext(), 7f);
        exoControlsBack.setId(R.id.exo_controls_back);
        exoControlsBack.setImageDrawable(ContextCompat.getDrawable(getContext(), icBackImage));
        exoControlsBack.setPadding(ss, ss, ss, ss);
        exoControlsBack.setContentDescription("返回上一级");
        FrameLayout frameLayout = playerView.getContentFrameLayout();
        frameLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.exo_player_background_color));
        exoLoadingLayout.setVisibility(GONE);
        customLoadingLayout.setVisibility(GONE);
//        exoLoadingLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.exo_player_background_color));
//        exoLoadingLayout.setClickable(true);
        frameLayout.addView(mGestureControlView, frameLayout.getChildCount());
        frameLayout.addView(mActionControlView, frameLayout.getChildCount());
        frameLayout.addView(mLockControlView, frameLayout.getChildCount());
        if (null != exoPlayPreviewLayout) {
            frameLayout.addView(exoPlayPreviewLayout, frameLayout.getChildCount());
        }
        frameLayout.addView(exoLoadingLayout, frameLayout.getChildCount());
        frameLayout.addView(customLoadingLayout, frameLayout.getChildCount());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(VideoPlayUtils.dip2px(getContext(), 35f), VideoPlayUtils.dip2px(getContext(), 35f));
        frameLayout.addView(exoControlsBack, frameLayout.getChildCount(), layoutParams);
        int index = frameLayout.indexOfChild(findViewById(R.id.exo_controller_barrage));
        if (exoBarrageLayout != null) {
            frameLayout.removeViewAt(index);
            exoBarrageLayout.setBackgroundColor(Color.TRANSPARENT);
            frameLayout.addView(exoBarrageLayout, index);
        }
        exoPlayWatermark = playerView.findViewById(R.id.exo_player_watermark);
        videoLoadingShowText = playerView.findViewById(R.id.exo_loading_show_text);

        exoPreviewBottomImage = playerView.findViewById(R.id.exo_preview_image_bottom);
        if (playerView.findViewById(R.id.exo_preview_image) != null) {
            exoPreviewImage = playerView.findViewById(R.id.exo_preview_image);
            exoPreviewImage.setBackgroundResource(android.R.color.transparent);
        } else {
            exoPreviewImage = exoPreviewBottomImage;
        }
        setSystemUiVisibility = ((Activity) getContext()).getWindow().getDecorView().getSystemUiVisibility();

        exoPreviewPlayBtn = playerView.findViewById(R.id.exo_preview_play);


        getTimeBar().addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                if (seekListener != null) {
                    seekListener.seek(position);
                }
            }
        });
    }

    /**
     * 是否开启竖屏全屏
     *
     * @param verticalFullScreen isWGh  默认 false  true 开启
     */
    public void setVerticalFullScreen(boolean verticalFullScreen) {
        isVerticalFullScreen = verticalFullScreen;
    }

    public boolean isVerticalFullScreen() {
        return isVerticalFullScreen;
    }

    /**
     * 是否开启竖屏全屏
     *
     * @param verticalFullScreen isWGh  默认 false  true 开启
     */
    public void setPipMode(boolean verticalFullScreen) {
        isPipMode = verticalFullScreen;
    }

    public boolean isPipMode() {
        return isPipMode;
    }


    /**
     * On destroy.
     */
    public void onDestroy() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        if (belowView != null) {
            belowView = null;
        }
        if (exoControlsBack != null && exoControlsBack.animate() != null) {
            exoControlsBack.animate().cancel();
        }
        if (mLockControlView != null) {
            mLockControlView.onDestroy();
        }
        if (mExoPlayerListener != null) {
            mExoPlayerListener = null;
        }
        nameSwitch = null;
        if (danmakuView != null) {
            danmakuView.release();
        }
        danmuDestroyed = true;
        if (danmuWebView != null) {
            danmuWebView.onPause();
            danmuWebView.destroy();
        }
    }


    /***
     * 设置水印图和封面图
     * @param userWatermark userWatermark  水印图
     * @param defaultArtworkId defaultArtworkId   封面图
     */
    protected void initWatermark(int userWatermark, int defaultArtworkId) {
        if (userWatermark != 0) {
            exoPlayWatermark.setImageResource(userWatermark);
        }
        if (defaultArtworkId != 0) {
            setPreviewImage(BitmapFactory.decodeResource(getResources(), defaultArtworkId));
        }
    }

    /***
     * 隐藏网络提示框
     */
    protected void hideDialog() {
        if (alertDialog == null || !alertDialog.isShowing()) {
            return;
        }
        alertDialog.dismiss();
    }

    /***
     * 显示网络提示框
     */
    protected void showDialog() {
        if (!networkNotify) {
            showBtnContinueHint(View.GONE);
            if (mExoPlayerListener != null) {
                if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                    mExoPlayerListener.getPlay().setStartOrPause(true);
                } else {
                    mExoPlayerListener.playVideoUri();
                    mExoPlayerListener.getPlay().setStartOrPause(true);
                }
            }
            return;
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            return;
        }
        alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle(getContext().getString(R.string.exo_play_reminder));
        alertDialog.setMessage(getContext().getString(R.string.exo_play_wifi_hint_no));
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getContext().
                getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (mExoPlayerListener != null) {
                    if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                        mExoPlayerListener.getPlay().setStartOrPause(false);
                    }
                }
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showBtnContinueHint(View.GONE);
                if (mExoPlayerListener != null) {
                    if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                        mExoPlayerListener.getPlay().setStartOrPause(true);
                    } else {
                        mExoPlayerListener.playVideoUri();
                        mExoPlayerListener.getPlay().setStartOrPause(true);
                    }
                }

            }
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_dialog_cardbg);
            alertDialog.show();
            WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            int width = Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
            if (width > 0) {
                lp.width = 4 * width / 5;
                alertDialog.getWindow().setAttributes(lp);
            }
        }
    }

    public void toPortraitLayout() {
        if (!isLandLayout) {
            return;
        }
        ViewGroup parent = (ViewGroup) playerView.getParent();
        if (parent != null) {
            parent.removeView(playerView);
        }
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(playerView, params);
        isLandLayout = false;
    }

    public void toLandLayout() {
        if (isLandLayout) {
            return;
        }
        ViewGroup parent = (ViewGroup) playerView.getParent();
        if (parent != null) {
            parent.removeView(playerView);
        }
        ViewGroup contentView = ((Activity) getContext()).findViewById(android.R.id.content);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        contentView.addView(playerView, params);
        isLandLayout = true;
    }

    /***
     * 设置内容横竖屏内容
     *
     * @param newConfig 旋转对象
     */
    protected void scaleLayout(int newConfig) {
        if (isPipMode) {
            toLandLayout();
            return;
        }
        if (isVerticalFullScreen()) {
            scaleVerticalLayout();
            return;
        }
        if (newConfig == Configuration.ORIENTATION_PORTRAIT) {
            toPortraitLayout();
        } else {
            toLandLayout();
        }
    }

    /***
     * 设置内容竖屏全屏
     *
     */
    public void scaleVerticalLayout() {
        ViewGroup contentView = activity.findViewById(android.R.id.content);
        final ViewGroup parent = (ViewGroup) playerView.getParent();
        if (isLand) {
            if (parent != null) {
                parent.removeView(playerView);
            }
            LayoutParams params;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                contentView.addView(playerView, params);
            } else {
                params = new LayoutParams(getWidth(), getHeight());
                contentView.addView(playerView, params);
                ChangeBounds changeBounds = new ChangeBounds();
                //开启延迟动画，在这里会记录当前视图树的状态
                changeBounds.setDuration(ANIM_DURATION);
                TransitionManager.beginDelayedTransition(contentView, changeBounds);
                ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
                layoutParams.height = LayoutParams.MATCH_PARENT;
                layoutParams.width = LayoutParams.MATCH_PARENT;
                playerView.setLayoutParams(layoutParams);
            }

        } else {
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ChangeBounds changeBounds = new ChangeBounds();
                //开启延迟动画，在这里会记录当前视图树的状态
                changeBounds.setDuration(ANIM_DURATION);
                TransitionManager.beginDelayedTransition(contentView, changeBounds);
                ViewGroup.LayoutParams layoutParams2 = playerView.getLayoutParams();
                layoutParams2.width = getWidth();
                layoutParams2.height = getHeight();
                playerView.setLayoutParams(layoutParams2);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (parent != null) {
                            parent.removeView(playerView);
                        }
                        if (playerView.getParent() != null) {
                            ((ViewGroup) playerView.getParent()).removeView(playerView);
                        }
                        BaseView.this.addView(playerView);
                    }
                }, ANIM_DURATION);
            } else {
                if (parent != null) {
                    parent.removeView(playerView);
                }
                addView(playerView, params);
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mExoPlayerListener.land();
        }
    }

    /***
     * 显示隐藏加载页
     *
     * @param visibility 状态
     */
    public void showLockState(int visibility) {
        mLockControlView.showLockState(visibility);
    }

    public int getLockState() {
        return mLockControlView.getLockState();
    }

    public boolean isLock() {
        return mLockControlView.isLock();
    }

    /***
     * 显示隐藏加载页
     *
     * @param visibility 状态
     */
    protected void showLoadState(int visibility) {
        if (visibility == View.VISIBLE) {
            showErrorState(GONE);
            showReplay(GONE);
            showLockState(GONE);
        }
        if (exoLoadingLayout != null) {
            exoLoadingLayout.setVisibility(visibility);
        }
    }

    public void showCustomLoad(boolean show) {
        if (customLoadingLayout != null) {
            if (show) {
                customLoadingLayout.setVisibility(VISIBLE);
                if (custom_loading_text == null) {
                    custom_loading_text = customLoadingLayout.findViewById(R.id.custom_loading_text);
                }
                custom_loading_text.setText(("加载中，请稍候"));
            } else {
                customLoadingLayout.setVisibility(GONE);
            }
        }
    }

    public void showCustomLoadProgress(int progress) {
        if (customLoadingLayout != null) {
            if (customLoadingLayout.getVisibility() == VISIBLE) {
                if (custom_loading_text == null) {
                    custom_loading_text = customLoadingLayout.findViewById(R.id.custom_loading_text);
                }
                custom_loading_text.setText(("加载中，请稍候 " + progress + "%"));
            }
        }
    }

    /***
     * 显示隐藏错误页
     *
     * @param visibility 状态
     */
    protected void showErrorState(int visibility) {
        if (visibility == View.VISIBLE) {
            playerView.hideController();
            showReplay(GONE);
            showBackView(VISIBLE, true);
            showLockState(GONE);
            showLoadState(GONE);
            showPreViewLayout(GONE);
        }
        mActionControlView.showErrorState(visibility);
    }

    /***
     * 显示按钮提示页
     *
     * @param visibility 状态
     */
    public void showBtnContinueHint(int visibility) {
        showBtnContinueHint(visibility, getResources().getString(R.string.exo_play_wifi_hint_no));
    }

    /***
     * 显示按钮提示页
     *
     * @param visibility 状态
     */
    public void showBtnContinueHint(int visibility, String msg) {
        if (visibility == View.VISIBLE) {
            showReplay(GONE);
            showErrorState(GONE);
            showPreViewLayout(GONE);
            showLoadState(GONE);
            showBackView(VISIBLE, true);
            if (networkNotifyUseDialog && msg != null) {
                showDialog();
            }
        } else {
            hideDialog();
        }
        mActionControlView.showBtnContinueHint(visibility, msg);
    }

    /***
     * 显示隐藏重播页
     *
     * @param visibility 状态
     */
    protected void showReplay(int visibility) {
        if (visibility == View.VISIBLE) {
            controllerView.hideNo();
            showErrorState(GONE);
            showBtnContinueHint(GONE);
            showPreViewLayout(GONE);
            showLockState(GONE);
            showBackView(VISIBLE, true);
            showLoadState(GONE);
        }
        mActionControlView.showReplay(visibility);
    }

    /***
     * 显示隐藏自定义预览布局
     *
     * @param visibility 状态
     */
    protected void showPreViewLayout(int visibility) {
        if (exoPlayPreviewLayout != null) {
            if (exoPlayPreviewLayout.getVisibility() == visibility) {
                return;
            }
            exoPlayPreviewLayout.setVisibility(visibility);
            if (playerView.findViewById(R.id.exo_preview_play) != null) {
                playerView.findViewById(R.id.exo_preview_play).setVisibility(visibility);
            }
        }
    }

    /***
     * 显示隐藏返回键
     *
     * @param visibility 状态
     * @param is is
     */
    protected void showBackView(int visibility, boolean is) {
        if (exoControlsBack != null) {
            //如果是竖屏和且不显示返回按钮，就隐藏
            if (!isShowBack && !isLand) {
                exoControlsBack.setVisibility(GONE);
                return;
            }
            if (isListPlayer() && !isLand) {
                exoControlsBack.setVisibility(GONE);
            } else {
                if (visibility == VISIBLE && is) {
                    exoControlsBack.setTranslationY(0);
                    exoControlsBack.setAlpha(1f);
                }
                exoControlsBack.setVisibility(visibility);
            }
        }
    }


    /***
     * 为了播放完毕后，旋转屏幕，导致播放图像消失处理
     * @param visibility 状态
     * @param bitmap the bitmap
     */
    protected void showBottomView(int visibility, Bitmap bitmap) {
        exoPreviewBottomImage.setVisibility(visibility);
        if (bitmap != null) {
            exoPreviewBottomImage.setImageBitmap(bitmap);
        }
    }


    public boolean isShowBack() {
        return isShowBack;
    }

    /**
     * 设置标题
     *
     * @param showBack true 显示返回  false 反之
     */
    public void setShowBack(boolean showBack) {
        this.isShowBack = showBack;
    }

    /**
     * 设置标题
     *
     * @param title 内容
     */
    public void setTitle(@NonNull String title) {
        controllerView.setTitle(title);
    }

    /***
     * 显示水印图
     *
     * @param res 资源
     */
    public void setExoPlayWatermarkImg(int res) {
        if (exoPlayWatermark != null) {
            exoPlayWatermark.setImageResource(res);
        }
    }

    /**
     * 设置占位预览图
     *
     * @param previewImage 预览图
     */
    public void setPreviewImage(Bitmap previewImage) {
        this.exoPreviewImage.setImageBitmap(previewImage);
    }

    /***
     * 设置播放的状态回调 .,此方法不是外部使用，请不要调用
     *
     * @param mExoPlayerListener 回调
     */
    public void setExoPlayerListener(ExoPlayerListener mExoPlayerListener) {
        this.mExoPlayerListener = mExoPlayerListener;
    }

    /***
     * 设置开启线路切换按钮
     *
     * @param showVideoSwitch true 显示  false 不现实
     */
    public void setShowVideoSwitch(boolean showVideoSwitch) {
        isShowVideoSwitch = showVideoSwitch;
    }

    /**
     * 设置全屏按钮样式
     *
     * @param icFullscreenStyle 全屏按钮样式
     */
    public void setFullscreenStyle(@DrawableRes int icFullscreenStyle) {
        controllerView.setFullscreenStyle(icFullscreenStyle);
    }

    /**
     * 设置开启开启锁屏功能
     *
     * @param openLock 默认 true 开启   false 不开启
     */
    public void setOpenLock(boolean openLock) {
        mLockControlView.setOpenLock(openLock);
    }

    /**
     * 设置开启开启锁屏功能
     *
     * @param openLock 默认 false 不开启   true 开启
     */
    public void setOpenProgress2(boolean openLock) {
        mLockControlView.setProgress(openLock);
    }

    /**
     * Gets name switch.
     *
     * @return the name switch
     */
    public ArrayList<String> getNameSwitch() {
        if (nameSwitch == null) {
            nameSwitch = new ArrayList<>();
        }
        return nameSwitch;
    }

    protected void setNameSwitch(ArrayList<String> nameSwitch) {
        this.nameSwitch = nameSwitch;
    }

    /**
     * Gets name switch.
     *
     * @return the name switch
     */
    protected int getSwitchIndex() {
        return switchIndex;
    }

    /**
     * 设置多分辨显示文字
     *
     * @param name        name
     * @param switchIndex switchIndex
     */
    public void setSwitchName(@NonNull List<String> name, @Size(min = 0) int switchIndex) {
        this.nameSwitch = new ArrayList<>(name);
        this.switchIndex = switchIndex;
        getSwitchText().setText(name.get(switchIndex));
    }

    /****
     * 获取控制类
     *
     * @return PlaybackControlView playback control view
     */
    @NonNull
    public ExoPlayerControlView getPlaybackControlView() {
        return controllerView;
    }

    /***
     * 获取当前加载布局
     *
     * @return boolean
     */
    public boolean isLoadingLayoutShow() {
        return exoLoadingLayout.getVisibility() == VISIBLE;
    }

    /***
     * 获取视频加载view
     *
     * @return View load layout
     */
    @Nullable
    public View getLoadLayout() {
        return exoLoadingLayout;
    }

    /***
     * 流量播放提示view
     *
     * @return View play hint layout
     */
    @Nullable
    public View getPlayHintLayout() {
        return mActionControlView.getPlayBtnHintLayout();
    }

    /***
     * 重播展示view
     *
     * @return View replay layout
     */
    @Nullable
    public View getReplayLayout() {
        return mActionControlView.getPlayReplayLayout();
    }

    /***
     * 错误展示view
     *
     * @return View error layout
     */
    @Nullable
    public View getErrorLayout() {
        return mActionControlView.getExoPlayErrorLayout();
    }

    /***
     * 获取手势音频view
     *
     * @return View 手势
     */
    @NonNull
    public View getGestureAudioLayout() {
        return mGestureControlView.getExoAudioLayout();
    }

    /***
     * 获取手势亮度view
     *
     * @return View gesture brightness layout
     */
    @NonNull
    public View getGestureBrightnessLayout() {
        return mGestureControlView.getExoBrightnessLayout();
    }

    /***
     * 获取手势视频进度调节view
     *
     * @return View gesture progress layout
     */
    @NonNull
    public View getGestureProgressLayout() {
        return mGestureControlView.getDialogProLayout();
    }

    /***
     * 是否属于列表播放
     *
     * @return boolean boolean
     */
    public boolean isListPlayer() {
        return isListPlayer;
    }

    /***
     * 获取全屏按钮
     * @return boolean exo fullscreen
     */
    public AppCompatCheckBox getExoFullscreen() {
        return controllerView.getExoFullscreen();
    }

    /**
     * Gets switch text.
     *
     * @return the switch text
     */
    @NonNull
    public TextView getSwitchText() {
        return controllerView.getSwitchText();
    }

    /**
     * 获取g播放控制类
     *
     * @return ExoUserPlayer play
     */
    @Nullable
    public ExoUserPlayer getPlay() {
        if (mExoPlayerListener == null) {
            return null;
        } else {
            return mExoPlayerListener.getPlay();
        }
    }

    /***
     * 获取预览图
     *
     * @return ImageView preview image
     */
    @NonNull
    public ImageView getPreviewImage() {
        return exoPreviewImage;
    }

    /***
     * 获取内核播放view
     *
     * @return SimpleExoPlayerView player view
     */
    @NonNull
    public ExoPlayerView getPlayerView() {
        return playerView;
    }

    /**
     * 获取进度条
     *
     * @return ExoDefaultTimeBar time bar
     */
    @NonNull
    public ExoDefaultTimeBar getTimeBar() {
        return (ExoDefaultTimeBar) controllerView.getTimeBar();
    }

    /**
     * Sets the aspect ratio that this view should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        getPlayerView().getAspectRatioFrameLayout().setAspectRatio(widthHeightRatio);
    }


    public boolean isNetworkNotify() {
        return networkNotify;
    }

    public void setNetworkNotify(boolean networkNotify) {
        this.networkNotify = networkNotify;
    }


    public void useWebDanmuku(boolean use, String url, int lineCount) {
        useWebDanmuku(use, url, lineCount, null);
    }

    public void useDanmuku(boolean use, File file, int lineCount) {
        useDanmuku(use, file, lineCount, null);
    }

    /**
     * 使用弹幕库
     *
     * @param use
     * @param file
     */
    public void useDanmuku(boolean use, File file, int lineCount, ViewGroup viewContainer) {
        danmaKuShow = use;
        useDanmuWebView = false;
        if (use) {
            networkNotifyUseDialog = true;
            setDanmuViewContainer(viewContainer);
            danmuLineCount = lineCount;
            if (danmakuView != null) {
                if (danmakuView.isShown()) {
                    danmakuView.hide();
                }
                danmakuView.release();
                danmakuView.removeAllDanmakus(true);
                danmakuView = null;
                getDanmuViewContainer().removeView((DanmakuView) danmakuView);
                if (parser != null) {
                    parser.release();
                    parser = null;
                }
            }
            initDanmuku(lineCount);
            if (file != null) {
                createParser(getIsStream(file), file);
                onPrepareDanmaku();
                resolveDanmakuShow();
            }
        } else {
            if (danmakuView != null) {
                if (danmakuView.isShown()) {
                    danmakuView.hide();
                }
                danmakuView.release();
                danmakuView.removeAllDanmakus(true);
            }
        }
    }

    public void useWebDanmuku(boolean use, String url, int lineCount, ViewGroup viewContainer) {
        danmaKuShow = use;
        useDanmuWebView = true;
        if (use) {
            networkNotifyUseDialog = true;
            setDanmuViewContainer(viewContainer);
            danmuLineCount = lineCount;
            if (danmuWebView == null) {
                initDanmuWebView();
            }
            if (danmuWebView.getParent() != null) {
                ((ViewGroup) danmuWebView.getParent()).removeView(danmuWebView);
            }
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDanmuViewContainer().addView(danmuWebView, params);
            danmuWebView.onResume();
            danmuWebView.loadUrl(url);
            postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getPlayerView().getPlayer() != null) {
                    danmuWebView.evaluateJavascript("window.isPlaying = " + getPlayerView().getPlayer().isPlaying() +
                            ";\nwindow.lineCount = " + lineCount, null);
                }
            }, 1000);
            postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getPlayerView().getPlayer() != null) {
                    danmuWebView.evaluateJavascript("window.isPlaying = " + getPlayerView().getPlayer().isPlaying() +
                            ";\nwindow.lineCount = " + lineCount, null);
                }
            }, 3000);
        } else {
            if (danmuWebView != null) {
                danmuWebView.onPause();
                getDanmuViewContainer().removeView(danmuWebView);
            }
        }
    }

    private void initDanmuWebView() {
        danmuWebView = new DanmuWebView(getContext());
        WebSettings webSettings = danmuWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowContentAccess(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        danmuWebView.setBackgroundColor(0); // 设置背景色
        if (danmuWebView.getBackground() != null) {
            danmuWebView.getBackground().setAlpha(0); // 设置填充透明度 范围：0-255
        }
        if (getPlayerView().getPlayer() != null) {
            getPlayerView().getPlayer().addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (danmaKuShow && danmuWebView != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            danmuWebView.evaluateJavascript("window.isPlaying = " + isPlaying +
                                    ";\nwindow.lineCount = " + danmuLineCount, null);
                        }
                        if (isPlaying) {
                            danmuWebView.onResume();
                        } else {
                            danmuWebView.onPause();
                        }
                    }
                }
            });
        }
        addSeekListener();
    }

    private void addSeekListener() {
        getTimeBar().addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                Log.d(TAG, "xxxxxx-onScrubStop: ");
                resolveDanmakuSeek(position);
            }
        });
    }


    private void onPrepareDanmaku() {
        if (danmakuView != null) {
            danmakuView.prepare(parser, danmakuContext);
        }
    }

    private InputStream getIsStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initDanmuku(int lineCount) {
        if (danmakuView != null) {
            return;
        }
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, lineCount); // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        danmakuView = new DanmakuView(getContext());
//        danmakuView.showFPS(true);
        getDanmuViewContainer().addView((DanmakuView) danmakuView, params);
        DanamakuAdapter danamakuAdapter = new DanamakuAdapter(danmakuView);
        danmakuContext = DanmakuContext.create();
        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
                .setScrollSpeedFactor(1.2f)
                .setCacheStuffer(new SpannedCacheStuffer(), danamakuAdapter) // 图文混排使用SpannedCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);
        addSeekListener();
        danmuDestroyed = false;
        if (danmakuView != null) {
            danmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                    if (playerView != null && VideoPlayerManager.PLAY_SPEED > 1f || VideoPlayerManager.tempFastPlay) {
                        float speed = VideoPlayerManager.tempFastPlay ? VideoPlayerManager.PLAY_SPEED * 2 : VideoPlayerManager.PLAY_SPEED;
                        if (speed > 1f) {
                            timer.add((long) (timer.lastInterval() * (speed - 1)));
                        }
                    }
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                }

                @Override
                public void prepared() {
                    if (danmakuView != null) {
                        post(() -> {
                            if (playerView.getPlayer() != null && playerView.getPlayer().getCurrentPosition() > 0) {
                                Log.d(TAG, "xxxxxx-prepared: ");
                                if (danmakuView != null) {
                                    danmakuView.start(getPlayerView().getPlayer().getCurrentPosition());
                                }
                            } else {
                                danmakuView.start();
                            }
                            if (!playerView.getPlayer().isPlaying()) {
                                danmakuView.pause();
                            }
                            resolveDanmakuShow();
                        });
                    }
                }
            });
            danmakuView.enableDanmakuDrawingCache(true);
            if (getPlayerView().getPlayer() != null) {
                getPlayerView().getPlayer().addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        if (danmaKuShow && danmakuView != null) {
                            if (isPlaying) {
                                danmakuView.resume();
                            } else {
                                danmakuView.pause();
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * 正常情况下不需要主动调用，自动跟随视频暂停和播放
     */
    public void resumeDanmu() {
        if (danmaKuShow && danmakuView != null && getPlayerView().getPlayer() != null) {
            danmakuView.resume();
        }
        if (danmaKuShow && danmuWebView != null && getPlayerView().getPlayer() != null) {
            danmuWebView.onResume();
        }
        if (playerView.getPlayer() != null && playerView.getPlayer().getCurrentPosition() > 0) {
            Log.d(TAG, "xxxxxx-prepared: ");
            resolveDanmakuSeek(playerView.getPlayer().getCurrentPosition());
        }
    }

    /**
     * 正常情况下不需要主动调用，自动跟随视频暂停和播放
     */
    public void pauseDanmu() {
        if (danmaKuShow && danmakuView != null && getPlayerView().getPlayer() != null) {
            danmakuView.pause();
        }
        if (danmaKuShow && danmuWebView != null && getPlayerView().getPlayer() != null) {
            danmuWebView.onPause();
        }
    }

    /**
     * 弹幕的显示与关闭
     */
    private void resolveDanmakuShow() {
        post(() -> {
            if (danmakuView == null) {
                return;
            }
            if (danmaKuShow) {
                if (!danmakuView.isShown()) {
                    danmakuView.show();
                    if (danmakuView.isPaused()) {
                        danmakuView.resume();
                    }
                }
            } else {
                if (danmakuView.isShown()) {
                    danmakuView.hide();
                }
            }
        });
    }

    /**
     * 弹幕偏移
     */
    private void resolveDanmakuSeek(long time) {
        if (!useDanmuWebView && danmakuView != null && danmakuView.isPrepared()) {
            //不延时会抖动，延时后bug：如果一秒内又暂停播放了，弹幕无法暂停住
            postDelayed(() -> {
                if (!danmuDestroyed && danmakuView != null && getPlayerView().getPlayer() != null) {
                    danmakuView.seekTo(getPlayerView().getPlayer().getCurrentPosition());
                }
            }, 1000);
        }
        if (useDanmuWebView && danmuWebView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                danmuWebView.evaluateJavascript("window.seek = " + time, null);
            }
        }
    }

    /**
     * 创建解析器对象，解析输入流
     *
     * @param stream
     * @return
     */
    private void createParser(InputStream stream, File file) {
        if (stream == null) {
            parser = isJson(file) ? new JSONDanmukuParser() {
                @Override
                public IDanmakus parse() {
                    return new Danmakus();
                }
            } : new BiliDanmukuParser() {
                @Override
                public Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
        parser = isJson(file) ? new JSONDanmukuParser() : new BiliDanmukuParser();
        loadDanmuStream(stream, file);
    }

    private boolean isJson(File file) {
        return file.getAbsolutePath().endsWith(".json");
    }

    private void loadDanmuStream(InputStream stream, File file) {
        if (parser == null || stream == null) {
            return;
        }
        ILoader loader = DanmakuLoaderFactory.create(isJson(file) ? DanmakuLoaderFactory.TAG_ACFUN : DanmakuLoaderFactory.TAG_BILI);
        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
    }

    public void seekFromPlayer(long pos) {
        Log.d(TAG, "xxxxxx-seekFromPlayer: ");
        resolveDanmakuSeek(pos);
        if (seekListener != null) {
            seekListener.seek(pos);
        }
    }

    public void updateDanmuLines(int lineCount) {
        if (danmakuView != null && danmakuContext != null) {
            HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
            maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_LR, lineCount);
            danmakuContext.setMaximumLines(maxLinesPair);
        }
    }

    public interface SeekListener {
        void seek(long pos);
    }
}
