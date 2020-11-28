package org.adblockplus.libadblockplus.android.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

public class MySubsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;

    public List<MySubscription> getList() {
        return list;
    }

    private List<MySubscription> list;
    private OnClickListener clickListener;

    public MySubsAdapter(Context context, List<MySubscription> list, OnClickListener onClickListener) {
        this.context = context;
        this.list = list;
        this.clickListener = onClickListener;
    }

    public interface OnClickListener {
        void click(String url);

        void longClick(String url);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TitleHolder(LayoutInflater.from(context).inflate(R.layout.my_sub_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof TitleHolder) {
            try {
                TitleHolder holder = (TitleHolder) viewHolder;
                String title = list.get(position).title;
                holder.title.setText(title);
                if (list.get(position).size > 0) {
                    String desc = list.get(position).size + "条规则";
                    if (list.get(position).lastSuccess > 0) {
                        desc = desc + "，更新于" + Utils.getAutoTime(new Date(list.get(position).lastSuccess * 1000));
                    }
                    holder.item_video.setText(desc);
                    holder.item_video.setVisibility(View.VISIBLE);
                } else if (list.get(position).errors > 0) {
                    holder.item_video.setText("出错、请检查规则是否正确或者网络是否流畅");
                    holder.item_video.setVisibility(View.VISIBLE);
                } else {
                    holder.item_video.setVisibility(View.GONE);
                }

                holder.item_bg.setOnClickListener(v -> {
                    try {
                        if (holder.getAdapterPosition() >= 0 && holder.getAdapterPosition() < list.size()) {
                            clickListener.click(list.get(holder.getAdapterPosition()).url);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                holder.item_bg.setOnLongClickListener(v -> {
                    try {
                        if (holder.getAdapterPosition() >= 0 && holder.getAdapterPosition() < list.size()) {
                            clickListener.longClick(list.get(holder.getAdapterPosition()).url);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class TitleHolder extends RecyclerView.ViewHolder {
        TextView title, item_video;
        View item_bg;

        TitleHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_ad_title);
            item_video = itemView.findViewById(R.id.item_video);
            item_bg = itemView.findViewById(R.id.item_bg);
        }
    }
}
