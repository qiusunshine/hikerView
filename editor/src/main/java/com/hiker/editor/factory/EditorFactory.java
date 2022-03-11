package com.hiker.editor.factory;

import android.app.Activity;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ahmadaghazadeh.editor.processor.language.LanguageProvider;
import com.hiker.editor.R;

import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2021/7/13
 * 时间：At 14:28
 */

public class EditorFactory {

    private Activity context;
    private View rootView;

    public enum Editor {
        JSEditEditor,
        MultiCodeEditor,
        PureText,
        JSTextEditor
    }

    public static Editor get(int code) {
        return Editor.values()[code];
    }

    public static int getCode(Editor editor) {
        int c = 0;
        Editor[] editors = Editor.values();
        for (int i = 0; i < editors.length; i++) {
            if(editor == editors[i]){
                return i;
            }
        }
        return c;
    }

    Editor usedEditor;
    EditorAdapter adapter;

    public static int view_editor_multi = R.layout.view_editor_multi;
    public static int view_editor_jsc = R.layout.view_editor_jsc;
    public static int view_editor_pure = R.layout.view_editor_pure;


    public EditorFactory(Activity context) {
        this.context = context;
    }

    public EditorFactory(Activity context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }

    public EditorFactory use(Editor editor) {
        usedEditor = editor;
        if (editor == Editor.PureText) {
            adapter = new PureTextAdapter(context, rootView);
        } else if (editor == Editor.JSEditEditor) {
            adapter = new JSEditorAdapter(context, rootView);
        } else if (editor == Editor.JSTextEditor) {
            adapter = new JSTextEditorAdapter(context, rootView);
        } else {
            adapter = new MultiEditorAdapter(context, rootView);
        }
        return this;
    }

    public EditorFactory use(Editor editor, ViewGroup wrapper) {
        use(editor);
        if (editor == Editor.JSEditEditor) {
            inflate(wrapper, view_editor_jsc);
        } else if (editor == Editor.MultiCodeEditor) {
            inflate(wrapper, view_editor_multi);
        } else if (editor == Editor.PureText) {
            inflate(wrapper, view_editor_pure);
        }
        return this;
    }

    public void change(Editor editor, ViewGroup wrapper) {
        wrapper.removeAllViews();
        use(editor, wrapper);
    }

    public EditorFactory inflate(ViewGroup wrapper, int resourceId) {
        View view = LayoutInflater.from(context).inflate(resourceId, null);
        wrapper.addView(view);
        if (resourceId == view_editor_multi) {
            use(Editor.MultiCodeEditor);
            initEditor(R.id.editor, -1);
        } else if (resourceId == view_editor_jsc) {
            use(Editor.JSEditEditor);
            initEditor(R.id.js_edit_code_pane, R.id.js_edit_code_pane_bg);
        } else if (resourceId == view_editor_pure) {
            use(Editor.PureText);
            initEditor(R.id.editor, R.id.editor_linear_bg);
        }
        return this;
    }

    public EditorFactory initEditor(int editorId, int parentId) {
        if (adapter == null) {
            use(Editor.JSEditEditor);
        }
        adapter.init(editorId, parentId);
        return this;
    }

    public void setText(String text) {
        adapter.setText(text);
    }


    public Editable getEditable() {
        return adapter.getEditable();
    }

    public void setSelectBackgroundColor(int color) {
        if (adapter instanceof JSEditorAdapter) {
            JSEditorAdapter editorAdapter = (JSEditorAdapter) adapter;
            editorAdapter.getCodeText().setSelectBackgroundColor(color);
        } else if (adapter instanceof JSTextEditorAdapter) {
            JSTextEditorAdapter editorAdapter = (JSTextEditorAdapter) adapter;
            editorAdapter.getCodeText().setSelectBackgroundColor(color);
        }
    }

    public String getText() {
        return adapter.getText() == null ? "" : adapter.getText();
    }

    public boolean canBreakWord() {
        if (usedEditor == Editor.PureText || usedEditor == Editor.MultiCodeEditor) {
            return true;
        }
        return false;
    }

    public boolean canExpandWord() {
        if (usedEditor == Editor.PureText) {
            return false;
        }
        return true;
    }

    public void setBreakWord(boolean breakWord) {
        if (usedEditor == Editor.MultiCodeEditor) {
            MultiEditorAdapter multiEditorAdapter = (MultiEditorAdapter) adapter;
            multiEditorAdapter.getCodeEditor().setHorizontallyScrolling(!breakWord);
        } else {
            Toast.makeText(context, "当前编辑器不支持切换换行模式", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isBreakWord() {
        if (usedEditor == Editor.PureText || usedEditor == Editor.JSEditEditor || usedEditor == Editor.JSTextEditor) {
            return false;
        }
        if (usedEditor == Editor.MultiCodeEditor) {
            MultiEditorAdapter multiEditorAdapter = (MultiEditorAdapter) adapter;
            return !multiEditorAdapter.getCodeEditor().getHorizontallyScrolling();
        }
        return true;
    }

    public void requestFocus() {
        adapter.requestFocus();
    }

    public TextView getTextView() {
        return adapter.getTextView();
    }

    public EditText getEditText() {
        TextView textView = adapter.getTextView();
        if (textView instanceof EditText) {
            return (EditText) textView;
        } else {
            return null;
        }
    }

    public void loadSuggestions(List<String> data) {
        adapter.loadSuggestions(data);
    }

    public void loadExtendedKeyboard(String... symbolList) {
        if (adapter instanceof MultiEditorAdapter) {
            MultiEditorAdapter multiEditorAdapter = (MultiEditorAdapter) adapter;
            multiEditorAdapter.loadExtendedKeyboard(symbolList);
        }
    }

    public void hideExtendedKeyboard() {
        if (adapter instanceof MultiEditorAdapter) {
            MultiEditorAdapter multiEditorAdapter = (MultiEditorAdapter) adapter;
            multiEditorAdapter.hideExtendedKeyboard();
        }
    }

    public void undo() {
        adapter.undo();
    }

    public void redo() {
        adapter.redo();
    }

    public void setLanguage(String lang) {
        if (adapter instanceof MultiEditorAdapter) {
            MultiEditorAdapter multiEditorAdapter = (MultiEditorAdapter) adapter;
            multiEditorAdapter.getCodeEditor().setLanguage(LanguageProvider.getLanguage(lang));
        }
    }

    public void setSyntaxHighlight(boolean syntaxHighlight) {
        adapter.setSyntaxHighlight(syntaxHighlight);
    }

    public float getTextNowSize() {
        return adapter.getTextNowSize();
    }

    public void setTextNowSize(float textNowSize) {
        adapter.setTextNowSize(textNowSize);
    }

    public float getScale() {
        return adapter.getScale();
    }

    public void setScale(float scale) {
        adapter.setScale(scale);
    }
} 