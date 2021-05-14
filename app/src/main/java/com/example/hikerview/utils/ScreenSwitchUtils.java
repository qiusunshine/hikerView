package com.example.hikerview.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * 作者：By hdy
 * 日期：On 2018/12/15
 * 时间：At 16:52
 */
public class ScreenSwitchUtils {

//    private volatile static ScreenSwitchUtils mInstance;

    private WeakReference<Activity> mActivity;

    // 是否是竖屏
    private boolean isPortrait = true;

    private SensorManager sm;
    private OrientationSensorListener listener;
    private Sensor sensor;

    private SensorManager sm1;
    private Sensor sensor1;
    private boolean isLocked;
    private boolean isTempLocked;
    private OrientationSensorListener1 listener1;

    public boolean isLocked() {
        if (this.isTempLocked()) {
            return true;
        }
        return this.isLocked;
    }

    public void reverseLocked(Activity activity) {
        if (!isLocked) {
            //锁住，去掉监听
            stopListen();
        } else {
            startListen(activity);
        }
        this.isLocked = !isLocked;
        PreferenceMgr.put(activity, "ijkplayer", "isScreenLocked", isLocked);
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    public ScreenSwitchUtils(Context context, final boolean locked) {
        isLocked = locked;
        try {
            // 注册重力感应器,监听屏幕旋转
            sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Handler mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 888:
                            int orientation = msg.arg1;
                            if (orientation > 55 && orientation < 125) {
//                            if (orientation > 45 && orientation < 135) {
                                if (!isLocked()) {
                                    try {
                                        if (mActivity.get() != null) {
                                            mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                isPortrait = false;
                            } else if (orientation > 125 && orientation < 235) {
//                            } else if (orientation > 135 && orientation < 225) {
//                            if (!isLocked()) {
//                                try {
//                                    if (mActivity.get() != null) {
//                                        mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
                                //横屏转竖屏
//                                    if (!isLocked && orientation > 150 && orientation < 210) {
//                                        try {
//                                            if (mActivity.get() != null) {
//                                                mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//                                            }
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                    isPortrait = true;
                            } else if (orientation > 235 && orientation < 305) {
//                            } else if (orientation > 225 && orientation < 315) {
                                if (!isLocked()) {
                                    try {
                                        if (mActivity.get() != null) {
                                            mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                isPortrait = false;
                            } else if ((orientation > 305 && orientation < 360) || (orientation > 0 && orientation < 55)) {
//                            } else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {
//                            if (!isLocked()) {/
//                                try {
//                                    if (mActivity.get() != null) {
//                                        mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
                                //横屏转竖屏
//                                    if((orientation > 330 && orientation < 360) || (orientation > 0 && orientation < 30)) {
//                                        if (!isLocked) {
//                                            try {
//                                                if (mActivity.get() != null) {
//                                                    mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                                                }
//                                            } catch (Exception e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    }
//                                    isPortrait = true;
                            }
                            break;
                        default:
                            break;
                    }

                }
            };
            listener = new OrientationSensorListener(mHandler);

            // 根据 旋转之后/点击全屏之后 两者方向一致,激活sm.
            sm1 = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            sensor1 = sm1.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            listener1 = new OrientationSensorListener1();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始监听
     */
    public void onResume(Activity activity) {
        if (!isLocked) {
            startListen(activity);
        }
    }

    /**
     * 停止监听
     */
    public void onPause() {
        try {
            stopListen();
        } catch (Exception ignored) {
        }
    }

    private void startListen(Activity activity) {
        try {
            mActivity = new WeakReference<>(activity);
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
        } catch (Exception ignored) {
        }
    }

    /**
     * 停止监听
     */
    public void stopListen() {
        try {
            sm.unregisterListener(listener);
            sm1.unregisterListener(listener1);
        } catch (Exception ignored) {
        }
    }

    /**
     * 手动横竖屏切换方向
     */
    public void toggleScreen() {
        sm.unregisterListener(listener);
        sm1.registerListener(listener1, sensor1, SensorManager.SENSOR_DELAY_UI);
        if (isPortrait) {
            isPortrait = false;
            // 切换成横屏
            if (mActivity.get() != null) {
                mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            isPortrait = true;
            // 切换成竖屏
            if (mActivity.get() != null) {
                mActivity.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    public boolean isPortrait() {
        return this.isPortrait;
    }

    public boolean isTempLocked() {
        return isTempLocked;
    }

    public void setTempLocked(boolean tempLocked) {
        isTempLocked = tempLocked;
    }

    /**
     * 重力感应监听者
     */
    public class OrientationSensorListener implements SensorEventListener {
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;

        public static final int ORIENTATION_UNKNOWN = -1;

        private Handler rotateHandler;

        public OrientationSensorListener(Handler handler) {
            rotateHandler = handler;
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X * X + Y * Y;
            // Don't trust the angle if the magnitude is small compared to the y
            // value
            if (magnitude * 4 >= Z * Z) {
                // 屏幕旋转时
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - (int) Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }
            if (rotateHandler != null) {
                rotateHandler.obtainMessage(888, orientation, 0).sendToTarget();
            }
        }
    }

    public class OrientationSensorListener1 implements SensorEventListener {
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;

        public static final int ORIENTATION_UNKNOWN = -1;

        public OrientationSensorListener1() {
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X * X + Y * Y;
            // Don't trust the angle if the magnitude is small compared to the y
            // value
            if (magnitude * 4 >= Z * Z) {
                // 屏幕旋转时
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - (int) Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }
            if (orientation > 225 && orientation < 315) {// 检测到当前实际是横屏
                if (!isPortrait) {
                    sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
                    sm1.unregisterListener(listener1);
                }
            } else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {// 检测到当前实际是竖屏
                if (isPortrait) {
                    sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
                    sm1.unregisterListener(listener1);
                }
            }
        }
    }
}
