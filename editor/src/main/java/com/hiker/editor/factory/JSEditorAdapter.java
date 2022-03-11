package com.hiker.editor.factory;

import android.app.Activity;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionAdapter;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionType;
import com.github.ahmadaghazadeh.editor.processor.language.JSLanguage;
import com.github.ahmadaghazadeh.editor.processor.utils.text.SymbolsTokenizer;
import com.hiker.editor.R;
import com.hiker.editor.editor.jsc.CodePane;
import com.hiker.editor.editor.jsc.CodePaneBg;
import com.hiker.editor.editor.jsc.CodeText;
import com.hiker.editor.editor.jsc.PreformEdit;
import com.hiker.editor.editor.jsc.ZoomCodePaneView;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/7/13
 * 时间：At 14:36
 */

public class JSEditorAdapter implements EditorAdapter {
    private CodePane codePane;
    private PreformEdit preformEdit;
    private Activity context;
    private View rootView;


    public JSEditorAdapter(Activity context) {
        this.context = context;
    }

    public JSEditorAdapter(Activity context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }

    @Override
    public void setText(String text) {
        codePane.getCodeText().setText(text);
    }

    @Override
    public Editable getEditable() {
        return codePane.getCodeText().getText();
    }


    @Override
    public String getText() {
        return getEditable() == null ? null : getEditable().toString();
    }

    @Override
    public void init(int editorId, int parentId) {
        CodePaneBg codePaneBg = findViewById(editorId);
        codePane = codePaneBg.getCodePane();
        preformEdit = new PreformEdit(codePane.getCodeText());
        ZoomCodePaneView zoomCodePaneView = findViewById(parentId);
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
        ArrayList<SuggestionItem> suggestionItemList = new ArrayList<>();
        for (String suggestionItem : data) {
            suggestionItemList.add(new SuggestionItem(SuggestionType.TYPE_OUTSIDE, suggestionItem));
        }
        for (String name : new JSLanguage().getAllCompletions()) {
            suggestionItemList.add(new SuggestionItem(SuggestionType.TYPE_METHOD, name));
        }
        SuggestionAdapter mAdapter =
                new SuggestionAdapter(codePane.getCodeText().getContext(), R.layout.item_list_suggest, suggestionItemList);
        codePane.getCodeText().setAdapter(mAdapter);
        codePane.getCodeText().setDynamicSuggestionsConsumer(mAdapter::setDynamicNames);
        SymbolsTokenizer mTokenizer = new SymbolsTokenizer();
        codePane.getCodeText().setTokenizer(mTokenizer);
        codePane.getCodeText().setThreshold(2);
    }

    @Override
    public void undo() {
        preformEdit.undo();
    }

    @Override
    public void redo() {
        preformEdit.redo();
    }

    @Override
    public void setSyntaxHighlight(boolean syntaxHighlight) {

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

    public CodeText getCodeText() {
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