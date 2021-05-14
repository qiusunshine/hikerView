package com.example.hikerview.ui.download;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.example.hikerview.R;
import com.example.hikerview.event.DownloadStoreRefreshEvent;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.ui.base.BaseSlideActivity;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.enums.SortType;
import com.example.hikerview.ui.view.EnhanceTabLayout;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.MyStatusBarUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.ToastMgr;
import com.google.android.material.tabs.TabLayout;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.example.hikerview.utils.PreferenceMgr.SETTING_CONFIG;

/**
 * 作者：By 15968
 * 日期：On 2019/10/5
 * 时间：At 10:09
 */
public class DownloadRecordsActivity extends BaseSlideActivity {
    private static final String TAG = "DownloadRecordsActivity";
    private ViewPager viewPager;
    private EnhanceTabLayout tabLayout;
    private DownloadRecordsFragment downloadingFragment, downloadedFragment;
    private Button addBtn, clearBtn;
    private LoadingPopupView loadingPopupView;

    @Override
    protected View getBackgroundView() {
        return findView(R.id.download_list_window);
    }

    @Override
    protected int initLayout(Bundle savedInstanceState) {
        return R.layout.activit_download_list;
    }

    @Override
    protected void initView2() {
        ((TextView) findView(R.id.download_list_title_text)).setText("我的下载");
        clearBtn = findView(R.id.download_list_add);
        clearBtn.setText("清空");
        clearBtn.setOnClickListener(addBtnListener);
        //初始化高度
        int marginTop = MyStatusBarUtil.getStatusBarHeight(getContext()) + DisplayUtil.dpToPx(getContext(), 86);
        View bg = findView(R.id.download_list_bg);
        findView(R.id.download_list_window).setOnClickListener(view -> finish());
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bg.getLayoutParams();
        layoutParams.topMargin = marginTop;
        bg.setLayoutParams(layoutParams);
        downloadingFragment = new DownloadRecordsFragment();
        downloadedFragment = new DownloadRecordsFragment();
        addBtn = findView(R.id.download_list_url);
        addBtn.setOnClickListener(v -> {
            if (!"新增".equals(addBtn.getText().toString())) {
                downloadedFragment.saveOrders();
                addBtn.setText("新增");
                return;
            }
            DownloadDialogUtil.showEditDialog(DownloadRecordsActivity.this, null, null);
        });
        viewPager = findView(R.id.download_view_pager);
        tabLayout = findView(R.id.download_header_tab);
        viewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return position == 0 ? downloadedFragment : downloadingFragment;
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public int getItemPosition(@NonNull Object object) {
                return POSITION_NONE;
            }
        });
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addTab("已下载");
        tabLayout.addTab("下载中");
        Objects.requireNonNull(tabLayout.getTabLayout().getTabAt(0)).select();
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout.getTabLayout()));
        if (!getIntent().getBooleanExtra("downloaded", false)) {
            viewPager.setCurrentItem(1);
        }
        findView(R.id.menu_icon).setOnClickListener(this::clickMenu);
    }

    private void clickMenu(View view) {
        new XPopup.Builder(getContext())
                .atView(view)
                .asAttachList(new String[]{"排序方式", "批量删除"}, null,
                        (position, text) -> {
                            switch (text) {
                                case "排序方式":
                                    setSortType(view);
                                    break;
                                case "批量删除":
                                    DownloadRecordsFragment fragment = viewPager.getCurrentItem() == 0 ? downloadedFragment : downloadingFragment;
                                    fragment.batchDelete();
                                    break;
                            }
                        })
                .show();
    }

    private void setSortType(View view) {
        new XPopup.Builder(getContext())
                .atView(view)
                .asAttachList(SortType.getNames(), null, (position, text) -> {
                    SortType type = SortType.getByName(text);
                    PreferenceMgr.put(getContext(), SETTING_CONFIG, "download_sort", type.getCode());
                    ToastMgr.shortBottomCenter(getContext(), "排序方式已设置为" + text);
                }).show();
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefresh(DownloadStoreRefreshEvent event) {
        LitePal.findAllAsync(DownloadRecord.class).listen(list -> {
            if (CollectionUtil.isEmpty(list)) {
                downloadingFragment.setRules(new ArrayList<>());
                downloadedFragment.setRules(new ArrayList<>());
                return;
            }
            int type = PreferenceMgr.getInt(getContext(), SETTING_CONFIG, "download_sort", 0);
            SortType sortType = SortType.getByCode(type);
            if (sortType == SortType.NAME) {
                Collections.sort(list, (o1, o2) -> {
                    if (StringUtils.equals(o1.getFileName(), o2.getFileName())) {
                        return (int) (o1.getId() - o2.getId());
                    }
                    if (o1.getFileName() == null) {
                        return -1;
                    }
                    if (o2.getFileName() == null) {
                        return 1;
                    }
                    return o1.getFileName().compareTo(o2.getFileName());
                });
            } else {
                Collections.sort(list, (o1, o2) -> {
                    int order = o2.getOrder() - o1.getOrder();
                    if (order == 0) {
                        return (int) (o1.getId() - o2.getId());
                    }
                    return order;
                });
            }
            downloadingFragment.setRules(Stream.of(list).filter(downloadRecord -> !DownloadStatusEnum.SUCCESS.getCode().equals(downloadRecord.getStatus())).collect(Collectors.toList()));
            downloadedFragment.setRules(Stream.of(list).filter(downloadRecord -> DownloadStatusEnum.SUCCESS.getCode().equals(downloadRecord.getStatus())).collect(Collectors.toList()));
        });
    }

    public void showSortSaveBtn() {
        addBtn.setText("保存");
    }

    @Override
    public void onBackPressed() {
        if (!"新增".equals(addBtn.getText().toString())) {
            new XPopup.Builder(getContext())
                    .asConfirm("温馨提示", "排序结果还没有保存哦，建议先点击右上角保存按钮来保存排序结果再退出当前页面，当然也可以选择不保存，确定不保存强制退出该页面？",
                            this::finish).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    public void showDeleteBtn() {
        clearBtn.setText("删除选中");
    }

    private View.OnClickListener addBtnListener = v -> {
        if ("清空".equals(clearBtn.getText().toString())) {
            new PromptDialog(getContext())
                    .setTitleText("温馨提示")
                    .setContentText("确认要清空下载吗？")
                    .setPositiveListener("确认", dialog -> {
                        dialog.dismiss();
                        DownloadManager.instance().cancelAllTask();
                        FileUtil.deleteDirs(DownloadConfig.rootPath);
                        LitePal.deleteAll(DownloadRecord.class);
                        ToastMgr.shortBottomCenter(getContext(), "正在清空下载");
                    }).show();
        } else {
            List<DownloadRecord> records = new ArrayList<>();
            for (DownloadRecord rule : downloadedFragment.getRules()) {
                if (rule.isSelected()) {
                    records.add(rule);
                }
            }
            if (CollectionUtil.isEmpty(records)) {
                downloadedFragment.setMultiDeleting(false);
                clearBtn.setText("清空");
                ToastMgr.shortCenter(getContext(), "没有选中要删除的内容");
                return;
            }
            new XPopup.Builder(getContext())
                    .asConfirm("温馨提示", "确认删除选中的" + records.size() + "项内容？", () -> {
                        downloadedFragment.setMultiDeleting(false);
                        clearBtn.setText("清空");
                        if (loadingPopupView == null) {
                            loadingPopupView = new XPopup.Builder(getContext()).asLoading();
                        }
                        loadingPopupView.setTitle("正在删除中，请稍候");
                        loadingPopupView.show();
                        HeavyTaskUtil.executeNewTask(() -> {
                            for (DownloadRecord record : records) {
                                DownloadManager.instance().cancelTask(record.getTaskId());
                                FileUtil.deleteDirs(DownloadManager.instance().getDownloadDir(record.getFileName()));
                            }
                            if (isFinishing()) {
                                return;
                            }
                            runOnUiThread(() -> {
                                loadingPopupView.dismiss();
                                ToastMgr.shortCenter(getContext(), "已删除选中的内容");
                            });
                        });
                    })
                    .show();
        }
    };
}
