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
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.video.PlayerChooser;
import com.example.hikerview.ui.video.VideoChapter;
import com.example.hikerview.ui.webdlan.LocalServerParser;
import com.example.hikerview.utils.ClipboardUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.lxj.xpopup.XPopup;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2019/12/22
 * 时间：At 19:17
 */
public class DownloadRecordsFragment extends BaseFragment {
    private RecyclerView recyclerView;
    private DownloadRecordsAdapter adapter;
    private List<DownloadRecord> rules = new ArrayList<>();
    private boolean isSorting = false;
    private boolean isMultiDeleting = false;

    @Override
    protected int initLayout() {
        return R.layout.fragment_download_list;
    }

    @Override
    protected void initView() {
        recyclerView = findView(R.id.download_list_recycler_view);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new DownloadRecordsAdapter(getContext(), rules);
        adapter.setOnItemClickListener(onItemClickListener);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
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
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                } else {
                    recyclerView.postDelayed(() -> adapter.notifyDataSetChanged(), 500);
                }
            });
        }
    }

    private DownloadRecordsAdapter.OnItemClickListener onItemClickListener = new DownloadRecordsAdapter.OnItemClickListener() {
        @Override
        public void onClick(View view, int position) {
            if (position < 0 || position >= rules.size()) {
                return;
            }
            DownloadRecord record = rules.get(position);
            if (!DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                onLongClick(view, position);
                return;
            }
            if (isMultiDeleting) {
                record.setSelected(!record.isSelected());
                adapter.notifyItemChanged(position);
                return;
            }
            List<VideoChapter> chapters = new ArrayList<>();
            for (int i = 0; i < position; i++) {
                VideoChapter videoChapter = new VideoChapter();
                videoChapter.setTitle(rules.get(i).getSourcePageTitle());
                videoChapter.setUse(false);
                videoChapter.setDownloadRecord(rules.get(i));
                chapters.add(videoChapter);
            }
            VideoChapter videoChapter = new VideoChapter();
            videoChapter.setTitle(rules.get(position).getSourcePageTitle());
            videoChapter.setUrl(LocalServerParser.getRealUrl(getContext(), record));
            videoChapter.setDownloadRecord(rules.get(position));
            videoChapter.setUse(true);
            chapters.add(videoChapter);
            for (int i = position + 1; i < rules.size(); i++) {
                VideoChapter chapter = new VideoChapter();
                chapter.setTitle(rules.get(i).getSourcePageTitle());
                chapter.setDownloadRecord(rules.get(i));
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
            if (position < 0 || position >= rules.size()) {
                return;
            }
            String status = rules.get(position).getStatus();
            String[] operations;
            if (DownloadStatusEnum.SUCCESS.getCode().equals(status) || DownloadStatusEnum.ERROR.getCode().equals(status)
                    || DownloadStatusEnum.CANCEL.getCode().equals(status) || DownloadStatusEnum.BREAK.getCode().equals(status)) {
                operations = new String[]{"删除下载", "批量删除", "重新下载", "重命名标题", "长按拖拽排序", "复制下载链接"};
            } else {
                operations = new String[]{"取消下载任务"};
            }
            DownloadRecord record = rules.get(position);
            new XPopup.Builder(getContext())
                    .asCenterList("请选择操作", operations, ((option, text) -> {
                        switch (text) {
                            case "删除下载":
                                new XPopup.Builder(getContext())
                                        .asConfirm("温馨提示", "确认删除该下载内容吗？", () -> {
                                            ToastMgr.shortBottomCenter(getContext(), "正在删除下载任务");
                                            DownloadManager.instance().cancelTask(record.getTaskId());
                                            FileUtil.deleteDirs(DownloadManager.instance().getDownloadDir(record.getFileName()));
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
                                            record.setSourcePageTitle(text1);
                                            record.save();
                                            adapter.notifyDataSetChanged();
                                        })
                                        .show();
                                break;
                            case "批量删除":
                                batchDelete();
                                break;
                            case "复制下载链接":
                                ClipboardUtil.copyToClipboard(getContext(), record.getSourcePageUrl());
                                break;
                            case "重新下载":
                                DownloadManager.instance().cancelTask(record.getTaskId());
                                FileUtil.deleteDirs(DownloadManager.instance().getDownloadDir(record.getFileName()));
                                DownloadTask downloadTask = new DownloadTask(
                                        UUIDUtil.genUUID(), null, null, null, record.getSourcePageUrl(),
                                        record.getSourcePageUrl(), record.getSourcePageTitle(), 0L);
                                DownloadManager.instance().addTask(downloadTask);
                                ToastMgr.shortBottomCenter(getContext(), "已加入下载队列");
                                break;
                            case "取消下载任务":
                                ToastMgr.shortBottomCenter(getContext(), "正在取消下载任务");
                                DownloadManager.instance().cancelTask(record.getTaskId());
                                break;
                            case "长按拖拽排序":
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

    void batchDelete() {
        new XPopup.Builder(getContext())
                .asConfirm("温馨提示", "请点击下载项来勾选要删除的内容", () -> {
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
