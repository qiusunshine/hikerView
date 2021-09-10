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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.github.ahmadaghazadeh.editor.keyboard.ExtendedKeyboard;


/**
 * Created by Administrator on 2018/2/11.
 */

public class CodePaneBg extends FrameLayout {
    private CodePane codePane;
    int preHeight = 0;

    private ExtendedKeyboard recyclerView;

    public CodePaneBg(Context context) {
        super(context);
        init(context);
    }

    public CodePaneBg(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        codePane = new CodePane(context);

        addView(codePane);

        recyclerView = new ExtendedKeyboard(getContext());

        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int height = getHeight(); //height is ready
            if (preHeight != height) {
                LayoutParams recyclerViewParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                recyclerViewParam.setMargins(0, height - 100, 0, 0);
                recyclerView.setLayoutParams(recyclerViewParam);
                preHeight = height;
            }
        });

        recyclerView.setListener((view, symbol) -> {
                    if (symbol.getShowText().endsWith("End")) {
                        String text = getCodeText().getText().toString();
                        int x = text.indexOf("\n", getCodeText().getSelectionStart());
                        if (x == -1) {
                            x = text.length();
                        }
                        getCodeText().setSelection(x);
                    } else if (symbol.getShowText().endsWith("Del")) {
                        String text = getCodeText().getText().toString();
                        int start = getCodeText().getSelectionStart();
                        if (start < 0 || start + 1 >= text.length()) {
                            //nothing
                        } else {
                            getCodeText().getText().delete(start, start + 1);
                        }
                    } else if (symbol.getShowText().endsWith("←")) {
                        int x = getCodeText().getSelectionStart() - 1;
                        if (x < 0) {
                            x = 0;
                        }
                        getCodeText().setSelection(x);
                    } else if (symbol.getShowText().endsWith("→")) {
                        String text = getCodeText().getText().toString();
                        int x = getCodeText().getSelectionStart() + 1;
                        if (x > text.length()) {
                            x = text.length();
                        }
                        getCodeText().setSelection(x);
                    } else {
                        getCodeText().getText().insert(getCodeText().getSelectionStart(), symbol.getWriteText());
                    }
                }
        );
        setShowExtendedKeyboard(true);
        addView(recyclerView);
    }


    public void setShowExtendedKeyboard(Boolean showExtendedKeyboard) {
        if (recyclerView != null) {
            if (showExtendedKeyboard) {
                FrameLayout.LayoutParams codePaneParams = (FrameLayout.LayoutParams) codePane.getLayoutParams();
                codePaneParams.setMargins(0, 0, 0, 100);
                codePane.setLayoutParams(codePaneParams);
            } else {
                FrameLayout.LayoutParams codePaneParams = (FrameLayout.LayoutParams) codePane.getLayoutParams();
                codePaneParams.setMargins(0, 0, 0, 0);
                codePane.setLayoutParams(codePaneParams);
            }
            recyclerView.setVisibility(showExtendedKeyboard ? VISIBLE : GONE);
        }
    }

    public ExtendedKeyboard getExtendedKeyboard() {
        return recyclerView;
    }

    public CodeText getCodeText() {
        return codePane.getCodeText();
    }

    public CodePane getCodePane() {
        return codePane;
    }
}
