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


package com.hiker.editor.editor.jsc;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import static android.content.ClipDescription.MIMETYPE_TEXT_HTML;

/**
 * Created by Administrator on 2018/2/11.
 */

public class CodeText extends com.hiker.editor.editor.jsc.ColorsText {
    private static final String TAG = "CodeText";
    com.hiker.editor.editor.jsc.JsCodeParser codeParser;
    private boolean mHorizontallyScrolling = true;

    public CodeText(Context context) {
        super(context);
        init();
    }

    public CodeText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public void setHorizontallyScrolling(boolean yes) {
        mHorizontallyScrolling = yes;
        super.setHorizontallyScrolling(yes);
    }

    public boolean getHorizontallyScrolling() {
        return mHorizontallyScrolling;
    }

    private void init() {
        codeParser = new com.hiker.editor.editor.jsc.JsCodeParser(this);
        // 动态解析js代码更新文字颜色
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Layout layout = getLayout();
                if (layout != null && before == 0 && s.subSequence(start, start + count).toString().equals("\n")) {
                    int line = layout.getLineForOffset(start);
                    int startPos = layout.getLineStart(line);
                    String space = "";
                    for (int i = startPos; i < s.length(); i++) {
                        if (getText().charAt(i) == ' ') {
                            space += " ";
                        } else {
                            break;
                        }
                    }
                    String insert = space;

                    char beforeChar = 0, afterChar = 0;
                    if (start != 0) beforeChar = s.charAt(start - 1);
                    if (start + count < s.length()) afterChar = s.charAt(start + count);
                    if (beforeChar == '{' || beforeChar == '[' || beforeChar == '(') {
                        insert += "    ";
                    }
                    int selectionPos = start + 1 + insert.length();
                    if ((beforeChar == '{' && afterChar == '}') || (beforeChar == '[' && afterChar == ']') || (beforeChar == '(' && afterChar == ')')) {
                        insert += "\n" + space;
                    }
                    getText().insert(start + 1, insert);
                    setSelection(selectionPos);
                    return;
                }
                codeParser.parse(start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                getParent().requestLayout();
            }
        });
    }


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

    private final Object nameLock = new Object();
    private final Set<String> names = new HashSet<>();
    private Consumer<Set<String>> dynamicSuggestionsConsumer;

    public void addName(String name) {
        synchronized (nameLock) {
            this.names.add(name);
            if(dynamicSuggestionsConsumer != null){
                dynamicSuggestionsConsumer.update(this.names);
            }
        }
    }

    public Consumer<Set<String>> getDynamicSuggestionsConsumer() {
        return dynamicSuggestionsConsumer;
    }

    public void setDynamicSuggestionsConsumer(Consumer<Set<String>> dynamicSuggestionsConsumer) {
        this.dynamicSuggestionsConsumer = dynamicSuggestionsConsumer;
    }

    public interface Consumer<T> {
        void update(T data);
    }
}
