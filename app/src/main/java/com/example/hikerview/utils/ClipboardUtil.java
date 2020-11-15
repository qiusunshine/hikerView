package com.example.hikerview.utils;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.example.hikerview.event.ClearDetectedEvent;

import org.greenrobot.eventbus.EventBus;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * 作者：By hdy
 * 日期：On 2017/11/5
 * 时间：At 8:59
 */

public class ClipboardUtil {
    private static final String TAG = "ClipboardUtil";

    /**
     * 获取剪贴板
     *
     * @param context
     * @return
     */
    public static void getText(Context context, View v, ClipListener clipListener) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            v.postDelayed(() -> {
                EditText et = new EditText(context);
                et.requestFocus();
                et.setOnClickListener(view -> clipListener.hasText(getTextNow(context)));
                et.performClick();
            }, 1000);
        } else {
            clipListener.hasText(getTextNow(context));
        }
    }

    /**
     * 获取剪贴板
     *
     * @param context
     * @return
     */
    public static void getTextNoDelay(Context context, ClipListener clipListener) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            EditText et = new EditText(context);
            et.requestFocus();
            et.setOnClickListener(view -> clipListener.hasText(getTextNow(context)));
            et.performClick();
        } else {
            clipListener.hasText(getTextNow(context));
        }
    }

    /**
     * 获取剪贴板
     *
     * @param context
     * @return
     */
    public static String getTextNow(Context context) {
        String text = "";
        try {
            text = tryGetTextNow(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Log.d(TAG, "getTextNow: "+ text);
        return text;
    }

    public static String tryGetTextNow(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        //无数据时直接返回
        if (clipboard != null && !clipboard.hasPrimaryClip()) {
            return "";
        }
        //如果是文本信息
//        Log.d(TAG, "tryGetTextNow: " + JSON.toJSONString(clipboard == null ? "" : clipboard.getPrimaryClipDescription().getMimeType(0)));
        if (clipboard != null && clipboard.getPrimaryClipDescription() != null && (clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))) {
            ClipData cdText = clipboard.getPrimaryClip();
//            Log.d(TAG, "tryGetTextNow: "+ JSON.toJSONString(cdText.getItemAt(0)));
            ClipData.Item item = null;
            if (cdText != null) {
                item = cdText.getItemAt(0);
            }
            //此处是TEXT文本信息
            if (item != null) {
                if (item.getText() == null) {
                    return "";
                } else {
                    return item.getText().toString();
                }
            }
        }
        return "";
    }

    public static boolean copyToClipboard(Context context, String str) {
        return copyToClipboard(context, str, true);
    }

    public static boolean copyToClipboard(Context context, String str, boolean showToast) {
        if ("".equals(str)) {
            EventBus.getDefault().post(new ClearDetectedEvent());
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            EditText et = new EditText(context);
            et.requestFocus();
            final String s = str;
            et.setOnClickListener(view -> {
                String str1 = s;
                ClipboardManager clipManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (str1.length() > 1024) {
                    str1 = str1.substring(0, 1024);
                }
                ClipData mClipData = ClipData.newPlainText("Label", str1);
                if (clipManager != null) {
                    clipManager.setPrimaryClip(mClipData);
                    if (!TextUtils.isEmpty(str1) && showToast) {
                        ToastMgr.shortBottomCenter(context, "复制成功");
                    }
                }
            });
            et.performClick();
            return true;
        }
        ClipboardManager clipManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (str == null) {
            return false;
        }
        if (str.length() > 1024) {
            str = str.substring(0, 1024);
        }
        ClipData mClipData = ClipData.newPlainText("Label", str);
        if (clipManager != null) {
            clipManager.setPrimaryClip(mClipData);
            if (!TextUtils.isEmpty(str) && showToast) {
                ToastMgr.shortBottomCenter(context, "复制成功");
            }
        }
        return true;
    }

    public static boolean copyToClipboardForce(Context context, String str) {
        return copyToClipboardForce(context, str, true);
    }

    public static boolean copyToClipboardForce(Context context, String str, boolean showToast) {
        if ("".equals(str)) {
            EventBus.getDefault().post(new ClearDetectedEvent());
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            EditText et = new EditText(context);
            et.requestFocus();
            final String s = str;
            et.setOnClickListener(view -> {
                ClipboardManager clipManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("Label", s);
                if (clipManager != null) {
                    clipManager.setPrimaryClip(mClipData);
                    if (!TextUtils.isEmpty(s) && showToast) {
                        ToastMgr.shortBottomCenter(context, "复制成功");
                    }
                }
            });
            et.performClick();
            return true;
        }
        ClipboardManager clipManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (str == null) {
            return false;
        }
        ClipData mClipData = ClipData.newPlainText("Label", str);
        if (clipManager != null) {
            clipManager.setPrimaryClip(mClipData);
            if (!TextUtils.isEmpty(str) && showToast) {
                ToastMgr.shortBottomCenter(context, "复制成功");
            }
        }
        return true;
    }

    public interface ClipListener {
        void hasText(String text);
    }
}


