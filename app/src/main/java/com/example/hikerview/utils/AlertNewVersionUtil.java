package com.example.hikerview.utils;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hikerview.R;
import com.example.hikerview.ui.browser.WebViewActivity;
import com.example.hikerview.ui.home.ArticleListRuleMagActivity;
import com.example.hikerview.ui.home.MainActivity;
import com.example.hikerview.ui.search.SearchActivity;
import com.example.hikerview.ui.setting.ProtocolPopup;
import com.example.hikerview.ui.setting.model.SettingConfig;

import it.sephiroth.android.library.xtooltip.ClosePolicy;
import it.sephiroth.android.library.xtooltip.Tooltip;

/**
 * 作者：By 15968
 * 日期：On 2020/4/6
 * 时间：At 20:46
 */
public class AlertNewVersionUtil {

    /**
     * 显示用户提示
     *
     * @param activity
     * @return true：显示了，false：没显示
     */
    public static boolean alert(AppCompatActivity activity) {
        if (SettingConfig.professionalMode) {
            return false;
        }
        int nowVersion = PreferenceMgr.getInt(activity, "version", activity.getClass().getSimpleName(), 0);
        int newVersion;
        if (activity instanceof MainActivity) {
            int nowAppVersion = PreferenceMgr.getInt(activity, "version", "hiker", 0);
            int newAppVersion = 5;
            if (newAppVersion > nowAppVersion) {
                ProtocolPopup.showProtocolPopup(activity, newAppVersion);
            }
            newVersion = 8;
            if (newVersion > nowVersion) {
                PreferenceMgr.put(activity, "version", activity.getClass().getSimpleName(), newVersion);
                View button = activity.findViewById(R.id.bottom_bbs);
                button.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(button, 0, 0, false)
                            .text("长按打开我的小程序页面")
                            .showDuration(newAppVersion > nowAppVersion ? 15000L : 10000L)
                            .create().show(button, Tooltip.Gravity.TOP, true);
                });

                View home_header_tab = activity.findViewById(R.id.home_header_tab);
                home_header_tab.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(home_header_tab, 0, 0, false)
                            .text("长按搜索频道、管理规则")
                            .showDuration(newAppVersion > nowAppVersion ? 15000L : 10000L)
                            .create().show(home_header_tab, Tooltip.Gravity.BOTTOM, true);
                });
                return true;
            }
        } else if (activity instanceof WebViewActivity) {
            newVersion = 8;
            if (newVersion > nowVersion) {
                PreferenceMgr.put(activity, "version", activity.getClass().getSimpleName(), newVersion);
                View bottom_bar_refresh = activity.findViewById(R.id.bottom_bar_refresh_card);
                bottom_bar_refresh.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(bottom_bar_refresh, 0, 0, false)
                            .text("点击刷新")
                            .showDuration(10000L)
                            .create().show(bottom_bar_refresh, Tooltip.Gravity.TOP, true);
                });

                View bottom_bar_title_card = activity.findViewById(R.id.bottom_bar_title_card);
                bottom_bar_title_card.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(bottom_bar_title_card, 0, 0, false)
                            .text("按住上拉回首页")
                            .showDuration(10000L)
                            .create().show(bottom_bar_title_card, Tooltip.Gravity.TOP, true);
                });
                return true;
            }
        } else if (activity instanceof ArticleListRuleMagActivity) {
            newVersion = 4;
            if (newVersion > nowVersion) {
                PreferenceMgr.put(activity, "version", activity.getClass().getSimpleName(), newVersion);
                View bottom_recycler_view = activity.findViewById(R.id.article_list_rule_bottom_recycler_view);
                bottom_recycler_view.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(bottom_recycler_view, 0, 0, false)
                            .text("点击切换分组、长按管理分组")
                            .showDuration(10000L)
                            .create().show(bottom_recycler_view, Tooltip.Gravity.BOTTOM, true);
                });

                View menu_icon = activity.findViewById(R.id.menu_icon);
                menu_icon.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(menu_icon, 0, 0, false)
                            .text("这里校验失效源和查看规则统计")
                            .showDuration(10000L)
                            .create().show(menu_icon, Tooltip.Gravity.TOP, true);
                });
                return true;
            }
        } else if (activity instanceof SearchActivity) {
            newVersion = 1;
            if (newVersion > nowVersion) {
                PreferenceMgr.put(activity, "version", activity.getClass().getSimpleName(), newVersion);
                View bottom_recycler_view = activity.findViewById(R.id.search_result_view_pager);
                bottom_recycler_view.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(bottom_recycler_view, 0, 0, false)
                            .text("左右滑动即可切换页面")
                            .showDuration(10000L)
                            .create().show(bottom_recycler_view, Tooltip.Gravity.CENTER, true);
                });

                View menu_icon = activity.findViewById(R.id.dropDownMenu);
                menu_icon.post(() -> {
                    new Tooltip.Builder(activity)
                            .styleId(R.style.MyTooltip)
                            .floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                            .closePolicy(ClosePolicy.Companion.getTOUCH_ANYWHERE_CONSUME())
                            .anchor(menu_icon, 0, 0, false)
                            .text("这里切换规则分组")
                            .showDuration(10000L)
                            .create().show(menu_icon, Tooltip.Gravity.LEFT, true);
                });
                return true;
            }
        }
        return false;
    }
}
