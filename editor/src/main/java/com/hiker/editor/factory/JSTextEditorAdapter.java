package com.hiker.editor.factory;

import android.app.Activity;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.hiker.editor.editor.jsc.CodeTextView;
import com.hiker.editor.editor.jsc.CodeTextViewPane;
import com.hiker.editor.editor.jsc.PreformTextView;
import com.hiker.editor.editor.jsc.ZoomCodeTextPaneView;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/7/13
 * 时间：At 14:36
 */

public class JSTextEditorAdapter implements EditorAdapter {
    private CodeTextViewPane codePane;
    private PreformTextView preformEdit;
    private Activity context;
    private ZoomCodeTextPaneView zoomCodePaneView;
    private View rootView;

    public JSTextEditorAdapter(Activity context) {
        this.context = context;
    }

    public JSTextEditorAdapter(Activity context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }

    @Override
    public void setText(String text) {
        codePane.getCodeText().setText(text);
    }

    @Override
    public String getText() {
        return codePane.getCodeText().getText().toString();
    }

    @Override
    public Editable getEditable() {
        return codePane.getCodeText().getEditableText();
    }

    @Override
    public void init(int editorId, int parentId) {
        codePane = findViewById(editorId);
        preformEdit = new PreformTextView(codePane.getCodeText());
        zoomCodePaneView = findViewById(parentId);
        float zoomScale = 0.05f;// 缩放比例
        zoomCodePaneView.init(codePane, zoomScale);
    }

    @Override
    public void requestFocus() {
        codePane.getCodeText().requestFocus();
    }


    @Override
    public TextView getTextView() {
        return codePane.getCodeText();
    }

    @Override
    public void loadSuggestions(List<String> data) {
        //nothing
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
        return zoomCodePaneView.getTextNowSize();
    }

    @Override
    public void setTextNowSize(float textNowSize) {
        zoomCodePaneView.setTextNowSize(textNowSize);
    }

    @Override
    public float getScale() {
        return zoomCodePaneView.getScale();
    }

    @Override
    public void setScale(float scale) {
        zoomCodePaneView.setScale(scale);
    }

    public CodeTextView getCodeText() {
        return codePane.getCodeText();
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