/*
 * Copyright 2018. who<980008027@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.hikerview.ui.js.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ViewGroup;


/**
 * Created by Administrator on 2018/2/11.
 */

public class CodePane extends HVScrollView {
    CodeText mCodeText;
    int mCodeTextMinHeight;
    int mCodeTextMinWidth;

    public CodePane(Context context) {
        super(context);
        init();

    }

    public CodePane(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setVerticalScrollBarEnabled(true);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //设置背景颜色
        setBackgroundColor(0XFF333333);
        mCodeText = new CodeText(getContext());
        mCodeText.setScrollView(this);
        addView(mCodeText);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //计算CodeText宽高
        int codeWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int codeHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (mCodeTextMinHeight != codeHeight || mCodeTextMinWidth != codeWidth) {
            mCodeTextMinWidth = codeWidth;
            mCodeTextMinHeight = codeHeight;
            mCodeText.setMinWidth(mCodeTextMinWidth);
            mCodeText.setMinHeight(mCodeTextMinHeight);
            invalidate();
            return;
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        //滑动的时候，通知CodeText绘制高亮
        mCodeText.postInvalidate();
    }

    public CodeText getCodeText() {
        return mCodeText;
    }

}
