package chuangyuan.ycj.videolibrary.video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.util.Util;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import chuangyuan.ycj.videolibrary.listener.DataSourceListener;
import chuangyuan.ycj.videolibrary.listener.OnGestureBrightnessListener;
import chuangyuan.ycj.videolibrary.listener.OnGestureProgressListener;
import chuangyuan.ycj.videolibrary.listener.OnGestureVolumeListener;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView;

/**
 * author yangc
 * date 2017/2/28
 * E-Mail:1007181167@qq.com
 * Description：增加手势播放器
 */
public class GestureVideoPlayer extends ExoUserPlayer {
    private static final String TAG = GestureVideoPlayer.class.getName();
    /***音量的最大值***/
    private int mMaxVolume;
    /*** 亮度值 ***/
    private float brightness = -1;
    /**** 当前音量  ***/
    private int volume = -1;
    /*** 新的播放进度 ***/
    private long newPosition = -1;
    /*** 音量管理 ***/
    private AudioManager audioManager;
    /*** 手势操作管理 ***/
    private final GestureDetector gestureDetector;
    /*** 屏幕最大宽度 ****/
    private int screeHeightPixels;
    /*** 屏幕最大宽度 ****/
    private int screeWidthPixels;
    /***格式字符 ****/
    private StringBuilder formatBuilder;
    /***格式化类 ***/
    private Formatter formatter;
    private boolean controllerHideOnTouch = true;
    /***手势进度接口实例 ***/
    private OnGestureProgressListener onGestureProgressListener;
    /***手势亮度接口实例 ***/
    private OnGestureBrightnessListener onGestureBrightnessListener;
    /***手势音频接口实例***/
    private OnGestureVolumeListener onGestureVolumeListener;

    private OnDoubleTapListener onDoubleTapListener;
    private float x = 0, y = 0;
    private VerticalMoveGestureListener verticalMoveGestureListener;

    public VerticalMoveGestureListener getVerticalMoveGestureListener() {
        return verticalMoveGestureListener;
    }

    public void setVerticalMoveGestureListener(VerticalMoveGestureListener verticalMoveGestureListener) {
        this.verticalMoveGestureListener = verticalMoveGestureListener;
    }

    public interface VerticalMoveGestureListener {
        void move(float dx, float dy);
    }

