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

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;

import static android.content.ClipDescription.MIMETYPE_TEXT_HTML;

/**
 * Created by Administrator on 2018/2/11.
 */

public class CodeTextView extends ColorsTextView {
    private static final String TAG = "CodeText";
    JsTextViewCodeParser codeParser;

    public CodeTextView(Context context) {
        super(context);
        init();
    }

    public CodeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        codeParser = new JsTextViewCodeParser(this);
        // 动态解析js代码更新文字颜色
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                codeParser.parse(start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private long drawUseTimeCount = 0;
    private long lastEnterTime = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        long startTime = System.currentTimeMillis();
        super.onDraw(canvas);
        long endTime = System.currentTimeMillis();
        drawUseTimeCount = endTime - startTime;
        Log.e("usetime", "" + drawUseTimeCount);
        Log.e("textlen", "" + getText().length());
    }

    /**
     * 记录用户操作的键盘，避免一次按键，多次输入
     */
    private int defaultDeviceId = -1000;

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            try {
                ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboardManager != null && clipboardManager.getPrimaryClip() != null) {
                    if (MIMETYPE_TEXT_HTML.equals(clipboardManager.getPrimaryClip().getDescription().getMimeType(0))) {
                        Log.d(TAG, "onTextContextMenuItem:  type is text/html, will change text");
                        clipboardManager.setText(clipboardManager.getText().toString());
                    }
                }
            } catch (Exception e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    id = android.R.id.pasteAsPlainText;
                }
                Log.e(TAG, "onTextContextMenuItem: " + e.getMessage(), e);
            }
        }
        return super.onTextContextMenuItem(id);
    }
}
