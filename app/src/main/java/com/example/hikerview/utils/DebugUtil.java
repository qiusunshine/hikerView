package com.example.hikerview.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.hikerview.R;
import com.example.hikerview.event.OnArticleListRuleChangedEvent;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.ui.view.CustomBottomPopup;
import com.lxj.xpopup.XPopup;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

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
            CustomBottomPopup popup = new CustomBottomPopup(activity, title).addOnClickListener(v -> {
                StringBuilder builder = new StringBuilder();
                builder.append("错误：").append(title);
                smartCheck(builder, msg);
                builder.append("\n描述：").append(msg)
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
                AlertDialog.Builder builder1 = new AlertDialog.Builder(context).setTitle("错误详细信息")
                        .setView(et)
                        .setCancelable(true)
                        .setPositiveButton("报告作者", (dialog, which) -> {
                            if (!ClipboardUtil.copyToClipboard(context, msg)) {
                                ToastMgr.shortBottomCenter(context, "复制失败");
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("忽略", (dialog, which) -> dialog.dismiss());
                if (StringUtil.isNotEmpty(code) && code.startsWith("home@")) {
                    builder1.setNeutralButton("删除规则", (dialog, which) -> {
                        delRuleByCode(context, code);
                    });
                }
                builder1.show();
            });
            new XPopup.Builder(activity)
                    .hasShadowBg(false)
                    .asCustom(popup)
                    .show()
                    .delayDismiss(5000);
//            ViewGroup viewGroup = (ViewGroup) activity.findViewById(android.R.id.content).getRootView();
//            TSnackbar.make(viewGroup, "出现错误，是否查看错误？", TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_TOP_TO_DOWN)
//                    .setPromptThemBackground(Prompt.WARNING)
//                    .setAction("查看", v1 -> {
//
//                    }).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void delRuleByCode(Context context, String code) {
        String rule = StringUtils.replaceOnce(code, "home@", "");
        ArticleListRule articleListRule = LitePal.where("title = ?", rule).findFirst(ArticleListRule.class);
        if (articleListRule == null) {
            ToastMgr.shortBottomCenter(context, "找不到规则：" + rule);
            return;
        }
        if (SettingConfig.homeName.equals(rule)) {
            ToastMgr.shortCenter(context, "当前规则为默认主页，不能删除");
            return;
        }
        new XPopup.Builder(context)
                .asConfirm("温馨提示", "确定删除规则‘" + rule + "’吗？注意删除后无法恢复！", () -> {
                    articleListRule.delete();
                    EventBus.getDefault().post(new OnArticleListRuleChangedEvent());
                    ToastMgr.shortBottomCenter(context, "已将" + articleListRule.getTitle() + "删除");
                }).show();
    }

    private static void smartCheck(StringBuilder builder, String msg) {
        if (StringUtil.isNotEmpty(msg)) {
            if (msg.contains("syntax error") || msg.contains("is not defined") || msg.contains("JSEngine")) {
                builder.append("\n提示：JS语法错误，请联系规则作者");
            } else if (msg.contains("http response code is") || msg.contains("HttpRequestError")) {
                builder.append("\n提示：疑似网站已关闭或者规则已失效，建议删除规则或者联系规则作者");
            } else if (msg.contains("org.jsoup.")) {
                builder.append("\n提示：规则已失效，请联系规则作者");
            }
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
