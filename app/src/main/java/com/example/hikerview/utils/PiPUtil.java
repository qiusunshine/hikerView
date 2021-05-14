package com.example.hikerview.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Rational;

import androidx.annotation.DrawableRes;

import com.example.hikerview.BuildConfig;
import com.example.hikerview.R;
import com.example.hikerview.constants.PreferenceConstant;
import com.example.hikerview.ui.video.EmptyActivity;

import java.util.ArrayList;

import chuangyuan.ycj.videolibrary.widget.VideoPlayerView;
import timber.log.Timber;

/**
 * @author reborn
 * @program hiker-view
 * @description 画中画工具类
 * @create 2020-12-27 07:53
 **/
public class PiPUtil {

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    // 是否已经在画中画模式(自行判断赋值时机)
    private boolean isInPIPMode = false;
    // 是否点击进入过画中画模式--用于判断程序在后台时,由画中画返回全屏后退出,是否启动首页activity,以及onStop配合判断是否点击进入过画中画且在画中画模式
    private boolean isEnteredPIPMode = false;

    private boolean hasEnteredPIPMode = false;
    private boolean isMoveTaskToFront = true;

    private Activity context;
    private VideoPlayerView videoPlayerView;
    private OnPipMediaControlListener onPipMediaControlListener;

    private BroadcastReceiver mReceiver;
    private static final String ACTION_MEDIA_CONTROL = "media_control";
    private static final String EXTRA_CONTROL_TYPE = "control_type";
    private static final int CONTROL_TYPE_PLAY = 1;
    private static final int CONTROL_TYPE_PAUSE = 2;
    private static final int CONTROL_TYPE_LAST = 3;
    private static final int CONTROL_TYPE_NEXT = 4;
    private static final int REQUEST_TYPE_PLAY = 1;
    private static final int REQUEST_TYPE_PAUSE = 2;
    private static final int REQUEST_TYPE_LAST = 3;
    private static final int REQUEST_TYPE_NEXT = 4;

    public PiPUtil(Activity context, VideoPlayerView videoPlayerView, OnPipMediaControlListener onPipMediaControlListener) {
        this.context = context;
        this.videoPlayerView = videoPlayerView;
        this.onPipMediaControlListener = onPipMediaControlListener;
    }

    public boolean isInPIPMode() {
        return isInPIPMode;
    }

    public void setInPIPMode(boolean inPIPMode) {
        if (!hasEnteredPIPMode && inPIPMode) {
            hasEnteredPIPMode = true;
        }
        if (inPIPMode) {
            isEnteredPIPMode = true;
        }
        isInPIPMode = inPIPMode;
    }

    public boolean isMoveTaskToFront() {
        return isMoveTaskToFront;
    }

    public void setMoveTaskToFront(boolean moveTaskToFront) {
        isMoveTaskToFront = moveTaskToFront;
    }

    // 进入画中画前判断状态,调用 initPictureInPictureActions
    public void initPictureInPictureActions() {
        if (onPipMediaControlListener != null && onPipMediaControlListener.isPlaying()) {
            updatePictureInPictureActions(R.drawable.ic_exo_pause, "", CONTROL_TYPE_PAUSE, REQUEST_TYPE_PAUSE);
        } else {
            updatePictureInPictureActions(R.drawable.ic_exo_start, "", CONTROL_TYPE_PLAY, REQUEST_TYPE_PLAY);
        }
    }

