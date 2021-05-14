package com.example.hikerview.ui.dlan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.R;
import com.example.hikerview.utils.DlanListPopUtil;
import com.qingfeng.clinglibrary.Intents;
import com.qingfeng.clinglibrary.control.ClingPlayControl;
import com.qingfeng.clinglibrary.control.callback.ControlCallback;
import com.qingfeng.clinglibrary.control.callback.ControlReceiveCallback;
import com.qingfeng.clinglibrary.entity.ClingVolumeResponse;
import com.qingfeng.clinglibrary.entity.DLANPlayState;
import com.qingfeng.clinglibrary.entity.IResponse;
import com.qingfeng.clinglibrary.service.manager.ClingManager;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.support.model.PositionInfo;

import java.util.Timer;
import java.util.TimerTask;


public class MediaPlayActivity extends AppCompatActivity {
    private static final String TAG = "MediaPlayActivity";
    /**
     * 连接设备状态: 播放状态
     */
    public static final int PLAY_ACTION = 0xa1;
    /**
     * 连接设备状态: 暂停状态
     */
    public static final int PAUSE_ACTION = 0xa2;
    /**
     * 连接设备状态: 停止状态
     */
    public static final int STOP_ACTION = 0xa3;
    /**
     * 连接设备状态: 转菊花状态
     */
    public static final int TRANSITIONING_ACTION = 0xa4;
    /**
     * 获取进度
     */
    public static final int EXTRA_POSITION = 0xa5;
    /**
     * 投放失败
     */
    public static final int ERROR_ACTION = 0xa6;
    /**
     * tv端播放完成
     */
    public static final int ACTION_PLAY_COMPLETE = 0xa7;

    public static final int ACTION_POSITION_CALLBACK = 0xa8;

    private TextView tvVideoName;

