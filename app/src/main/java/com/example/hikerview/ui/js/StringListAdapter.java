package com.example.hikerview.ui.js;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikerview.R;

import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

class StringListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;

    public List<String> getList() {
        return list;
    }

    private List<String> list;
    private OnItemClickListener onItemClickListener;

    StringListAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
    }

    interface OnItemClickListener {
        void onDelete(View view, int position);

        void onLongClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RuleHolder(LayoutInflater.from(context).inflate(R.layout.item_ad, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof RuleHolder) {
            RuleHolder holder = (RuleHolder) viewHolder;
            String title = list.get(position);
            holder.title.setText(title);
            holder.delete.setText("编辑");
            holder.delete.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0 && holder.getAdapterPosition() < list.size()) {
                        onItemClickListener.onDelete(v, holder.getAdapterPosition());
                    }
                }
            });
            holder.title.setOnLongClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0 && holder.getAdapterPosition() < list.size()) {
                        onItemClickListener.onLongClick(v, holder.getAdapterPosition());
                    }
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class RuleHolder extends RecyclerView.ViewHolder {
        TextView title, delete;

        RuleHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_ad_title);
            delete = itemView.findViewById(R.id.item_ad_delete);
        }
    }
}
