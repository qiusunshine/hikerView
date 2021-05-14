package org.adblockplus.libadblockplus.android.settings;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2020/3/23
 * 时间：At 21:08
 */
public class MySubsPopup extends BottomPopupView {
    private List<MySubscription> mediaResults;
    private String title;

    public MySubsPopup with(List<MySubscription> mediaResults) {
        this.mediaResults = mediaResults;
        return this;
    }

    public MySubsPopup withTitle(String title) {
        this.title = title;
        return this;
    }

    public MySubsPopup(@NonNull Context context) {
        super(context);
    }

    // 返回自定义弹窗的布局
    @Override
    protected int getImplLayoutId() {
        return R.layout.pop_xiu_tan_result;
    }

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    @Override
    protected void onCreate() {
        super.onCreate();
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (title != null) {
            TextView titleView = findViewById(R.id.title);
            titleView.setText(title);
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        MySubsAdapter adapter = new MySubsAdapter(getContext(), mediaResults, new MySubsAdapter.OnClickListener() {
            @Override
            public void click(String url) {
                dismissWith(() -> {
                    ClipboardUtil.copyToClipboard(getContext(), url, false);
                    ToastMgr.shortBottomCenter(getContext(), "已复制订阅地址到剪贴板");
                });
            }

            @Override
            public void longClick(String url) {
                dismissWith(() -> {
                    ClipboardUtil.copyToClipboard(getContext(), url, false);
                    ToastMgr.shortBottomCenter(getContext(), "已复制订阅地址到剪贴板");
                });
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected int getMaxHeight() {
        return (int) (XPopupUtils.getScreenHeight(getContext()) * .85f);
    }
}
