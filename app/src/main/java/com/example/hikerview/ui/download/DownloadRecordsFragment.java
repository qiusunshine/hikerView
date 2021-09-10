package com.example.hikerview.ui.download;

import android.app.Service;
import android.os.Vibrator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.R;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.ui.base.BaseFragment;
import com.example.hikerview.ui.download.enums.SortType;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.video.PlayerChooser;
import com.example.hikerview.ui.video.VideoChapter;
import com.example.hikerview.ui.view.XGridLayoutManager;
import com.example.hikerview.ui.webdlan.LocalServerParser;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.ShareUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.jeffmony.m3u8library.listener.IVideoTransformListener;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.example.hikerview.utils.PreferenceMgr.SETTING_CONFIG;

/**
 * 作者：By 15968
 * 日期：On 2019/12/22
 * 时间：At 19:17
 */
public class DownloadRecordsFragment extends BaseFragment {
    private RecyclerView recyclerView;
    private DownloadRecordsAdapter adapter;
    private List<DownloadRecord> rules = new ArrayList<>();
    private List<DownloadRecord> showList = new ArrayList<>();
    private boolean isSorting = false;
    private boolean isMultiDeleting = false;
    private LoadingPopupView transformLoadingPopup;

    public String getSelectFilm() {
        return selectFilm;
    }

    private String selectFilm = null;

    @Override
    protected int initLayout() {
        return R.layout.fragment_download_list;
    }

