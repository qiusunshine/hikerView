package com.example.hikerview.ui.dlan;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikerview.R;
import com.qingfeng.clinglibrary.entity.ClingDevice;

import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2017/9/10
 * 时间：At 17:26
 */

public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "DeviceListAdapter";
    private Context context;
    private List<ClingDevice> list;
    private OnDeviceItemClickListener onItemClickListener;

    public DeviceListAdapter(Context context, List<ClingDevice> list, OnDeviceItemClickListener onItemClickListener) {
        this.context = context;
        this.list = list;
        this.onItemClickListener = onItemClickListener;
    }

    public void addDevice(ClingDevice device) {
        list.add(device);
        notifyDataSetChanged();
    }

    public void removeDevice(ClingDevice device) {
        list.remove(device);
        notifyDataSetChanged();
    }

    public interface OnDeviceItemClickListener {
        void onDeviceItemClick(ClingDevice device, boolean isActived);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RuleHolder(LayoutInflater.from(context).inflate(R.layout.item_devices_items, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof RuleHolder) {
            RuleHolder holder = (RuleHolder) viewHolder;
            Log.d(TAG, "onBindViewHolder: ==>" + list.get(position));
            String title = list.get(position).getDevice().getDetails().getFriendlyName();
            holder.listview_item_line_one.setText(title);
            holder.listview_item_line_one.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    if (holder.getAdapterPosition() >= 0 && holder.getAdapterPosition() < list.size()) {
                        onItemClickListener.onDeviceItemClick(list.get(holder.getAdapterPosition()), true);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class RuleHolder extends RecyclerView.ViewHolder {
        TextView listview_item_line_one;

        RuleHolder(View itemView) {
            super(itemView);
            listview_item_line_one = itemView.findViewById(R.id.listview_item_line_one);
        }
    }
}