    /**
     * 刷新自定义按钮 (若是初始化,注意区分进入画中画前 onPause 状态)
     *
     * @param iconId
     * @param title
     * @param controlType
     * @param requestCode 注意!! 每个 intent 的 requestCode 必须不一样
     */
    public void updatePictureInPictureActions(@DrawableRes int iconId, String title, int controlType, int requestCode) {
        if (checkPipPermission()) {
            if (mPictureInPictureParamsBuilder == null) {
                mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
            }
            final ArrayList<RemoteAction> actions = new ArrayList<>();
            // This is the PendingIntent that is invoked when a user clicks on the action item.  You need to use distinct request codes for play and pause, or the PendingIntent won't be  updated.

            // 上一个
            final PendingIntent intentLast = PendingIntent.getBroadcast(context, REQUEST_TYPE_NEXT, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_LAST), 0);
            actions.add(new RemoteAction(Icon.createWithResource(context, R.drawable.ic_action_last_white), "", "", intentLast));
            // 暂停/播放
            final PendingIntent intentPause = PendingIntent.getBroadcast(context, requestCode, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType), 0);
            actions.add(new RemoteAction(Icon.createWithResource(context, iconId), title, title, intentPause));
            // 下一个
            final PendingIntent intentNext = PendingIntent.getBroadcast(context, REQUEST_TYPE_LAST, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_NEXT), 0);
            actions.add(new RemoteAction(Icon.createWithResource(context, R.drawable.ic_action_next_white), "", "", intentNext));

            mPictureInPictureParamsBuilder.setActions(actions);

            // This is how you can update action items (or aspect ratio) for Picture-in-Picture mode. Note this call can happen even when the app is not in PiP mode.
            context.setPictureInPictureParams(mPictureInPictureParamsBuilder.build());
        }
    }

    public void updatePiPRatio(int mVideoWith, int mVideoHeight) {
        if (checkPipPermission()) {
            // videoPlayerView.setIsInPictureInPictureMode(true);
            if (mPictureInPictureParamsBuilder == null) {
                mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
            }
            // Calculate the aspect ratio of the PiP screen. 计算video的纵横比
            if (mVideoWith != 0 && mVideoHeight != 0 && (float) mVideoWith / (float) mVideoHeight < 2.39
                    && (float) mVideoHeight / (float) mVideoWith < 2.39) {
                // 设置 param 宽高比，根据宽高比例调整初始参数
                Rational aspectRatio = new Rational(mVideoWith, mVideoHeight);
                mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio);
            }
            context.setPictureInPictureParams(mPictureInPictureParamsBuilder.build());
        }
    }

    /**
     * 进入画中画模式
     */
    private PictureInPictureParams.Builder mPictureInPictureParamsBuilder;

    public void enterPiPMode() {
        if (videoPlayerView == null) {
            return;
        }
        if (checkPipPermission()) {
            // videoPlayerView.setIsInPictureInPictureMode(true);
            if (mPictureInPictureParamsBuilder == null) {
                mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
            }

            mVideoWidth = mVideoWidth == 0 ? videoPlayerView.getWidth() : mVideoWidth;
            mVideoHeight = mVideoHeight == 0 ? videoPlayerView.getHeight() : mVideoHeight;

            // Calculate the aspect ratio of the PiP screen. 计算video的纵横比
            updatePiPRatio(mVideoWidth, mVideoHeight);
            initPictureInPictureActions();
            // 进入 PiP 模式
            context.enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());
//            TaskUtil.showDetailActivityFromRecents(context, false);
        } else {
            ToastMgr.shortBottomCenter(context, "系统版本太低，不支持小窗模式");
        }
    }

    /**
     * 画中画返回全屏会执行 onResume
     */
    public void onResume() {
        isEnteredPIPMode = false;
    }

    public void onPause() {
        if (isInPictureInPictureMode()) {
            isInPIPMode = true;
            isEnteredPIPMode = true;
        }
    }

    public void onStop() {
        //备注: 在画中画模式下,onStop执行时, 若是关闭画中画,isInPictureInPictureMode()=false ; 若是锁屏,isInPictureInPictureMode()=true ; 判断锁屏isLockPage()一直为false
        boolean inPictureInPictureMode = isInPictureInPictureMode();
        if (BuildConfig.DEBUG) {
            Timber.i("onStop -- inPictureInPictureMode=" + inPictureInPictureMode + " ,isEnteredPIPMode=" + isEnteredPIPMode + " ,isInPIPMode=" + isInPIPMode);
        }
        // MIUI 在小窗状态下点击关闭 inPictureInPictureMode() 会为 true，导致 !inPictureInPictureMode() 判断条件无法进入而使得点击关闭后还有声音
        // 但是如果不加上 inPictureInPictureMode 的话，锁屏暂停恢复会直接失效，因为无论如何都会进这个判断条件杀掉 Activity
        if (isEnteredPIPMode) {
            //满足此条件下认为是关闭了画中画界面
            Timber.d("onStop -- 判断为PIP下关闭画中画");
            isMoveTaskToFront = false;
            TaskUtil.setExcludeFromRecentTasks(context, context.getClass());
            context.finishAndRemoveTask();
            return;
        }
        if (inPictureInPictureMode && isInPIPMode && isEnteredPIPMode && videoPlayerView != null) {
            //满足此条件下认为是画中画模式下锁屏
            if (onPipMediaControlListener != null) {
                onPipMediaControlListener.onPause();
            }
            if (BuildConfig.DEBUG) {
                Timber.w("onStop -- 判断为PIP下锁屏");
            }
        }
    }

    /**
     * 处理页面finish得情况，如果已经进入过画中画，需要移除task
     *
     * @param activity
     * @return false：页面自己执行finish
     */
    public boolean finished(Activity activity) {
        if (hasEnteredPIPMode) {
            TaskUtil.setExcludeFromRecentTasks(activity, activity.getClass());
            activity.finishAndRemoveTask();
            // 保证多任务可以恢复视界的卡片，避免按返回键就回到桌面
            activity.startActivity(new Intent(activity, EmptyActivity.class));
            return true;
        }
        return false;
    }

    /**
     * 切换应用时进入 PiP
     * <p>
     * TODO Bug: 华为、小米不调用该方法
     * Reason: 实测三大金刚键回桌面会调用，但是全面屏手势回桌面则不调用
     */
    public void onUserLeaveHint() {
        boolean backgroundToPiP = PreferenceMgr.getBoolean(context, PreferenceConstant.KEY_BACKGROUND_TO_PIP, false);
        if (backgroundToPiP && onPipMediaControlListener != null && onPipMediaControlListener.isPlaying()) {
            try {
                enterPiPMode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkPipPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public boolean isInPictureInPictureMode() {
        return checkPipPermission() && context.isInPictureInPictureMode();
    }

    public void setVideoWidthAndHeight(int width, int height) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
    }

    public void registerPipReceiver() {
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !ACTION_MEDIA_CONTROL.equals(intent.getAction())) {
                        return;
                    }
                    // This is where we are called back from Picture-in-Picture action
                    final int controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0);
                    try {
                        switch (controlType) {
                            case CONTROL_TYPE_PLAY:
                                // player.setStartOrPause(true);
                                if (onPipMediaControlListener != null) {
                                    onPipMediaControlListener.onMediaPlay();
                                }
                                // 自定义 action 刷新 开始播放 按钮替换为暂停
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    updatePictureInPictureActions(R.drawable.ic_exo_pause, "", CONTROL_TYPE_PAUSE, REQUEST_TYPE_PAUSE);
                                }
                                break;
                            case CONTROL_TYPE_PAUSE:
                                // player.setStartOrPause(false);
                                if (onPipMediaControlListener != null) {
                                    onPipMediaControlListener.onMediaPause();
                                }
                                // 自定义 action 刷新 暂停播放 按钮替换为开始
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    updatePictureInPictureActions(R.drawable.ic_exo_start, "", CONTROL_TYPE_PLAY, REQUEST_TYPE_PLAY);
                                }
                                break;
                            case CONTROL_TYPE_LAST:
                                /*if (context instanceof VideoPlayerActivity) {
                                    ((VideoPlayerActivity) context).nextMovie(true);
                                }*/
                                if (onPipMediaControlListener != null) {
                                    onPipMediaControlListener.onLast();
                                }
                                break;
                            case CONTROL_TYPE_NEXT:
                                /*if (context instanceof VideoPlayerActivity) {
                                    ((VideoPlayerActivity) context).nextMovie(false);
                                }*/
                                if (onPipMediaControlListener != null) {
                                    onPipMediaControlListener.onNext();
                                }
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            context.registerReceiver(mReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
        }
    }

    public void unregisterPipReceiver() {
        if (mReceiver != null) {
            context.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    public interface OnPipMediaControlListener {
        boolean isPlaying();

        void onPause();

        void onMediaPlay();

        void onMediaPause();

        void onLast();

        void onNext();
    }

    public void setOnPipMediaControlListener(OnPipMediaControlListener onPipMediaControlListener) {
        this.onPipMediaControlListener = onPipMediaControlListener;
    }
}
