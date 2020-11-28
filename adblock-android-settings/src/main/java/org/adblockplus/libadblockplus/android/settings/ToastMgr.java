package org.adblockplus.libadblockplus.android.settings;

/**
 * 作者：By hdy
 * 日期：On 2017/11/6
 * 时间：At 16:51
 */

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class ToastMgr {
    private static Toast sToast, centerToast;

    public static void shortBottomCenter(Context context, String message) {
        try {
            if (sToast == null) {
                View v = Toast.makeText(context, "", Toast.LENGTH_SHORT).getView();
                sToast = new Toast(context);
                sToast.setView(v);
            }
            sToast.setText(message);
            sToast.show();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void shortCenter(Context context, String message) {
        try {
            if (centerToast == null) {
                View v = Toast.makeText(context, "", Toast.LENGTH_SHORT).getView();
                centerToast = new Toast(context);
                centerToast.setView(v);
                centerToast.setGravity(Gravity.CENTER, 0, 0);
            }
            centerToast.setText(message);
            centerToast.show();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
}
