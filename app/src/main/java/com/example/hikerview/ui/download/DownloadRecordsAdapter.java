package com.example.hikerview.ui.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikerview.R;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.TimeUtil;

import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

class DownloadRecordsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<DownloadRecord> list;
    private OnItemClickListener onItemClickListener;

    DownloadRecordsAdapter(Context context, List<DownloadRecord> list) {
        this.context = context;
        this.list = list;
    }

    interface OnItemClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == -1) {
            return new DirHolder(LayoutInflater.from(context).inflate(R.layout.item_download_dir, parent, false));
        }
        return new RuleHolder(LayoutInflater.from(context).inflate(R.layout.item_download, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return "dir".equals(list.get(position).getVideoType()) ? -1 : 0;
    }

    public String getStatus(DownloadRecord downloadTask) {
        if (DownloadStatusEnum.ERROR.getCode().equals(downloadTask.getStatus())) {
            return downloadTask.getFailedReason();
        } else {
            return DownloadStatusEnum.getByCode(downloadTask.getStatus()).getDesc();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof RuleHolder) {
            RuleHolder holder = (RuleHolder) viewHolder;
            DownloadRecord record = list.get(position);
            String title = record.getSourcePageTitle();
            if (record.getSaveTime() > 0) {
                title = title + " - " + TimeUtil.formatTime(record.getSaveTime());
            }
            holder.title.setText(title);
            holder.status.setText(getStatus(record));
            if (record.isSelected()) {
                holder.bg.setBackground(context.getDrawable(R.drawable.ripple_disabled_grey));
            } else {
                holder.bg.setBackground(context.getDrawable(R.drawable.ripple_white));
            }
            if (!DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                holder.speed.setText((FileUtil.getFormatedFileSize(record.getCurrentSpeed()) + "/s"));
            } else {
                long speed = record.getCurrentSpeed();
                if (record.getFinishedTime() - record.getSaveTime() > 0) {
                    speed = record.getTotalDownloaded() * 1000 / (record.getFinishedTime() - record.getSaveTime());
                }
                holder.speed.setText((FileUtil.getFormatedFileSize(speed) + "/s"));
            }
            holder.downloaded.setText(FileUtil.getFormatedFileSize(record.getTotalDownloaded()));
//            holder.total.setText(FileUtil.getFormatedFileSize(record.getSize()));
            bindClick(holder.bg, holder);
        } else if (viewHolder instanceof DirHolder) {
            DirHolder holder = (DirHolder) viewHolder;
            DownloadRecord record = list.get(position);
            String title = record.getSourcePageTitle() + "（" + record.getSourcePageUrl() + "）";
//            if (record.getSaveTime() > 0) {
//                title = title + " - " + TimeUtil.formatTime(record.getSaveTime());
//            }
            holder.title.setText(title);
            if (record.isSelected()) {
                holder.bg.setBackground(context.getDrawable(R.drawable.ripple_disabled_grey));
            } else {
                holder.bg.setBackground(context.getDrawable(R.drawable.ripple_white));
            }
            bindClick(holder.bg, holder);
        }
    }

    private void bindClick(View bg, RecyclerView.ViewHolder holder) {
        bg.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                if (holder.getAdapterPosition() >= 0) {
                    onItemClickListener.onClick(v, holder.getAdapterPosition());
                }
            }
        });
        bg.setOnLongClickListener(v -> {
            if (onItemClickListener != null) {
                if (holder.getAdapterPosition() >= 0) {
                    onItemClickListener.onLongClick(v, holder.getAdapterPosition());
                }
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class RuleHolder extends RecyclerView.ViewHolder {
        TextView title, status, speed, downloaded;
        View bg;

        RuleHolder(View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.item_download_bg);
            title = itemView.findViewById(R.id.item_download_title);
            status = itemView.findViewById(R.id.item_download_status);
//            total = itemView.findViewById(R.id.item_download_total);
            downloaded = itemView.findViewById(R.id.item_download_downloaded);
            speed = itemView.findViewById(R.id.item_download_speed);
        }
    }

    private class DirHolder extends RecyclerView.ViewHolder {
        TextView title;
        View bg;

        DirHolder(View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.item_download_bg);
            title = itemView.findViewById(R.id.item_download_title);
        }
    }
}
