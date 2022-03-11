/*
 * Copyright (C) 2018 Light Team Software
 *
 * This file is part of ModPE IDE.
 *
 * ModPE IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ModPE IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ahmadaghazadeh.editor.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.github.ahmadaghazadeh.editor.R;
import com.github.ahmadaghazadeh.editor.keyboard.ExtendedKeyboard;

import java.util.List;


/**
 * @author Trần Lê Duy
 */
public class SymbolAdapter extends RecyclerView.Adapter<SymbolAdapter.ViewHolder> {

    public List<Symbol> getList() {
        return mList;
    }

    private List<Symbol> mList;
    private ExtendedKeyboard.OnKeyListener mListener;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_key, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Symbol symbol = mList.get(position);
        holder.text.setText(symbol.getShowText());
        holder.text.setOnClickListener(v -> {
            if (mListener != null)
                mListener.onKeyClick(v, symbol);
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public void setListKey(List<Symbol> mList) {
        this.mList = mList;
    }

    public void setListener(ExtendedKeyboard.OnKeyListener listener) {
        mListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView text;

        ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text_view);
        }
    }
}
