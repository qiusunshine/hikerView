package com.hiker.editor.factory;

import android.text.Editable;
import android.widget.TextView;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/7/13
 * 时间：At 14:34
 */

public interface EditorAdapter {
    void setText(String text);

    Editable getEditable();

    String getText();

    void init(int editorId, int parentId);

    void requestFocus();

    TextView getTextView();

    void loadSuggestions(List<String> data);

    void undo();

    void redo();

    void setSyntaxHighlight(boolean syntaxHighlight);

    float getTextNowSize();

    void setTextNowSize(float textNowSize);

    float getScale();

    void setScale(float scale);
}
