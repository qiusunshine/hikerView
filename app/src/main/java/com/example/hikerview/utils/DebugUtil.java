package com.example.hikerview.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.hikerview.R;
import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 作者：By hdy
 * 日期：On 2018/1/22
 * 时间：At 18:20
 */

public class DebugUtil {
    public static void showErrorMsg(Activity context, Throwable ex) {
        showErrorMsg(context, context, ex.getMessage(), ex.getMessage(), ex.getMessage(), ex);
    }

    public static void showErrorMsg(Activity context, String msg, Throwable ex) {
        showErrorMsg(context, context, msg, ex.getMessage(), ex.getMessage(), ex);
    }

    public static String getErrorMsg(String title, String msg, String code, Throwable ex) {
        StringBuilder builder = new StringBuilder();
        builder.append("错误：").append(title)
                .append("   错误码：").append(code)
                .append("   描述：").append(msg)
                .append("   堆栈：");
        if (ex != null) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            Throwable cause = ex.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.close();
            String result = writer.toString();
            builder.append(result);
        }
        return builder.toString();
    }

    public static void showErrorMsg(Activity activity, Context context, String title, String msg, String code, Throwable ex) {
        try {
            ViewGroup viewGroup = (ViewGroup) activity.findViewById(android.R.id.content).getRootView();
            TSnackbar.make(viewGroup, "出现错误，是否查看错误？", TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_TOP_TO_DOWN)
                    .setPromptThemBackground(Prompt.WARNING)
                    .setAction("查看", v1 -> {
                        StringBuilder builder = new StringBuilder();
                        builder.append("错误：").append(title)
                                .append("\n错误码：").append(code)
                                .append("\n描述：").append(msg)
                                .append("\n堆栈：");
                        if (ex != null) {
                            Writer writer = new StringWriter();
                            PrintWriter printWriter = new PrintWriter(writer);
                            ex.printStackTrace(printWriter);
                            Throwable cause = ex.getCause();
                            while (cause != null) {
                                cause.printStackTrace(printWriter);
                                cause = cause.getCause();
                            }
                            printWriter.close();
                            String result = writer.toString();
                            builder.append(result);
                        }
                        String text = builder.toString();
                        final View et = LayoutInflater.from(context).inflate(R.layout.view_scroll_text, null);
                        TextView textView = et.findViewById(R.id.get_html_txt);
                        textView.setText(text);
                        new AlertDialog.Builder(context).setTitle("错误详细信息")
                                .setView(et)
                                .setCancelable(true)
                                .setPositiveButton("报告作者", (dialog, which) -> {
                                    if (!ClipboardUtil.copyToClipboard(context, msg)) {
                                        ToastMgr.shortBottomCenter(context, "复制失败");
                                    }
                                    dialog.dismiss();
                                })
                                .setNegativeButton("忽略", (dialog, which) -> dialog.dismiss())
                                .show();
                    }).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showErrorAllMsg(final Context context, final String msg) {
        try {
            final View et = LayoutInflater.from(context).inflate(R.layout.view_scroll_text, null);
            TextView textView = et.findViewById(R.id.get_html_txt);
            textView.setText(msg);
            new AlertDialog.Builder(context).setTitle("出现错误")
                    .setView(et)
                    .setCancelable(true)
                    .setPositiveButton("报告作者", (dialog, which) -> {
                        if (!ClipboardUtil.copyToClipboard(context, msg)) {
                            ToastMgr.shortBottomCenter(context, "复制失败");
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("忽略", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static StackTraceElement getCallerStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    private static String generateTag(StackTraceElement caller) {
        String tag = "%s.%s(Line:%d)"; // 占位符
        String callerClazzName = caller.getClassName(); // 获取到类名
//        callerClazzName = callerClazzName.substring(callerClazzName
//                .lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(),
                caller.getLineNumber()); // 替换
        String customTagPrefix = "at";
        tag = TextUtils.isEmpty(customTagPrefix) ? tag : customTagPrefix + ":"
                + tag;
        return tag;
    }


}