    @Override
    protected void initView() {
        recyclerView = findView(R.id.download_list_recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new DownloadRecordsAdapter(getContext(), showList);
        adapter.setOnItemClickListener(onItemClickListener);
        XGridLayoutManager gridLayoutManager = new XGridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initData() {
        touchHelper.attachToRecyclerView(recyclerView);
    }

    public List<DownloadRecord> getRules() {
        return rules;
    }

    public void setMultiDeleting(boolean isMultiDeleting) {
        this.isMultiDeleting = isMultiDeleting;
    }

    public void setRules(List<DownloadRecord> rules) {
        if (isSorting || isMultiDeleting) {
            return;
        }
        if (JSON.toJSONString(this.rules).equals(JSON.toJSONString(rules))) {
            return;
        }
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                this.rules.clear();
                this.rules.addAll(rules);
                updateShowList();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                } else {
                    recyclerView.postDelayed(() -> adapter.notifyDataSetChanged(), 500);
                }
            });
        }
    }

    public void backToHome() {
        updateShowList(null);
        adapter.notifyDataSetChanged();
    }

    private synchronized void updateShowList(String film) {
        selectFilm = film;
        updateShowList();
    }

    private synchronized void updateShowList() {
        List<DownloadRecord> records = new ArrayList<>();
        if (StringUtil.isNotEmpty(selectFilm)) {
            for (DownloadRecord rule : rules) {
                if (selectFilm.equals(rule.getFilm())) {
                    records.add(rule);
                }
            }
        } else {
            //用来保证顺序
            List<String> dirList = new ArrayList<>();
            Map<String, Integer> countMap = new HashMap<>();
            records.addAll(rules);
            for (DownloadRecord rule : rules) {
                if (StringUtil.isNotEmpty(rule.getFilm())) {
                    if (!dirList.contains(rule.getFilm())) {
                        dirList.add(rule.getFilm());
                        countMap.put(rule.getFilm(), 1);
                    } else {
                        countMap.put(rule.getFilm(), countMap.get(rule.getFilm()) + 1);
                    }
                }
            }
            for (Iterator<DownloadRecord> iterator = records.iterator(); iterator.hasNext(); ) {
                DownloadRecord next = iterator.next();
                if (StringUtil.isNotEmpty(next.getFilm()) && countMap.containsKey(next.getFilm()) && countMap.get(next.getFilm()) > 1) {
                    iterator.remove();
                }
            }
            //删除只有一个item的文件夹
            for (Iterator<String> iterator = dirList.iterator(); iterator.hasNext(); ) {
                String next = iterator.next();
                if (countMap.containsKey(next) && countMap.get(next) < 2) {
                    iterator.remove();
                }
            }
            for (int i = dirList.size() - 1; i >= 0; i--) {
                DownloadRecord record = new DownloadRecord();
                record.setSourcePageTitle(dirList.get(i));
                record.setVideoType("dir");
                record.setSourcePageUrl(String.valueOf(countMap.get(dirList.get(i))));
                records.add(0, record);
            }
        }
        showList.clear();
        showList.addAll(records);
    }

    private DownloadRecordsAdapter.OnItemClickListener onItemClickListener = new DownloadRecordsAdapter.OnItemClickListener() {
        @Override
        public void onClick(View view, int position) {
            if (position < 0 || position >= showList.size()) {
                return;
            }
            DownloadRecord record = showList.get(position);
            if (isMultiDeleting) {
                record.setSelected(!record.isSelected());
                adapter.notifyItemChanged(position);
                return;
            }
            if ("dir".equals(record.getVideoType())) {
                updateShowList(record.getSourcePageTitle());
                adapter.notifyDataSetChanged();
                return;
            }
            if (!DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                onLongClick(view, position);
                return;
            }
            List<VideoChapter> chapters = new ArrayList<>();
            for (int i = 0; i < position; i++) {
                VideoChapter videoChapter = new VideoChapter();
                videoChapter.setTitle(showList.get(i).getSourcePageTitle());
                videoChapter.setUse(false);
                videoChapter.setDownloadRecord(showList.get(i));
                chapters.add(videoChapter);
            }
            VideoChapter videoChapter = new VideoChapter();
            videoChapter.setTitle(showList.get(position).getSourcePageTitle());
            videoChapter.setUrl(LocalServerParser.getRealUrl(getContext(), record));
            videoChapter.setDownloadRecord(showList.get(position));
            videoChapter.setUse(true);
            chapters.add(videoChapter);
            for (int i = position + 1; i < showList.size(); i++) {
                VideoChapter chapter = new VideoChapter();
                chapter.setTitle(showList.get(i).getSourcePageTitle());
                chapter.setDownloadRecord(showList.get(i));
                chapter.setUse(false);
                chapters.add(chapter);
            }
            PlayerChooser.startPlayer(getActivity(), chapters);
        }

        @Override
        public void onLongClick(View view, int position) {
            if (isSorting || isMultiDeleting) {
                return;
            }
            if (position < 0 || position >= showList.size()) {
                return;
            }
            DownloadRecord record = showList.get(position);
            String status = showList.get(position).getStatus();
            String[] operations;

            if ("dir".equals(record.getVideoType())) {
                operations = new String[]{"删除下载", "重命名标题"};
            } else if (DownloadStatusEnum.SUCCESS.getCode().equals(status) || DownloadStatusEnum.ERROR.getCode().equals(status)
                    || DownloadStatusEnum.CANCEL.getCode().equals(status) || DownloadStatusEnum.BREAK.getCode().equals(status)) {
                if (DownloadStatusEnum.SUCCESS.getCode().equals(status) && "player/m3u8".equals(record.getVideoType())) {
                    operations = new String[]{"删除下载", "批量删除", "重新下载", "重命名标题", "复制下载链接", "合并为MP4格式"};
                } else if (DownloadStatusEnum.SUCCESS.getCode().equals(status) && "normal".equals(record.getVideoType())) {
                    operations = new String[]{"删除下载", "批量删除", "重新下载", "重命名标题", "复制下载链接", "分享视频文件", "复制文件路径"};
                } else {
                    operations = new String[]{"删除下载", "批量删除", "重新下载", "重命名标题", "复制下载链接"};
                }
            } else {
                operations = new String[]{"取消下载", "批量取消"};
            }
            new XPopup.Builder(getContext())
                    .asCenterList("请选择操作", operations, ((option, text) -> {
                        switch (text) {
                            case "删除下载":
                                new XPopup.Builder(getContext())
                                        .asConfirm("温馨提示", "确认删除该下载内容吗？", () -> {
                                            ToastMgr.shortBottomCenter(getContext(), "正在删除下载任务");
                                            List<DownloadRecord> list = new ArrayList<>();
                                            if ("dir".equals(record.getVideoType())) {
                                                for (DownloadRecord rule : rules) {
                                                    if (record.getSourcePageTitle().equals(rule.getFilm())) {
                                                        list.add(rule);
                                                    }
                                                }
                                            } else {
                                                list.add(record);
                                            }
                                            HeavyTaskUtil.executeNewTask(() -> {
                                                for (DownloadRecord downloadRecord : list) {
                                                    DownloadManager.instance().cancelTask(downloadRecord.getTaskId());
                                                    FileUtil.deleteDirs(DownloadManager.instance().getDownloadDir(downloadRecord.getFileName()));
                                                }
                                            });
                                        }).show();
                                break;
                            case "重命名标题":
                                new XPopup.Builder(getContext()).asInputConfirm("重命名标题", null,
                                        record.getSourcePageTitle(), "请输入标题，不能为空",
                                        text1 -> {
                                            if (StringUtil.isEmpty(text1)) {
                                                ToastMgr.shortBottomCenter(getContext(), "不能为空");
                                                return;
                                            }
                                            if ("dir".equals(record.getVideoType())) {
                                                for (DownloadRecord rule : rules) {
                                                    if (record.getSourcePageTitle().equals(rule.getFilm())) {
                                                        rule.setFilm(text1);
                                                        rule.save();
                                                    }
                                                }
                                            } else {
                                                record.setSourcePageTitle(text1);
                                                record.save();
                                            }
                                            updateShowList();
                                            adapter.notifyDataSetChanged();
                                        })
                                        .show();
                                break;
                            case "批量删除":
                            case "批量取消":
                                batchDelete();
                                break;
                            case "复制下载链接":
                                ClipboardUtil.copyToClipboard(getContext(), record.getSourcePageUrl());
                                break;
                            case "分享视频文件":
                                ShareUtil.findChooserToSend(getContext(), getMp4Path(record));
                                break;
                            case "复制文件路径":
                                ClipboardUtil.copyToClipboard(getContext(), getMp4Path(record));
                                break;
                            case "合并为MP4格式":
                                transformLoadingPopup = new XPopup.Builder(getContext())
                                        .asLoading("合并中");
                                transformLoadingPopup.show();
                                DownloadManager.transformM3U8ToMp4(getContext(), record, new IVideoTransformListener() {
                                    @Override
                                    public void onTransformProgress(float v) {
                                        if (getActivity() == null || getActivity().isFinishing() || isDetached()) {
                                            return;
                                        }
                                        if (transformLoadingPopup != null) {
                                            transformLoadingPopup.setTitle("合并中" + Math.round(v) + "%");
                                        }
                                    }

                                    @Override
                                    public void onTransformFailed(Exception e) {
                                        if (getActivity() == null || getActivity().isFinishing() || isDetached()) {
                                            return;
                                        }
                                        if (transformLoadingPopup != null) {
                                            transformLoadingPopup.dismiss();
                                        }
                                    }

                                    @Override
                                    public void onTransformFinished() {
                                        if (getActivity() == null || getActivity().isFinishing() || isDetached()) {
                                            return;
                                        }
                                        if (transformLoadingPopup != null) {
                                            transformLoadingPopup.dismiss();
                                        }
                                    }
                                });
                                break;
                            case "重新下载":
                                DownloadManager.instance().cancelTask(record.getTaskId());
                                FileUtil.deleteDirs(DownloadManager.instance().getDownloadDir(record.getFileName()));
                                DownloadTask downloadTask = new DownloadTask(
                                        UUIDUtil.genUUID(), null, null, null, record.getSourcePageUrl(),
                                        record.getSourcePageUrl(), record.getSourcePageTitle(), 0L);
                                downloadTask.setFilm(record.getFilm());
                                DownloadManager.instance().addTask(downloadTask);
                                ToastMgr.shortBottomCenter(getContext(), "已加入下载队列");
                                break;
                            case "取消下载":
                                ToastMgr.shortBottomCenter(getContext(), "正在取消下载任务");
                                DownloadManager.instance().cancelTask(record.getTaskId());
                                break;
                            case "长按拖拽排序":
                                int type = PreferenceMgr.getInt(getContext(), SETTING_CONFIG, "download_sort", 0);
                                SortType sortType = SortType.getByCode(type);
                                if (SortType.NAME.equals(sortType)) {
                                    ToastMgr.shortBottomCenter(getContext(), "当前排序方式为按名称排序，不支持手动排序");
                                    break;
                                }
                                isSorting = true;
                                if (getActivity() instanceof DownloadRecordsActivity) {
                                    ((DownloadRecordsActivity) getActivity()).showSortSaveBtn();
                                    ToastMgr.shortBottomCenter(getContext(), "长按拖拽排序哦，排序后点击右上角保存按钮保存排序结果");
                                } else {
                                    ToastMgr.shortBottomCenter(getContext(), "获取父页面失败");
                                }
                                break;
                        }
                    }))
                    .show();
        }
    };

    private String getMp4Path(DownloadRecord downloadRecord){
        String dir = DownloadManager.instance().getDownloadDir(downloadRecord.getFileName());
        String path = dir + File.separator + "video." + downloadRecord.getFileExtension();
        if(new File(path).exists()){
            return path;
        }
        return dir + File.separator + downloadRecord.getFileName() + "." + downloadRecord.getFileExtension();
    }

    void batchDelete() {
        new XPopup.Builder(getContext())
                .asConfirm("温馨提示", "请点击下载项来勾选要删除/取消的内容", () -> {
                    isMultiDeleting = true;
                    if (getActivity() instanceof DownloadRecordsActivity) {
                        ((DownloadRecordsActivity) getActivity()).showDeleteBtn();
                    } else {
                        ToastMgr.shortBottomCenter(getContext(), "获取父页面失败");
                    }
                }).show();
    }

    void saveOrders() {
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).setOrder(rules.size() - i);
        }
        LitePal.saveAllAsync(rules).listen(success -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                ToastMgr.shortBottomCenter(getContext(), "排序结果保存成功");
                isSorting = false;
            }
        });
    }


    private ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlag = 0;
            if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            } else if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            }
            return makeMovementFlags(dragFlag, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(rules, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(rules, i, i - 1);
                }
            }
            adapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            //侧滑删除可以使用；
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return isSorting && !isMultiDeleting;
        }

        /**
         * 长按选中Item的时候开始调用
         * 长按高亮
         * @param viewHolder
         * @param actionState
         */
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
//                viewHolder.itemView.setBackgroundColor(getContext().getResources().getColor(R.color.gray_rice));
                //获取系统震动服务//震动70毫秒
                try {
                    Vibrator vib = (Vibrator) requireActivity().getSystemService(Service.VIBRATOR_SERVICE);
                    if (vib != null) {
                        vib.vibrate(70);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        /**
         * 手指松开的时候还原高亮
         * @param recyclerView
         * @param viewHolder
         */
        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
//            viewHolder.itemView.setBackgroundColor(0);
            adapter.notifyDataSetChanged();
        }
    });
}
