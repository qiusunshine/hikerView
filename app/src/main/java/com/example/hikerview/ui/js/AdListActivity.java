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
import com.example.hikerview.model.AdBlockRule;
import com.example.hikerview.ui.base.BaseSlideActivity;
import com.example.hikerview.ui.browser.model.AdBlockModel;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.view.DialogBuilder;
import com.example.hikerview.utils.AutoImportHelper;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.MyStatusBarUtil;
import com.example.hikerview.utils.ToastMgr;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 10:09
 */
public class AdListActivity extends BaseSlideActivity {
    private RecyclerView recyclerView;
    private AdListAdapter adapter;
    private List<AdBlockRule> rules = new ArrayList<>();

    @Override
    protected View getBackgroundView() {
        return findView(R.id.ad_list_window);
    }

    @Override
    protected int initLayout(Bundle savedInstanceState) {
        return R.layout.activit_ad_list;
    }

    @Override
    protected void initView2() {
        recyclerView = findView(R.id.ad_list_recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        ((TextView) findView(R.id.ad_list_title_text)).setText("拦截广告（图片等元素）");
        View addBtn = findView(R.id.ad_list_add);
        addBtn.setOnClickListener(addBtnListener);
        //初始化高度
        int marginTop = MyStatusBarUtil.getStatusBarHeight(getContext()) + DisplayUtil.dpToPx(getContext(), 86);
        View bg = findView(R.id.ad_list_bg);
        findView(R.id.ad_list_window).setOnClickListener(view -> finish());
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bg.getLayoutParams();
        layoutParams.topMargin = marginTop;
        bg.setLayoutParams(layoutParams);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        List<AdBlockRule> adBlockRules = null;
        try {
            adBlockRules = LitePal.findAll(AdBlockRule.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtil.isEmpty(adBlockRules)) {
            rules.addAll(adBlockRules);
            Collections.sort(rules, (o1, o2) -> (int) (o2.getId() - o1.getId()));
        }
        adapter = new AdListAdapter(getContext(), rules);
        adapter.setOnItemClickListener(onItemClickListener);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    private AdListAdapter.OnItemClickListener onItemClickListener = new AdListAdapter.OnItemClickListener() {
        @Override
        public void onDelete(View view, int position, AdBlockRule adBlockRule) {
            if (adBlockRule == null) {
                return;
            }
            adBlockRule.delete();
            rules.remove(position);
            adapter.notifyItemRemoved(position);
        }

        @Override
        public void onClick(View view, int position, AdBlockRule adBlockRule) {
            if (adBlockRule == null) {
                return;
            }
            DialogBuilder.createInputConfirm(getContext(), "编辑拦截规则", adBlockRule.getRule(), text1 -> {
                if (TextUtils.isEmpty(text1)) {
                    ToastMgr.shortBottomCenter(getContext(), "规则不能为空");
                } else {
                    adBlockRule.setRule(text1);
                    adBlockRule.save();
                    ToastMgr.shortBottomCenter(getContext(), "已保存规则");
                }
            }).show();
        }

        @Override
        public void onLongClick(View view, int position, AdBlockRule adBlockRule) {
            if (adBlockRule == null) {
                return;
            }
            new XPopup.Builder(getContext())
                    .asCenterList("请选择操作", new String[]{"分享规则", "分享全部", "编辑规则", "删除规则"},
                            ((option, text) -> {
                                switch (option) {
                                    case 0:
                                        AutoImportHelper.shareWithCommand(getContext(), adBlockRule.getDom() + "::" + adBlockRule.getRule(), AutoImportHelper.AD_BLOCK_RULES);
                                        break;
                                    case 1:
                                        int size = rules.size();
                                        if (size > 1000) {
                                            size = 1000;
                                        }
                                        StringBuilder builder = new StringBuilder();
                                        for (int i = 0; i < size - 1; i++) {
                                            builder.append(rules.get(i).getDom()).append("::").append(rules.get(i).getRule()).append("##");
                                        }
                                        if (size > 0) {
                                            builder.append(rules.get(size - 1).getDom()).append("::").append(rules.get(size - 1).getRule());
                                        }
                                        ClipboardUtil.copyToClipboardForce(getContext(), builder.toString());
                                        break;
                                    case 2:
                                        DialogBuilder.createInputConfirm(getContext(), "编辑拦截规则", adBlockRule.getRule(), text1 -> {
                                            if (TextUtils.isEmpty(text1)) {
                                                ToastMgr.shortBottomCenter(getContext(), "规则不能为空");
                                            } else {
                                                adBlockRule.setRule(text1);
                                                adBlockRule.save();
                                                ToastMgr.shortBottomCenter(getContext(), "已保存规则");
                                            }
                                        }).show();
                                        break;
                                    case 3:
                                        adBlockRule.delete();
                                        rules.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        ToastMgr.shortBottomCenter(getContext(), "已删除规则");
                                        break;
                                }
                            }))
                    .show();
        }
    };

    private View.OnClickListener addBtnListener = v ->
            new XPopup.Builder(getContext()).asInputConfirm("新增广告拦截", "支持##分隔多条，最多同时导入1000条",
                    text -> {
                        if (TextUtils.isEmpty(text)) {
                            ToastMgr.shortBottomCenter(getContext(), "规则不能为空");
                            return;
                        }
                        String[] s = text.split("##");
                        List<AdBlockRule> adBlockRules = new ArrayList<>();
                        for (String s1 : s) {
                            String[] k = s1.split("::");
                            if (k.length != 2) {
                                continue;
                            }
                            if (TextUtils.isEmpty(k[0]) || TextUtils.isEmpty(k[1])) {
                                continue;
                            }
                            AdBlockRule rule = new AdBlockRule();
                            rule.setDom(k[0]);
                            rule.setRule(k[1]);
                            adBlockRules.add(rule);
                        }
                        LoadingPopupView loadingPopupView = new XPopup.Builder(getContext()).asLoading("正在导入中");
                        loadingPopupView.show();
                        AdBlockModel.saveRules(adBlockRules, new AdBlockModel.OnSaveListener() {
                            @Override
                            public void update(int progress) {

                            }

                            @Override
                            public void ok(int count) {
                                runOnUiThread(() -> {
                                    loadingPopupView.dismiss();
                                    ToastMgr.shortBottomCenter(getContext(), "成功导入" + count + "条规则");
                                    List<AdBlockRule> adBlockRules = null;
                                    try {
                                        adBlockRules = LitePal.findAll(AdBlockRule.class);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    rules.clear();
                                    if (!CollectionUtil.isEmpty(adBlockRules)) {
                                        rules.addAll(adBlockRules);
                                        Collections.sort(rules, (o1, o2) -> (int) (o2.getId() - o1.getId()));
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        });
                    })
                    .show();
}
