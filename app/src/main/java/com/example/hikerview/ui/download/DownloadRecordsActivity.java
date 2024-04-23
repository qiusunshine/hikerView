package com.example.hikerview.ui.download;

import static com.example.hikerview.utils.PreferenceMgr.SETTING_CONFIG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
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
import com.example.hikerview.ui.ActivityManager;
import com.example.hikerview.ui.base.BaseSlideActivity;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.enums.SortType;
import com.example.hikerview.ui.setting.file.FileBrowserActivity;
import com.example.hikerview.ui.setting.office.DownloaderOfficer;
import com.example.hikerview.ui.thunder.ThunderManager;
import com.example.hikerview.ui.video.VideoPlayerActivity;
import com.example.hikerview.ui.view.EnhanceTabLayout;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.example.hikerview.utils.AlertNewVersionUtil;
import com.example.hikerview.utils.DisplayUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.MyStatusBarUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.google.android.material.tabs.TabLayout;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private Button clearBtn;
    private LoadingPopupView loadingPopupView;
    private boolean isTouch = false;

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
        bg.setOnClickListener(view -> {
        });
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bg.getLayoutParams();
        layoutParams.topMargin = marginTop;
        bg.setLayoutParams(layoutParams);
        downloadingFragment = new DownloadRecordsFragment();
        downloadedFragment = new DownloadRecordsFragment();
        Button addBtn = findView(R.id.download_list_url);
        addBtn.setOnClickListener(v -> {
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
        AlertNewVersionUtil.alert(this);
    }

    private DownloadRecordsFragment getDownloadFragment() {
        return viewPager.getCurrentItem() == 0 ? downloadedFragment : downloadingFragment;
    }

    public void showPage(String taskId) {
        viewPager.setCurrentItem(0);
        if (StringUtil.isNotEmpty(taskId)) {
            downloadedFragment.openTaskItem(taskId);
        }
    }

    private void clickMenu(View view) {
        boolean download_time = PreferenceMgr.getBoolean(getContext(), SETTING_CONFIG, "download_time", false);
        new XPopup.Builder(getContext())
                .borderRadius(DisplayUtil.dpToPx(getContext(), 16))
                .atView(view)
                .asAttachList(new String[]{"排序方式", "批量删除", "文件管理", download_time ? "隐藏时间" : "显示时间", "分类显示", "更多设置"}, null,
                        (position, text) -> {
                            switch (text) {
                                case "分类显示":
                                    new XPopup.Builder(getContext())
                                            .borderRadius(DisplayUtil.dpToPx(getContext(), 16))
                                            .asCenterList(null, new String[]{"开启分类显示", "关闭分类显示"}, null, DownloadConfig.smartFilm ? 0 : 1, (position1, text1) -> {
                                                DownloadConfig.smartFilm = position1 == 0;
                                                PreferenceMgr.put(getContext(), "download", "smartFilm", DownloadConfig.smartFilm);
                                            }).show();
                                    break;
                                case "更多设置":
                                    DownloaderOfficer.INSTANCE.show(getActivity());
                                    break;
                                case "排序方式":
                                    setSortType(view);
                                    break;
                                case "批量删除":
                                    DownloadRecordsFragment fragment = getDownloadFragment();
                                    fragment.batchDelete();
                                    break;
                                case "文件管理":
                                    Intent intent2 = new Intent(getContext(), FileBrowserActivity.class);
                                    intent2.putExtra("path", DownloadConfig.rootPath);
                                    startActivity(intent2);
                                    break;
                                case "隐藏时间":
                                case "显示时间":
                                    PreferenceMgr.put(getContext(), SETTING_CONFIG, "download_time", !download_time);
                                    ToastMgr.shortBottomCenter(getContext(), "设置成功");
                                    break;
                            }
                        })
                .show();
    }

    private void setSortType(View view) {
        int type0 = PreferenceMgr.getInt(getContext(), SETTING_CONFIG, "download_sort", 0);
        new XPopup.Builder(getContext())
                .borderRadius(DisplayUtil.dpToPx(getContext(), 16))
                .atView(view)
                .asCenterList(null, SortType.getNames(), null, type0, (position, text) -> {
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
        if (isTouch) {
            return;
        }
        LitePal.findAllAsync(DownloadRecord.class).listen(list -> {
            if (isTouch) {
                return;
            }
            if (list == null) {
                list = new ArrayList<>();
            }
            int type = PreferenceMgr.getInt(getContext(), SETTING_CONFIG, "download_sort", 0);
            SortType sortType = SortType.getByCode(type);
            Comparator<DownloadRecord> nameComparator = (o1, o2) -> {
                if (StringUtils.equals(o1.getFileName(), o2.getFileName())) {
                    return (int) (o1.getId() - o2.getId());
                }
                if (o1.getFileName() == null) {
                    return -1;
                }
                if (o2.getFileName() == null) {
                    return 1;
                }
                if (o1.getFileName().length() > 2 && o2.getFileName().length() > 2) {
                    if (o1.getFileName().length() != o2.getFileName().length() && o1.getFileName().substring(0, 2).equals(o2.getFileName().substring(0, 2))) {
                        return o1.getFileName().length() - o2.getFileName().length();
                    }
                }
                return o1.getFileName().compareTo(o2.getFileName());
            };
            List<DownloadRecord> downloading = Stream.of(list)
                    .filter(downloadRecord -> !DownloadStatusEnum.SUCCESS.getCode().equals(downloadRecord.getStatus())
                            && !DownloadStatusEnum.DELETED.getCode().equals(downloadRecord.getStatus()))
                    .collect(Collectors.toList());
            List<DownloadRecord> downloaded = Stream.of(list)
                    .filter(downloadRecord -> DownloadStatusEnum.SUCCESS.getCode().equals(downloadRecord.getStatus()))
                    .collect(Collectors.toList());
            List<DownloadRecord> deletedRecords = Stream.of(list)
                    .filter(downloadRecord -> DownloadStatusEnum.DELETED.getCode().equals(downloadRecord.getStatus()))
                    .collect(Collectors.toList());

            List<DownloadRecord> localDownloaded = DownloadChooser.getLocalDownloaded(getContext());
            if (CollectionUtil.isNotEmpty(localDownloaded)) {
                if (CollectionUtil.isNotEmpty(downloaded)) {
                    Set<String> records = new HashSet<>(Stream.of(downloaded).map(DownloadRecordsFragment::getLocalFileName).toList());
                    localDownloaded = Stream.of(localDownloaded).filter(it -> !records.contains(it.getFileName())).toList();
                }
                //移除那些仅删除记录的
                if (CollectionUtil.isNotEmpty(deletedRecords)) {
                    Map<String, Long> deletedMap = new HashMap<>();
                    for (DownloadRecord deletedRecord : deletedRecords) {
                        String fileName = DownloadRecordsFragment.getLocalFileName(deletedRecord);
                        deletedMap.put(fileName, deletedRecord.getSaveTime());
                    }
                    Iterator<DownloadRecord> iterator = localDownloaded.iterator();
                    while (iterator.hasNext()) {
                        DownloadRecord record = iterator.next();
                        if (deletedMap.containsKey(record.getFileName())) {
                            Long t = deletedMap.get(record.getFileName());
                            if (t != null && t == record.getSaveTime()) {
                                iterator.remove();
                            }
                        }
                    }
                }
                if (CollectionUtil.isNotEmpty(localDownloaded)) {
                    downloaded.addAll(localDownloaded);
                }
            }

            for (DownloadRecord record : downloaded) {
                appendFilm(record);
            }
            for (DownloadRecord record : downloading) {
                appendFilm(record);
            }
            boolean download_time = PreferenceMgr.getBoolean(getContext(), SETTING_CONFIG, "download_time", false);
            if (download_time) {
                for (DownloadRecord downloadRecord : downloading) {
                    downloadRecord.setShowTime(true);
                }
                for (DownloadRecord downloadRecord : downloaded) {
                    downloadRecord.setShowTime(true);
                }
            }
            sort(sortType, nameComparator, downloading);
            sort(sortType, nameComparator, downloaded);
            downloadingFragment.setRules(downloading, false);
            downloadedFragment.setRules(downloaded, true);
        });
    }

    private void sort(SortType sortType, Comparator<DownloadRecord> nameComparator, List<DownloadRecord> list) {
        if (sortType == SortType.NAME) {
            Collections.sort(list, nameComparator);
        } else if (sortType == SortType.TIME_REVERSE) {
            Collections.sort(list, (o1, o2) -> {
                long order = o2.getSaveTime() - o1.getSaveTime();
                if (order == 0) {
                    return (int) (o2.getId() - o1.getId());
                }
                return order > 0 ? 1 : -1;
            });
        } else {
            Collections.sort(list, (o1, o2) -> {
                long order = o1.getSaveTime() - o2.getSaveTime();
                if (order == 0) {
                    return (int) (o1.getId() - o2.getId());
                }
                return order > 0 ? 1 : -1;
            });
        }
    }

    private void appendFilm(DownloadRecord record) {
        if (StringUtil.isEmpty(record.getFilm())) {
            record.setFilm(DownloadChooser.smartFilm(record.getFullName()));
        } else if ("其它格式".equals(record.getFilm())) {
            record.setFilm(DownloadChooser.smartFilm(record.getFullName()));
        }
    }

    @Override
    public void onBackPressed() {
        if (StringUtil.isNotEmpty(getDownloadFragment().getSelectFilm())) {
            getDownloadFragment().backToHome();
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
        if (!ActivityManager.getInstance().hasActivity(VideoPlayerActivity.class)) {
            ThunderManager.INSTANCE.release();
        }
    }

    public void showDeleteBtn() {
        clearBtn.setText("删除选中");
    }

    private View.OnClickListener addBtnListener = v -> {
        if ("清空".equals(clearBtn.getText().toString())) {
            String msg = viewPager.getCurrentItem() == 0 ? "确认要清空已下载的所有内容吗？" : "确认要清空正在下载的所有任务吗？";
            new PromptDialog(getContext())
                    .setTitleText("温馨提示")
                    .setContentText(msg)
                    .setPositiveListener("确认", dialog -> {
                        dialog.dismiss();
                        List<DownloadRecord> records = getDownloadFragment().getRules();
                        if (CollectionUtil.isNotEmpty(records)) {
                            if (viewPager.getCurrentItem() == 0) {
                                DownloadRecordsFragment.deleteRecords(getContext(), records);
                            } else {
                                DownloadRecordsFragment.cancelRecords(getContext(), records);
                            }
                            getDownloadFragment().updateShowList(null);
                        }
                        ToastMgr.shortBottomCenter(getContext(), "正在清空");
                    }).show();
        } else {
            List<DownloadRecord> records = new ArrayList<>();
            for (DownloadRecord rule : getDownloadFragment().getRules()) {
                if (rule.isSelected()) {
                    records.add(rule);
                }
            }
            if (CollectionUtil.isEmpty(records)) {
                getDownloadFragment().setMultiDeleting(false);
                clearBtn.setText("清空");
                ToastMgr.shortCenter(getContext(), "没有选中要删除的内容");
                return;
            }
            new XPopup.Builder(getContext())
                    .borderRadius(DisplayUtil.dpToPx(getContext(), 16))
                    .asConfirm("温馨提示", "确认删除选中的" + records.size() + "项内容？", () -> {
                        getDownloadFragment().setMultiDeleting(false);
                        clearBtn.setText("清空");
                        if (loadingPopupView == null) {
                            loadingPopupView = new XPopup.Builder(getContext()).borderRadius(DisplayUtil.dpToPx(getContext(), 16))
                                    .asLoading();
                        }
                        loadingPopupView.setTitle("正在删除中，请稍候");
                        loadingPopupView.show();
                        HeavyTaskUtil.executeNewTask(() -> {
                            DownloadRecordsFragment.deleteRecordsSync(getContext(), records);
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

    public static void clearApks(Context context) {
        boolean apkClean = PreferenceMgr.getBoolean(context, "download", "apkClean", false);
        if (!apkClean) {
            return;
        }
        //下载记录
        List<DownloadRecord> records = LitePal.where("status = ?", DownloadStatusEnum.SUCCESS.getCode()).find(DownloadRecord.class);
        if (CollectionUtil.isNotEmpty(records)) {
            List<DownloadRecord> downloads = new ArrayList<>();
            for (DownloadRecord record : records) {
                if ("apk".equals(record.getFileExtension()) || record.getFileName().endsWith(".apk")) {
                    downloads.add(record);
                }
            }
            if (CollectionUtil.isNotEmpty(downloads)) {
                DownloadRecordsFragment.deleteRecordsSync(context, downloads);
            }
        }
        //转存到公开目录的
        List<DownloadRecord> recordList = DownloadChooser.scanLocalFiles(context);
        if (CollectionUtil.isNotEmpty(recordList)) {
            List<DownloadRecord> downloads = new ArrayList<>();
            for (DownloadRecord record : recordList) {
                if ("apk".equals(record.getFileExtension()) || record.getFileName().endsWith(".apk")) {
                    downloads.add(record);
                }
            }
            if (CollectionUtil.isNotEmpty(downloads)) {
                DownloadRecordsFragment.deleteRecordsSync(context, downloads);
            }
        }
        //默认下载目录的
        String filePath = DownloadDialogUtil.getApkDownloadPath(context);
        if (filePath != null && filePath.length() > 0) {
            File dir = new File(filePath);
            if (!dir.exists()) {
                return;
            }
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory() || !file.getName().endsWith(".apk")) {
                        continue;
                    }
                    try {
                        file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            isTouch = true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            isTouch = false;
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setTouch(boolean isTouch) {
        this.isTouch = isTouch;
    }
}
