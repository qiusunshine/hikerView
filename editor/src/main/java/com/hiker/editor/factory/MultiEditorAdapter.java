package com.hiker.editor.factory;

import android.app.Activity;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionType;
import com.github.ahmadaghazadeh.editor.processor.TextProcessor;
import com.github.ahmadaghazadeh.editor.widget.CodeEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/7/13
 * 时间：At 14:36
 */

public class MultiEditorAdapter implements EditorAdapter {

    private CodeEditor codeEditor;
    private Activity context;
    private View rootView;

    public MultiEditorAdapter(Activity context) {
        this.context = context;
    }

    public MultiEditorAdapter(Activity context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }

    @Override
    public void setText(String text) {
        codeEditor.setText(text, 1);
    }

    @Override
    public Editable getEditable() {
        return codeEditor.getTextProcessor().getText();
    }


    @Override
    public String getText() {
        return codeEditor.getText();
    }

    @Override
    public void init(int editorId, int parentId) {
        codeEditor = findViewById(editorId);
        codeEditor.initEditor("", "js");
    }

    @Override
    public void requestFocus() {
        codeEditor.getTextProcessor().requestFocus();
    }

    @Override
    public TextView getTextView() {
        return codeEditor.getTextProcessor();
    }

    @Override
    public void loadSuggestions(List<String> data) {
        ArrayList<SuggestionItem> suggestionItemList = new ArrayList<>();
        for (String suggestionItem : data) {
            suggestionItemList.add(new SuggestionItem(SuggestionType.TYPE_KEYWORD, suggestionItem));
        }
        codeEditor.getTextProcessor().setSuggestionItems(suggestionItemList);
        codeEditor.getTextProcessor().setCodeCompletion(true);
    }

    @Override
    public void undo() {
        try {
            codeEditor.undo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void redo() {
        try {
            codeEditor.redo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public float getTextNowSize() {
        //todo
        return 0;
    }

    @Override
    public void setTextNowSize(float textNowSize) {

        //todo
    }

    @Override
    public float getScale() {
        //todo
        return 0;
    }

    @Override
    public void setScale(float scale) {

        //todo
    }

    public CodeEditor getCodeEditor() {
        return codeEditor;
    }

    public TextProcessor getTextProcessor() {
        return codeEditor.getTextProcessor();
    }

    public void loadExtendedKeyboard(String... symbolList) {
        if (codeEditor.getExtendedKeyboard() == null) {
            return;
        }
        codeEditor.getExtendedKeyboard().addSimpleSymbol(symbolList);
    }

    public void hideExtendedKeyboard() {
        codeEditor.setShowExtendedKeyboard(false);
    }

    @Override
    public void setSyntaxHighlight(boolean syntaxHighlight) {
        codeEditor.setSyntaxHighlight(syntaxHighlight);
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