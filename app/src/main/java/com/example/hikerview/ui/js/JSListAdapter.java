package com.example.hikerview.ui.js;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikerview.R;
import com.example.hikerview.ui.js.model.JsRule;
import com.example.hikerview.utils.StringUtil;

import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

class JSListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<JsRule> list;
    private OnItemClickListener onItemClickListener;

    JSListAdapter(Context context, List<JsRule> list) {
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ArticleListRuleHolder(LayoutInflater.from(context).inflate(R.layout.item_js_rect_radius, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof ArticleListRuleHolder) {
            ArticleListRuleHolder holder = (ArticleListRuleHolder) viewHolder;
            String name = list.get(position).getName();
            String[] names = name.split("_");
            String title = names.length <= 1 ? names[0] : StringUtil.arrayToString(names, 1, "_");
            holder.item_rect_text.setText(title);
            if ("global".equals(names[0])) {
                holder.desc.setText("全局插件（global）");
            } else {
                holder.desc.setText(names[0]);
            }
            if (list.get(position).isEnable()) {
                holder.btn_bg.setBackground(context.getResources().getDrawable(R.drawable.ripple_grey));
            } else {
                holder.btn_bg.setBackground(context.getResources().getDrawable(R.drawable.ripple_disabled_grey));
            }
            holder.btn_bg.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0) {
                        onItemClickListener.onClick(v, holder.getAdapterPosition());
                    }
                }
            });
            holder.btn_bg.setOnLongClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0) {
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

    private class ArticleListRuleHolder extends RecyclerView.ViewHolder {
        TextView item_rect_text, desc;
        View btn_bg;

        ArticleListRuleHolder(View itemView) {
            super(itemView);
            btn_bg = itemView.findViewById(R.id.btn_bg);
            item_rect_text = itemView.findViewById(R.id.item_rect_text);
            desc = itemView.findViewById(R.id.item_rect_text_desc);
        }
    }
}
