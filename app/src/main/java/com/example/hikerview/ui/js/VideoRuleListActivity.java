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
import com.example.hikerview.ui.base.BaseSlideActivity;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.MyStatusBarUtil;
import com.example.hikerview.utils.ToastMgr;
import com.lxj.xpopup.XPopup;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 10:09
 */
public class VideoRuleListActivity extends BaseSlideActivity {
    private RecyclerView recyclerView;
    private StringListAdapter adapter;

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
        ((TextView) findView(R.id.ad_list_title_text)).setText("音视频嗅探（正则）");
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
        List<String> rules = new ArrayList<>(UrlDetector.getVideoRules());
        adapter = new StringListAdapter(getContext(), rules);
        adapter.setOnItemClickListener(onItemClickListener);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    private StringListAdapter.OnItemClickListener onItemClickListener = new StringListAdapter.OnItemClickListener() {
        @Override
        public void onDelete(View view, int position) {
            editRule(adapter.getList().get(position), position);
        }

        @Override
        public void onLongClick(View view, int position) {
            new XPopup.Builder(getContext())
                    .asCenterList("请选择操作", new String[]{"删除规则"},
                            ((option, text) -> {
                                switch (text) {
                                    case "删除规则":
                                        removeRule(adapter.getList().get(position));
                                        break;
                                }
                            }))
                    .show();
        }
    };

    private View.OnClickListener addBtnListener = v -> {
        if (adapter.getList().size() >= 30) {
            new PromptDialog(getContext())
                    .setTitleText("温馨提示")
                    .setContentText("嗅探正则规则大于30条，不能继续导入！正常情况下音视频都会嗅探，只有某些特殊资源需要正则嗅探，如果当前数量限制不符合您的需求，请联系我们")
                    .setPositiveListener("我已知晓", PromptDialog::dismiss)
                    .show();
            return;
        }
        new XPopup.Builder(getContext()).asInputConfirm("新增音视频嗅探规则", "普通音视频（mp4、m3u8、mp3等）会自动嗅探，只有嗅探不了才特殊处理，该规则为正则规则",
                text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastMgr.shortBottomCenter(getContext(), "规则不能为空");
                        return;
                    }
                    UrlDetector.addVideoRule(getContext(), text);
                    ToastMgr.shortBottomCenter(getContext(), "规则保存成功");
                    adapter.getList().add(text);
                    adapter.notifyDataSetChanged();
                })
                .show();
    };

    private void editRule(String rule, int position) {
        new XPopup.Builder(getContext()).asInputConfirm("编辑音视频嗅探规则",
                "普通音视频（mp4、m3u8、mp3等）会自动嗅探，只有嗅探不了才特殊处理，该规则为正则规则",
                rule, null,
                text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastMgr.shortBottomCenter(getContext(), "规则不能为空");
                        return;
                    }
                    UrlDetector.updateVideoRule(rule, text);
                    ToastMgr.shortBottomCenter(getContext(), "规则保存成功");
                    adapter.getList().remove(position);
                    adapter.getList().add(position, text);
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private void removeRule(String rule) {
        new XPopup.Builder(getContext()).asConfirm("温馨提示",
                "确定要删除该规则吗？删除后无法恢复，请谨慎删除！", () -> {
                    UrlDetector.removeVideoRule(rule);
                    ToastMgr.shortBottomCenter(getContext(), "规则删除成功");
                    adapter.getList().remove(rule);
                    adapter.notifyDataSetChanged();
                }).show();
    }
}
