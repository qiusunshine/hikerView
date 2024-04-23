package com.example.hikerview.ui.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.hikerview.R;
import com.example.hikerview.model.DownloadRecord;
import com.example.hikerview.ui.browser.model.UrlDetector;
import com.example.hikerview.ui.home.view.MyRoundedCorners;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.TimeUtil;

import java.io.File;
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
        if (viewType == 1) {
            return new RuleHolder(LayoutInflater.from(context).inflate(R.layout.item_download_highlight, parent, false));
        }
        return new RuleHolder(LayoutInflater.from(context).inflate(R.layout.item_download, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return "dir".equals(list.get(position).getVideoType()) ? -1 : (list.get(position).isLastPlay() ? 1 : 0);
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
            holder.title.setText(StringUtil.trimBlanks(title));
            if (DownloadStatusEnum.RUNNING.getCode().equals(record.getStatus()) && record.getSize() > 0) {
                holder.status.setText(FileUtil.getFormatedFileSize(record.getSize()));
            } else {
                if (DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus())
                        && StringUtil.isNotEmpty(record.getPlayPos())) {
                    String pos = record.getPlayPos().split("@@")[0];
                    holder.status.setText(("播放至" + pos));
                } else {
                    String status = getStatus(record);
                    if (DownloadStatusEnum.SUCCESS.getDesc().equals(status) && StringUtil.isNotEmpty(record.getFileExtension())) {
                        holder.status.setText(record.getFileExtension());
                    } else {
                        holder.status.setText(status);
                    }
                }
            }
            if (DownloadStatusEnum.SUCCESS.getCode().equals(record.getStatus()) && record.getSize() > 0
                    && (UrlDetector.isImage(record.getFullName())
                    || (UrlDetector.isVideoOrMusic(record.getFullName(), true) && !record.getFullName().contains(".m3u8")))) {
                String normalPath = DownloadManager.getNormalFilePath(record);
                if (normalPath != null) {
                    File file = new File(normalPath);
                    if (file.exists() && !file.isDirectory()) {
                        holder.imageView.setVisibility(View.VISIBLE);
                        RequestOptions requestOptions = RequestOptions.bitmapTransform(new MyRoundedCorners());
                        if (StringUtil.isNotEmpty(record.getPlayPos())) {
                            String[] s = record.getPlayPos().split("@@");
                            if (s.length > 2) {
                                long t = Long.parseLong(s[2]);
                                requestOptions.frame(t * 1000);
                                //requestOptions.set(VideoDecoder.FRAME_OPTION, MediaMetadataRetriever.OPTION_CLOSEST);
                            }
                        }
                        Glide.with(context)
                                .load(file.getAbsolutePath())
                                .apply(requestOptions)
                                .into(holder.imageView);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                    }
                } else {
                    holder.imageView.setVisibility(View.GONE);
                }
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
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
                if (speed == 0) {
                    holder.speed.setText("光速");
                } else {
                    holder.speed.setText((FileUtil.getFormatedFileSize(speed) + "/s"));
                }
            }
            if (record.isShowTime()) {
                holder.item_download_time.setVisibility(View.VISIBLE);
                holder.item_download_time.setText(TimeUtil.formatTime(record.getSaveTime()));
            } else {
                holder.item_download_time.setVisibility(View.GONE);
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
            int id = R.drawable.icon_folder3;
            if ("安装包".equals(record.getSourcePageTitle())) {
                id = R.drawable.icon_app3;
            } else if ("压缩包".equals(record.getSourcePageTitle())) {
                id = R.drawable.icon_zip2;
            } else if ("音乐/音频".equals(record.getSourcePageTitle())) {
                id = R.drawable.icon_music3;
            } else if ("文档/电子书".equals(record.getSourcePageTitle())) {
                id = R.drawable.icon_txt2;
            } else if ("其它格式".equals(record.getSourcePageTitle())) {
                id = R.drawable.icon_unknown;
            } else if ("图片".equals(record.getSourcePageTitle())) {
                id = R.drawable.icon_pic3;
            }
            holder.imageView.setImageDrawable(context.getDrawable(id));
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
        TextView title, status, speed, downloaded, item_download_time;
        View bg;
        ImageView imageView;

        RuleHolder(View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.item_download_bg);
            title = itemView.findViewById(R.id.item_download_title);
            status = itemView.findViewById(R.id.item_download_status);
//            total = itemView.findViewById(R.id.item_download_total);
            downloaded = itemView.findViewById(R.id.item_download_downloaded);
            item_download_time = itemView.findViewById(R.id.item_download_time);
            speed = itemView.findViewById(R.id.item_download_speed);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    private class DirHolder extends RecyclerView.ViewHolder {
        TextView title;
        View bg;
        ImageView imageView;

        DirHolder(View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.item_download_bg);
            title = itemView.findViewById(R.id.item_download_title);
            imageView = itemView.findViewById(R.id.item_reult_img);
        }
    }
}
