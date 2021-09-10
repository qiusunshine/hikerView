package com.hiker.editor.factory;

import android.app.Activity;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.hiker.editor.editor.pure.ZoomLinarView;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/7/23
 * 时间：At 14:25
 */

public class PureTextAdapter implements EditorAdapter {

    private EditText editText;
    private Activity context;
    private View rootView;

    public PureTextAdapter(Activity context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }


    @Override
    public void setText(String text) {
        editText.setText(text);
    }

    @Override
    public Editable getEditable() {
        return editText.getText();
    }

    @Override
    public String getText() {
        return editText.getText() == null ? null : editText.getText().toString();
    }

    @Override
    public void init(int editorId, int parentId) {
        editText = findViewById(editorId);
        ZoomLinarView zoomLinarView = findViewById(parentId);
        zoomLinarView.init(editText,0.05f);
    }

    @Override
    public void requestFocus() {
        editText.requestFocus();
    }

    @Override
    public TextView getTextView() {
        return editText;
    }

    @Override
    public void loadSuggestions(List<String> data) {

    }

    @Override
    public void undo() {

    }

    @Override
    public void redo() {

    }

    @Override
    public void setSyntaxHighlight(boolean syntaxHighlight) {

    }

    @Override
    public float getTextNowSize() {
        return 0;
    }

    @Override
    public void setTextNowSize(float textNowSize) {

    }

    @Override
    public float getScale() {
        return 0;
    }

    @Override
    public void setScale(float scale) {

    }

    private <T extends View> T findViewById(@IdRes int id) {
        T view = null;
        if (rootView != null) {
            view = rootView.findViewById(id);
        }
        if (view == null) {
            view = context.findViewById(id);
        }
        return view;
    }
}