    /**
     * Instantiates a new Gesture video player.
     *
     * @param activity   the activity
     * @param playerView the player view
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public GestureVideoPlayer(@NonNull Activity activity, @NonNull VideoPlayerView playerView) {
        this(activity, playerView, null);
    }

    /**
     * Instantiates a new Gesture video player.
     *
     * @param activity the activity
     * @param reId     the re id
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public GestureVideoPlayer(@NonNull Activity activity, @IdRes int reId) {
        this(activity, reId, null);
    }

    /**
     * Instantiates a new Gesture video player.
     *
     * @param activity the activity
     * @param reId     the re id
     * @param listener the listener
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public GestureVideoPlayer(@NonNull Activity activity, @IdRes int reId, @Nullable DataSourceListener listener) {
        this(activity, (VideoPlayerView) activity.findViewById(reId), listener);
    }

    /**
     * Instantiates a new Gesture video player.
     *
     * @param activity   the activity
     * @param playerView the player view
     * @param listener   the listener
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public GestureVideoPlayer(@NonNull Activity activity, @NonNull VideoPlayerView playerView, @Nullable DataSourceListener listener) {
        super(activity, playerView, listener);
        intiViews();
        gestureDetector = new GestureDetector(activity, new PlayerGestureListener(this));
        videoPlayerView.getPlayerView().setAutoShowController(false);
    }

    /**
     * Instantiates a new Gesture video player.
     *
     * @param activity           the activity
     * @param mediaSourceBuilder the media source builder
     * @param playerView         the player view
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public GestureVideoPlayer(@NonNull Activity activity, @NonNull MediaSourceBuilder mediaSourceBuilder, @NonNull VideoPlayerView playerView) {
        super(activity, mediaSourceBuilder, playerView);
        intiViews();
        gestureDetector = new GestureDetector(activity, new PlayerGestureListener(this));
        videoPlayerView.getPlayerView().setAutoShowController(false);
    }

    private void intiViews() {
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        screeHeightPixels = displayMetrics.heightPixels;
        screeWidthPixels = displayMetrics.widthPixels;
        brightness = getScreenBrightness(activity) / 255.0f;
    }

    /**
     * 1.获取系统默认屏幕亮度值 屏幕亮度值范围（0-255）
     **/
    private int getScreenBrightness(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        int defVal = 125;
        return Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, defVal);
    }

    @Override
    public void onPlayNoAlertVideo() {
        super.onPlayNoAlertVideo();
        getPlayerViewListener().setPlatViewOnTouchListener(listener);
    }

    public void bindListener() {
        getPlayerViewListener().setPlatViewOnTouchListener(listener);
    }

    /**
     * 手势结束
     */
    private synchronized void endGesture() {
        volume = -1;
        brightness = -1f;
        if (newPosition >= 0) {
            if (onGestureProgressListener != null) {
                onGestureProgressListener.endGestureProgress(newPosition);
                newPosition = -1;
            } else {
                player.seekTo(newPosition);
                videoPlayerView.seekFromPlayer(newPosition);
                newPosition = -1;
            }
        }
        VideoPlayerManager.tempFastPlay = false;
        if (player != null && player.getPlaybackParameters() != null && player.getPlaybackParameters().speed != VideoPlayerManager.PLAY_SPEED) {
            player.setPlaybackParameters(new PlaybackParameters(VideoPlayerManager.PLAY_SPEED, 1f));
        }
        getPlayerViewListener().showGestureView(View.GONE);
    }

    /****
     * 滑动进度
     *
     * @param  seekTimePosition  滑动的时间
     * @param  duration         视频总长
     * @param  seekTime    滑动的时间 格式化00:00
     * @param  totalTime    视频总长 格式化00:00
     **/
    private void showProgressDialog(String nowTime, long seekTimePosition, long duration, String seekTime, String totalTime) {
        newPosition = seekTimePosition;
        if (onGestureProgressListener != null) {
            onGestureProgressListener.showProgressDialog(seekTimePosition, duration, seekTime, totalTime);
        } else {
            String gap = (seekTimePosition > player.getCurrentPosition() ? "+" : "-") + getMinSec(seekTimePosition);
            String stringBuilder = nowTime + "/" + seekTime + "\n" + gap;
//            ForegroundColorSpan blueSpan = new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.simple_exo_style_color));
            SpannableString spannableString = new SpannableString(stringBuilder);
//            spannableString.setSpan(blueSpan, nowTime.length(), stringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getPlayerViewListener().setTimePosition(spannableString);
        }
    }

    private String getMinSec(long seekTimePosition) {
        long sec = Math.abs(seekTimePosition - player.getCurrentPosition()) / 1000;
        if (sec < 60) {
            return sec + "s";
        }
        long min = sec / 60;
        return min + "m" + sec % 60 + "s";
    }

    /**
     * 滑动改变声音大小
     *
     * @param percent percent 滑动
     */
    private void showVolumeDialog(float percent) {
        if (volume == -1) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0) {
                volume = 0;
            }
        }
        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume) {
            index = mMaxVolume;
        } else if (index < 0) {
            index = 0;
        }
        // 变更进度条 // int i = (int) (index * 1.5 / mMaxVolume * 100);
        //  String s = i + "%";  // 变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        if (onGestureVolumeListener != null) {
            onGestureVolumeListener.setVolumePosition(mMaxVolume, index);
        } else {
            getPlayerViewListener().setVolumePosition(mMaxVolume, index);
        }
    }

    /**
     * 滑动改变亮度
     *
     * @param percent 值大小
     */
    private synchronized void showBrightnessDialog(float percent) {
        if (brightness < 0) {
            brightness = activity.getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f) {
                brightness = 0.50f;
            } else if (brightness < 0.01f) {
                brightness = 0.01f;
            }
        }
        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        activity.getWindow().setAttributes(lpa);
        if (onGestureBrightnessListener != null) {
            onGestureBrightnessListener.setBrightnessPosition(100, (int) (lpa.screenBrightness * 100));
        } else {
            getPlayerViewListener().setBrightnessPosition(100, (int) (lpa.screenBrightness * 100));
        }
    }


    /**
     * 长按临时快进
     *
     * @param speedNow 当前速度
     */
    private synchronized void showTempFastDialog(float speedNow) {
        if (player == null) {
            return;
        }
        getPlayerViewListener().showLongPress(speedNow, false);
        VideoPlayerManager.tempFastPlay = true;
        float speed = VideoPlayerManager.FAST_PLAY_TIMES > 0 ? VideoPlayerManager.FAST_PLAY_TIMES * speedNow : -VideoPlayerManager.FAST_PLAY_TIMES;
        player.setPlaybackParameters(new PlaybackParameters(speed, 1f));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        audioManager = null;
        formatBuilder = null;
        if (formatter != null) {
            formatter.close();
        }
        formatter = null;
        onGestureBrightnessListener = null;
        onGestureProgressListener = null;
        onGestureVolumeListener = null;
        listener = null;
        VideoPlayerManager.tempFastPlay = false;
    }

    /***
     * 设置手势touch 事件
     * @param controllerHideOnTouch true 启用  false 关闭
     */
    public void setPlayerGestureOnTouch(boolean controllerHideOnTouch) {
        this.controllerHideOnTouch = controllerHideOnTouch;
    }

    /***
     * 实现自定义进度监听事件
     * @param onGestureProgressListener 实例
     */
    public void setOnGestureProgressListener(OnGestureProgressListener onGestureProgressListener) {
        this.onGestureProgressListener = onGestureProgressListener;
    }

    /***
     * 实现自定义亮度手势监听事件
     * @param onGestureBrightnessListener 实例
     */
    public void setOnGestureBrightnessListener(OnGestureBrightnessListener onGestureBrightnessListener) {
        this.onGestureBrightnessListener = onGestureBrightnessListener;
    }

    /***
     * 实现自定义音频手势监听事件
     * @param onGestureVolumeListener 实例
     */
    public void setOnGestureVolumeListener(OnGestureVolumeListener onGestureVolumeListener) {
        this.onGestureVolumeListener = onGestureVolumeListener;
    }

    private View.OnTouchListener listener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    x = event.getX();
                    y = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (activity == null || (!VideoPlayUtils.isLand(activity) && !getVideoPlayerView().isNowVerticalFullScreen())) {
                        if (verticalMoveGestureListener != null) {
                            verticalMoveGestureListener.move(event.getX() - x, event.getY() - y);
                        }
                    }
                    break;
            }
            if (!controllerHideOnTouch) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    return true;
                }
                return false;
            } else if (getPlayerViewListener().isLock()) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    videoPlayerView.getmLockControlView().reverse();
                    return true;
                }
                return false;
            } else if (activity == null || (!VideoPlayUtils.isLand(activity) && !getVideoPlayerView().isNowVerticalFullScreen())) {
                //竖屏（非竖屏全屏）不执行手势
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    videoPlayerView.getPlayerView().reverseController();
                    return true;
                }
                return false;
            }
            // 处理手势结束
            if (event.getAction() == MotionEvent.ACTION_UP) {
                endGesture();
            }
            if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
                return true;
            }
            return false;
        }
    };

    public OnDoubleTapListener getOnDoubleTapListener() {
        return onDoubleTapListener;
    }

    public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener) {
        this.onDoubleTapListener = onDoubleTapListener;
    }

    /****
     * 手势监听类
     *****/
    private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;
        private boolean isNowVerticalFullScreen = false;
        private WeakReference<GestureVideoPlayer> weakReference;

        private boolean canTouchAfterTowFinger() {
            return videoPlayerView != null && videoPlayerView.getPlayerView().canTouchAfterTowFinger();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (e.getPointerCount() != 1) {
                return super.onSingleTapUp(e);
            }
            videoPlayerView.getPlayerView().reverseController();
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
//            boolean left = e.getX() < screeWidthPixels * 0.5f;
            if (e.getPointerCount() == 1 && canTouchAfterTowFinger()) {
                showTempFastDialog(VideoPlayerManager.PLAY_SPEED);
            }
            super.onLongPress(e);
        }

        private PlayerGestureListener(GestureVideoPlayer gestureVideoPlayer) {
            weakReference = new WeakReference<>(gestureVideoPlayer);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (e.getPointerCount() != 1) {
                return true;
            }
            if (onDoubleTapListener != null) {
                if (activity == null || activity.isFinishing()) {
                    return true;
                }
                int width = getPlayerViewListener().getWidth();
                int gap = width / 6;
                int right = width - gap;
                DoubleTapArea tapArea;
                if (e.getX() < gap) {
                    tapArea = DoubleTapArea.LEFT;
                } else if (e.getX() > right) {
                    tapArea = DoubleTapArea.RIGHT;
                } else {
                    tapArea = DoubleTapArea.CENTER;
                }
                onDoubleTapListener.onDoubleTap(e, tapArea);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            firstTouch = true;
            isNowVerticalFullScreen = getVideoPlayerView().isNowVerticalFullScreen();
            return super.onDown(e);
        }

        /**
         * 滑动
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (null == weakReference || weakReference.get() == null || e1.getPointerCount() > 1 || e2.getPointerCount() > 1
                    || !canTouchAfterTowFinger() || VideoPlayerManager.tempFastPlay) {
                return false;
            }
            float mOldX = e1.getX(), mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            if (firstTouch) {
                boolean isAllowScroll;
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                if (isNowVerticalFullScreen) {
                    volumeControl = mOldX > screeWidthPixels * 0.5f;
                    isAllowScroll = mOldY > screeHeightPixels * 0.1f;
                } else {
                    volumeControl = mOldX > screeHeightPixels * 0.5f;
                    isAllowScroll = mOldY > screeWidthPixels * 0.1f;
                }
                if (!isAllowScroll) {
                    return false;
                }
                firstTouch = false;
            }
            if (player != null) {
                if (toSeek) {
                    deltaX = -deltaX;
                    long position = player.getCurrentPosition();
                    long duration = player.getDuration();
                    long duration1 = Math.min(duration, 1000 * 60 * 7);
                    long newPosition = (int) (position + deltaX * duration1 / screeHeightPixels / 3);
                    if (Math.abs(newPosition - position) > 240000) {
                        //大于240秒，光速滑动
                        float p = Math.abs((float) newPosition - (float) position) / 10000 * 4 - 61;
                        duration1 = (long) (duration1 * p);
                        duration1 = Math.min(duration, duration1);
                        newPosition = (int) (position + deltaX * duration1 / screeHeightPixels / 3);
                    } else if (Math.abs(newPosition - position) > 120000) {
                        //大于120秒，超速滑动
                        float p = Math.abs((float) newPosition - (float) position) / 10000 * 2 - 13;
                        duration1 = (long) (duration1 * p);
                        duration1 = Math.min(duration, duration1);
                        newPosition = (int) (position + deltaX * duration1 / screeHeightPixels / 3);
                    } else if (Math.abs(newPosition - position) > 20000) {
                        //大于20秒，快速滑动
                        float p = Math.abs((float) newPosition - (float) position) / 10000 - 1;
                        duration1 = (long) (duration1 * p);
                        duration1 = Math.min(duration, duration1);
                        newPosition = (int) (position + deltaX * duration1 / screeHeightPixels / 3);
                    }
                    if (newPosition > duration) {
                        newPosition = duration;
                    } else if (newPosition <= 0) {
                        newPosition = 0;
                    }
                    showProgressDialog(Util.getStringForTime(formatBuilder, formatter, position), newPosition, duration, Util.getStringForTime(formatBuilder, formatter, newPosition), Util.getStringForTime(formatBuilder, formatter, duration));
                } else {
                    float percent = deltaY / getPlayerViewListener().getHeight();
                    if (volumeControl) {
                        showVolumeDialog(percent);
                    } else {
                        showBrightnessDialog(percent);
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public enum DoubleTapArea {
        LEFT,
        CENTER,
        RIGHT
    }


    public interface OnDoubleTapListener {
        void onDoubleTap(MotionEvent e, DoubleTapArea tapArea);
    }

}
