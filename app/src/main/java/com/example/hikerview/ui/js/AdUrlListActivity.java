package com.example.hikerview.ui.js;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikerview.R;
import com.example.hikerview.model.AdBlockUrl;
import com.example.hikerview.ui.base.BaseSlideActivity;
import com.example.hikerview.ui.browser.model.AdUrlBlocker;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.utils.AutoImportHelper;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.MyStatusBarUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 10:09
 */
public class AdUrlListActivity extends BaseSlideActivity {
    private RecyclerView recyclerView;
    private AdUrlListAdapter adapter;
    private List<AdBlockUrl> rules = new ArrayList<>();

    @Override
    protected View getBackgroundView() {
        return findView(R.id.ad_list_window);
    }

    @Override
    protected int initLayout(Bundle savedInstanceState) {
        return R.layout.activit_ad_url_list;
    }

    @Override
    protected void initView2() {
        recyclerView = findView(R.id.ad_list_recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        ((TextView) findView(R.id.ad_list_title_text)).setText("网址过滤（域名拦截）");
        View addBtn = findView(R.id.ad_list_add);
        addBtn.setOnClickListener(addBtnListener);
        //初始化高度
        int marginTop = MyStatusBarUtil.getStatusBarHeight(getContext()) + DisplayUtil.dpToPx(getContext(), 86);
        View bg = findView(R.id.ad_list_bg);
        findView(R.id.ad_list_window).setOnClickListener(view -> finish());
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bg.getLayoutParams();
        layoutParams.topMargin = marginTop;
        bg.setLayoutParams(layoutParams);

        findView(R.id.ad_list_share).setOnClickListener(v -> shareRules());
    }

    private void shareRules() {
        new XPopup.Builder(getContext())
                .asCenterList("请选择操作", new String[]{"分享最近10条规则", "分享最近20条规则", "分享最近50条规则", "分享最近100条规则", "分享最近1000条规则"},
                        ((option, text) -> {
                            switch (text) {
                                case "分享最近10条规则":
                                    shareLatestRulesByCount(10);
                                    break;
                                case "分享最近20条规则":
                                    shareLatestRulesByCount(20);
                                    break;
                                case "分享最近50条规则":
                                    shareLatestRulesByCount(50);
                                    break;
                                case "分享最近100条规则":
                                    shareLatestRulesByCount(100);
                                    break;
                                case "分享最近1000条规则":
                                    shareLatestRulesByCount(1000);
                                    break;
                            }
                        }))
                .show();
    }

    private void shareLatestRulesByCount(int count) {
        if (count > rules.size()) {
            count = rules.size();
        }
        if (count > 1000) {
            count = 1000;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count - 1; i++) {
            builder.append(rules.get(i).getUrl()).append("&&");
        }
        if (count > 0) {
            builder.append(rules.get(count - 1));
        }
        AutoImportHelper.shareWithCommand(getContext(), builder.toString(), AutoImportHelper.AD_URL_RULES);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        adapter = new AdUrlListAdapter(getContext(), rules);
        adapter.setOnItemClickListener(onItemClickListener);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);

        String rulesText = getIntent().getStringExtra("data");
        addRules(rulesText);
        //异步加载
        HeavyTaskUtil.executeNewTask(() -> {
            List<AdBlockUrl> adBlockRules = AdUrlBlocker.instance().getBlockUrls();
            if (!CollectionUtil.isEmpty(adBlockRules)) {
                rules.addAll(adBlockRules);
                Collections.sort(rules, (o1, o2) -> (int) (o2.getId() - o1.getId()));
            }
            runOnUiThread(()-> adapter.notifyDataSetChanged());
        });
    }

    private AdUrlListAdapter.OnItemClickListener onItemClickListener = new AdUrlListAdapter.OnItemClickListener() {
        @Override
        public void onDelete(View view, int position, AdBlockUrl adBlockRule) {
            AdUrlBlocker.instance().removeUrl(adBlockRule.getUrl());
            rules.remove(position);
            adapter.notifyItemRemoved(position);
        }

        @Override
        public void onLongClick(View view, int position, AdBlockUrl adBlockRule) {
            new XPopup.Builder(getContext())
                    .asCenterList("请选择操作", new String[]{"分享规则", "删除规则", "删除全部"},
                            ((option, text) -> {
                                switch (option) {
                                    case 0:
                                        AutoImportHelper.shareWithCommand(getContext(), adBlockRule.getUrl(), AutoImportHelper.AD_URL_RULES);
                                        break;
                                    case 1:
                                        AdUrlBlocker.instance().removeUrl(adBlockRule.getUrl());
                                        rules.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        ToastMgr.shortBottomCenter(getContext(), "已删除规则，但是网站附带的拦截规则需要到网络日志内更新");
                                        break;
                                    case 2:
                                        AdUrlBlocker.instance().removeAll();
                                        rules.clear();
                                        adapter.notifyDataSetChanged();
                                        ToastMgr.shortBottomCenter(getContext(), "已删除全部规则，但是网站附带的拦截规则需要到网络日志内更新");
                                        break;
                                }
                            }))
                    .show();
        }
    };

    private void addRules(String rulesText) {
        if (StringUtil.isEmpty(rulesText)) {
            return;
        }
        addRulesForce(rulesText);
    }

    private void addRulesForce(String rulesText) {
        if (StringUtil.isNotEmpty(rulesText) && rulesText.trim().startsWith("海阔视界")) {
            String[] sss = rulesText.trim().split("￥");
            if (sss.length == 3) {
                rulesText = sss[2];
            }
        }
        String hint = "支持&&分隔多条，最多同时导入1000条，重复导入会自动过滤";
        new XPopup.Builder(getContext()).asInputConfirm("新增网址过滤", hint, rulesText, hint,
                text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastMgr.shortBottomCenter(getContext(), "规则不能为空");
                        return;
                    }
                    List<String> urls = Arrays.asList(text.split("&&"));
                    //检查前两条规则，长度太长应该是乱导入的
                    if (urls.size() > 0) {
                        if (urls.get(0).length() > 100) {
                            ToastMgr.shortBottomCenter(getContext(), "单条规则长度大于100，请检查是否是网址过滤规则");
                            return;
                        }
                    }
                    if (urls.size() > 1) {
                        if (urls.get(1).length() > 100) {
                            ToastMgr.shortBottomCenter(getContext(), "单条规则长度大于100，请检查是否是网址过滤规则");
                            return;
                        }
                    }
                    LoadingPopupView loadingPopupView = new XPopup.Builder(getContext()).asLoading("正在导入中");
                    loadingPopupView.show();
                    HeavyTaskUtil.executeNewTask(() -> {
                        int count = AdUrlBlocker.instance().addUrls(urls);
                        runOnUiThread(() -> {
                            loadingPopupView.dismiss();
                            ToastMgr.shortBottomCenter(getContext(), "成功导入" + count + "条规则");
                            List<AdBlockUrl> adBlockRules = AdUrlBlocker.instance().getBlockUrls();
                            rules.clear();
                            if (!CollectionUtil.isEmpty(adBlockRules)) {
                                rules.addAll(adBlockRules);
                                Collections.sort(rules, (o1, o2) -> (int) (o2.getId() - o1.getId()));
                            }
                            adapter.notifyDataSetChanged();
                        });
                    });
                })
                .show();
    }

    private View.OnClickListener addBtnListener = v -> addRulesForce(null);
}
