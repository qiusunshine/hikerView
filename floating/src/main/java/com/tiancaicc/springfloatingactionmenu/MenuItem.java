package com.tiancaicc.springfloatingactionmenu;


import android.view.View;

import androidx.annotation.ColorRes;

/**
 * Copyright (C) 2016 tiancaiCC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class MenuItem {
    @ColorRes
    private int bgColor;
    private int icon;
    private String label;
    @ColorRes
    private int textColor = android.R.color.white;
    private int diameter = 80;
    private View.OnClickListener onClickListener;

    public MenuItem(int bgColor, int icon, String label, int textColor,View.OnClickListener onClickListener) {
        this.bgColor = bgColor;
        this.icon = icon;
        this.label = label;
        this.textColor = textColor;
        this.onClickListener = onClickListener;
    }

    public MenuItem(int bgColor, int icon, String label, int textColor, int diameter,View.OnClickListener onClickListener) {
        this.bgColor = bgColor;
        this.icon = icon;
        this.label = label;
        this.textColor = textColor;
        this.diameter = diameter;
        this.onClickListener = onClickListener;
    }

    public MenuItem(int bgColor) {
        this.bgColor = bgColor;
    }

    public MenuItem(int bgColor, int icon, String label) {
        this.bgColor = bgColor;
        this.icon = icon;
        this.label = label;
    }

    public int getBgColor() {
        return bgColor;
    }

    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getDiameter() {
        return diameter;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public View.OnClickListener getOnClickListener() {
        return onClickListener;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}