    private Context mContext;
    private Handler mHandler = new InnerHandler();
    private Timer timer = null;
    private boolean isPlaying = false;
    private ClingPlayControl mClingPlayControl = new ClingPlayControl();//投屏控制器
    private AppCompatSeekBar seekBar;
    private ImageView playBt;
    private TextView currTime;
    private TextView countTime;
    private long hostLength;
    private ImageView plusVolume;
    private ImageView reduceVolume;
    private int currentVolume;
    private TextView playStatus;
    private boolean isSeeking = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        initStatusBar();
        setContentView(R.layout.activity_dlan_play_layout);
        initView();
        initListener();
        registerReceivers();
        String playUrl = getIntent().getStringExtra(DLandataInter.Key.PLAYURL);
        String playTitle = getIntent().getStringExtra(DLandataInter.Key.PLAY_TITLE);
        initData(playUrl, playTitle);
    }

    private void initStatusBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void initView() {
        tvVideoName = findViewById(R.id.text_content_title);
        playBt = findViewById(R.id.img_play);
        findViewById(R.id.backup).setOnClickListener(v -> finish());
        seekBar = findViewById(R.id.seek_bar_progress);
        currTime = findViewById(R.id.text_play_time);
        countTime = findViewById(R.id.text_play_max_time);
        playStatus = findViewById(R.id.play_status);

        plusVolume = findViewById(R.id.plus_volume);
        reduceVolume = findViewById(R.id.reduce_volume);

        getVolume();
        plusVolume.setOnClickListener(v -> {
            if (currentVolume >= 96) {
                return;
            }
            currentVolume += 4;
            mClingPlayControl.setVolume(currentVolume, new ControlCallback() {
                @Override
                public void success(IResponse response) {
                    getVolume();
                }

                @Override
                public void fail(IResponse response) {
                    getVolume();
                }
            });
        });
        reduceVolume.setOnClickListener(v -> {
            if (currentVolume <= 4) {
                return;
            }
            currentVolume -= 4;
            mClingPlayControl.setVolume(currentVolume, new ControlCallback() {
                @Override
                public void success(IResponse response) {
                    getVolume();
                }

                @Override
                public void fail(IResponse response) {

                }
            });
        });

    }

    private void getVolume() {
        mClingPlayControl.getVolume(new ControlReceiveCallback() {
            @Override
            public void receive(IResponse response) {
                Object responseResponse = response.getResponse();
                if (responseResponse instanceof ClingVolumeResponse) {
                    ClingVolumeResponse resp = (ClingVolumeResponse) response;
                    currentVolume = resp.getResponse();
                }
            }

            @Override
            public void success(IResponse response) {

            }

            @Override
            public void fail(IResponse response) {

            }
        });
    }

    private void initData(String playUrl, String playTitle) {
        tvVideoName.setText(playTitle);
        playStatus.setText("正在缓冲...");
        playNew(playUrl, playTitle);
        Log.d(TAG, "initData: playUrl==>" + playUrl + ", playTitle==>" + playTitle);
    }

    private void initListener() {
        playBt.setOnClickListener(v -> {
            if (isPlaying) {
                pause();
                playBt.setSelected(false);
                playStatus.setText("暂停播放...");
            } else {
                continuePlay();
                playBt.setSelected(true);
                playStatus.setText("正在播放");
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newProgress = (int) (hostLength * (progress * 0.01f));
                String time = ModelUtil.toTimeString(newProgress);
                currTime.setText(time);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int currentProgress = seekBar.getProgress(); // 转为毫秒
                isSeeking = false;
                int progress = (int) (hostLength * 1000 * (currentProgress * 0.01f));
                mClingPlayControl.seek(progress, new ControlCallback() {
                    @Override
                    public void success(IResponse response) {
                        Log.e(TAG, "seek success");
                    }

                    @Override
                    public void fail(IResponse response) {
                        Log.e(TAG, "seek fail");
                    }
                });
            }
        });
//

    }


    private void playNew(String url, String playTitle) {
        Log.d(TAG, "playNew start: " + url);
        mClingPlayControl.playNew(url, playTitle, new ControlCallback() {
            @Override
            public void success(IResponse response) {
                Log.d(TAG, "playNew success: ");
                isPlaying = true;
                playBt.setSelected(true);
                ClingManager.getInstance().registerAVTransport(mContext);
                ClingManager.getInstance().registerRenderingControl(mContext);
                endGetProgress();
                startGetProgress();
                playStatus.setText("正在播放");
            }

            @Override
            public void fail(IResponse response) {
                mHandler.sendEmptyMessage(ERROR_ACTION);
                Log.d(TAG, "playNew fail: ");
            }
        });
    }

    private void continuePlay() {
        mClingPlayControl.play(new ControlCallback() {
            @Override
            public void success(IResponse response) {
                isPlaying = true;
//                tvVideoStatus.setText("正在投屏中");
                Log.e(TAG, "play success");

            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "play fail");
            }
        });

    }


    /**
     * 停止
     */
    private void stop() {
        mClingPlayControl.stop(new ControlCallback() {
            @Override
            public void success(IResponse response) {
            }

            @Override
            public void fail(IResponse response) {
            }
        });
    }

    /**
     * 暂停
     */
    private void pause() {
        mClingPlayControl.pause(new ControlCallback() {
            @Override
            public void success(IResponse response) {
                isPlaying = false;
//                tvVideoStatus.setText("暂停投屏中");
            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "pause fail");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stop();
//        mPlayer.release();
        mHandler.removeCallbacksAndMessages(null);
        endGetProgress();
        unregisterReceiver(TransportStateBroadcastReceiver);
    }

    private void registerReceivers() {
        //Register play status broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_PLAYING);
        filter.addAction(Intents.ACTION_PAUSED_PLAYBACK);
        filter.addAction(Intents.ACTION_STOPPED);
        filter.addAction(Intents.ACTION_TRANSITIONING);
        filter.addAction(Intents.ACTION_POSITION_CALLBACK);
        filter.addAction(Intents.ACTION_PLAY_COMPLETE);
        registerReceiver(TransportStateBroadcastReceiver, filter);
    }


    private final class InnerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case PLAY_ACTION:
                    Log.i(TAG, "Execute PLAY_ACTION");
//                    Toast.makeText(mContext, "正在投放", Toast.LENGTH_SHORT).show();
                    startGetProgress();
                    mClingPlayControl.setCurrentState(DLANPlayState.PLAY);
                    break;
                case PAUSE_ACTION:
                    Log.i(TAG, "Execute PAUSE_ACTION");
                    mClingPlayControl.setCurrentState(DLANPlayState.PAUSE);
                    break;
                case STOP_ACTION:
                    Log.i(TAG, "Execute STOP_ACTION");
                    mClingPlayControl.setCurrentState(DLANPlayState.STOP);
//                    foot.ivPlay.setImageResource(R.drawable.icon_video_pause);
                    break;
                case TRANSITIONING_ACTION:
                    Log.i(TAG, "Execute TRANSITIONING_ACTION");
                    Toast.makeText(mContext, "正在连接", Toast.LENGTH_SHORT).show();
                    break;

                case ACTION_POSITION_CALLBACK:
//                    foot.setCurProgress(msg.arg1);
                    break;
                case ACTION_PLAY_COMPLETE:
                    Log.i(TAG, "Execute GET_POSITION_INFO_ACTION");
//                    ToastUtils.showLong("播放完成");
                    break;

                case ERROR_ACTION:
                    Log.e(TAG, "Execute ERROR_ACTION");
                    Toast.makeText(mContext, "投放失败", Toast.LENGTH_SHORT).show();
                    if (DlanListPopUtil.instance().getUsedDevice() != null) {
                        DlanListPopUtil.instance().reInit();
                    }
                    break;
            }
        }
    }

    private void startGetProgress() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                if (mClingPlayControl != null)
                    mClingPlayControl.getPositionInfo(new ControlReceiveCallback() {
                        @Override
                        public void receive(IResponse response) {
                            Log.d(TAG, "receive: " + JSON.toJSONString(response));
                            Object responseResponse = response.getResponse();
                            if (responseResponse instanceof PositionInfo) {
                                Log.d(TAG, "receive: responseResponse instanceof PositionInfo");
                                final PositionInfo positionInfo = (PositionInfo) responseResponse;
                                hostLength = positionInfo.getTrackDurationSeconds();
                                runOnUiThread(() -> {
                                    if (!isSeeking) {
                                        countTime.setText((positionInfo.getTrackDuration() + ""));
                                        currTime.setText((positionInfo.getRelTime() + ""));
                                        seekBar.setProgress(positionInfo.getElapsedPercent());
                                    }
                                });

                            }
                        }

                        @Override
                        public void success(IResponse response) {

                        }

                        @Override
                        public void fail(IResponse response) {

                        }
                    });

            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 1, 3000);
    }

    public static int timeToSec(String time) {
        String[] timeArray = time.split(":");
        int hour = Integer.parseInt(timeArray[0]) * 3600;
        int min = Integer.parseInt(timeArray[1]) * 60;
        int sec = Integer.parseInt(timeArray[2]);
        return (hour + min + sec) * 1000;
    }


    private void endGetProgress() {
        if (timer != null)
            timer.cancel();
        timer = null;
    }

    /**
     * 接收状态改变信息
     */
    BroadcastReceiver TransportStateBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(TAG, "Receive playback intent:" + action);
            if (Intents.ACTION_PLAYING.equals(action)) {
                mHandler.sendEmptyMessage(PLAY_ACTION);

            } else if (Intents.ACTION_PAUSED_PLAYBACK.equals(action)) {
                mHandler.sendEmptyMessage(PAUSE_ACTION);

            } else if (Intents.ACTION_STOPPED.equals(action)) {
                mHandler.sendEmptyMessage(STOP_ACTION);

            } else if (Intents.ACTION_TRANSITIONING.equals(action)) {
                mHandler.sendEmptyMessage(TRANSITIONING_ACTION);
            } else if (Intents.ACTION_POSITION_CALLBACK.equals(action)) {
                Message msg = Message.obtain();
                msg.what = ACTION_POSITION_CALLBACK;
                msg.arg1 = intent.getIntExtra(Intents.EXTRA_POSITION, -1);
                mHandler.sendMessage(msg);
            } else if (Intents.ACTION_PLAY_COMPLETE.equals(action)) {
                mHandler.sendEmptyMessage(ACTION_PLAY_COMPLETE);
            }
        }
    };
}