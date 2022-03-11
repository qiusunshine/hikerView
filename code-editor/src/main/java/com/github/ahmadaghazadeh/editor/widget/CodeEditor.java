package com.github.ahmadaghazadeh.editor.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;

import com.github.ahmadaghazadeh.editor.R;
import com.github.ahmadaghazadeh.editor.document.commons.LinesCollection;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionType;
import com.github.ahmadaghazadeh.editor.keyboard.ExtendedKeyboard;
import com.github.ahmadaghazadeh.editor.processor.TextNotFoundException;
import com.github.ahmadaghazadeh.editor.processor.TextProcessor;
import com.github.ahmadaghazadeh.editor.processor.language.Language;
import com.github.ahmadaghazadeh.editor.processor.language.LanguageProvider;
import com.github.ahmadaghazadeh.editor.processor.utils.DefaultSetting;
import com.github.ahmadaghazadeh.editor.processor.utils.ITextProcessorSetting;
import com.github.ahmadaghazadeh.editor.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

public class CodeEditor extends FrameLayout {
    FrameLayout rootView;
    boolean isReadOnly = false;
    boolean isShowExtendedKeyboard = false;
    int preHeight = 0;
    private Context context;
    private boolean mHorizontallyScrolling;

    public TextProcessor getTextProcessor() {
        return editor;
    }

    private TextProcessor editor;
    private Language language;
    private LinesCollection lineNumbers;
    private Editable text;
    private ITextProcessorSetting setting;

    public ExtendedKeyboard getExtendedKeyboard() {
        return recyclerView;
    }

    private ExtendedKeyboard recyclerView;
    private ICodeEditorTextChange codeEditorTextChange;
    private boolean isDirty; //На данный момент не используется

    public CodeEditor(Context context) {
        super(context);
        init(context, null);
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, null);
    }

    @BindingAdapter(value = {"code", "lang", "isReadOnly", "isShowExtendedKeyboard"})
    public static void setCodeView(CodeEditor view, MutableLiveData<String> code, MutableLiveData<String> lang,
                                   boolean isReadOnly,
                                   boolean isShowExtendedKeyboard) {
        if (view == null) {
            return;
        }
        if (code != null) {
            view.setText(code.getValue(), 1);
        }
        if (lang != null) {
            view.setLanguage(LanguageProvider.getLanguage(lang.getValue()));
        }
        view.setReadOnly(isReadOnly);
        view.setShowExtendedKeyboard(isShowExtendedKeyboard);

    }

    public void setOnTextChange(ICodeEditorTextChange onTextChange) {
        codeEditorTextChange = onTextChange;
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (codeEditorTextChange != null) {
                    codeEditorTextChange.onTextChange(s.toString());
                }
            }
        });
    }

    private void init(Context context, AttributeSet attrs) {
        try {
            // removeAllViews();
            this.context = context;
            initEditor();
            String code = "";
            String lang = "html";
            isReadOnly = false;
            isShowExtendedKeyboard = false;
            if (attrs != null) {
                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CodeEditor, 0, 0);
                if (a.hasValue(R.styleable.CodeEditor_code)) {
                    code = a.getString(R.styleable.CodeEditor_code);
                }
                if (a.hasValue(R.styleable.CodeEditor_lang)) {
                    lang = a.getString(R.styleable.CodeEditor_lang);
                }
                isReadOnly = a.getBoolean(R.styleable.CodeEditor_isReadOnly, false);
                isShowExtendedKeyboard = a.getBoolean(R.styleable.CodeEditor_isShowExtendedKeyboard, true);
                a.recycle();

            }
            FrameLayout.LayoutParams rootViewParam = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            rootViewParam.gravity = Gravity.BOTTOM;
            rootView = new FrameLayout(context);
            rootView.setLayoutParams(rootViewParam);
            GutterView gutterView = new GutterView(context);
            LinearLayout.LayoutParams paramsGutter = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            //paramsGutter.alignWithParent = true;
            paramsGutter.gravity = Gravity.START;
            gutterView.setLayoutParams(paramsGutter);
            rootView.addView(gutterView);


            editor = new TextProcessor(context);
            FrameLayout.LayoutParams paramsTxtprocessor = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            editor.setLayoutParams(paramsTxtprocessor);
            editor.setScrollBarStyle(SCROLLBARS_OUTSIDE_INSET);
            editor.setGravity(Gravity.TOP | Gravity.START);

            TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ThemeAttributes, 0, 0);
            try {
                int colorResource = a.getColor(R.styleable.ThemeAttributes_colorDocBackground, getResources().getColor(R.color.colorDocBackground));
                editor.setBackgroundColor(colorResource);
            } finally {
                a.recycle();
            }

            a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ThemeAttributes, 0, 0);
            try {
                int colorResource = a.getColor(R.styleable.ThemeAttributes_colorDocText, getResources().getColor(R.color.colorDocText));
                editor.setTextColor(colorResource);
            } finally {
                a.recycle();
            }


            editor.setLayerType(LAYER_TYPE_SOFTWARE, new TextPaint());
            rootView.addView(editor);

            editor.init(this);
            editor.setReadOnly(isReadOnly);

            FastScrollerView mFastScrollerView = new FastScrollerView(context);
            FrameLayout.LayoutParams fastParam = new FrameLayout.LayoutParams(DisplayUtils.dpToPx(context, 20), LayoutParams.MATCH_PARENT);
            //fastParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, TRUE);
            fastParam.gravity = Gravity.END;

            mFastScrollerView.setLayoutParams(fastParam);
            rootView.addView(mFastScrollerView);
            mFastScrollerView.link(editor); //подключаем FastScroller к редактору

            gutterView.link(editor, lineNumbers); //подключаем Gutter к редактору
            LinesCollection lines = new LinesCollection();
            lines.add(0, 0);
            setLanguage(LanguageProvider.getLanguage(lang)); //ставим язык
            setText(code, 1); //заполняем поле текстом
            setLineStartsList(lines); //подгружаем линии
            refreshEditor(); //подключаем все настройки
            editor.enableUndoRedoStack();

            recyclerView = new ExtendedKeyboard(context);


            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = rootView.getHeight(); //height is ready
                    if (preHeight != height) {

                        FrameLayout.LayoutParams recyclerViewParam = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        recyclerViewParam.setMargins(0, height - 100, 0, 0);
                        recyclerView.setLayoutParams(recyclerViewParam);
                        paramsTxtprocessor.setMargins(0, 0, 0, 100);
                        editor.setLayoutParams(paramsTxtprocessor);
                        preHeight = height;
                    }

                }
            });


            recyclerView.setListener((view, symbol) -> {
                        if (symbol.getShowText().endsWith("End")) {
                            String text = editor.getText().toString();
                            int x = text.indexOf("\n", editor.getSelectionStart());
                            if (x == -1) {
                                x = text.length();
                            }
                            editor.setSelection(x);

                        }  else if (symbol.getShowText().endsWith("←")) {
                            int x = editor.getSelectionStart() - 1;
                            if (x < 0) {
                                x = 0;
                            }
                            editor.setSelection(x);
                        } else if (symbol.getShowText().endsWith("→")) {
                            String text = editor.getText().toString();
                            int x = editor.getSelectionStart() + 1;
                            if (x > text.length()) {
                                x = text.length();
                            }
                            editor.setSelection(x);
                        } else if (symbol.getShowText().endsWith("Del")) {
                            String text = editor.getText().toString();
                            int start = editor.getSelectionStart();
                            if (start < 0 || start + 1 >= text.length()) {
                                //nothing
                            } else {
                                editor.getText().delete(start, start + 1);
                            }
                        } else {
                            editor.getText().insert(editor.getSelectionStart(), symbol.getWriteText());
                        }

                    }
            );
            setShowExtendedKeyboard(isShowExtendedKeyboard);
            rootView.addView(recyclerView);
            addView(rootView);

        } catch (Exception ex) {
            ex.getMessage();
        }
    }


//    public   int getScreenHeight() {
//        int mRealSizeHeight=0;
//        WindowManager windowManager =
//                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        final Display display = windowManager.getDefaultDisplay();
//        Point outPoint = new Point();
//        if (Build.VERSION.SDK_INT >= 19) {
//            // include navigation bar
//            display.getRealSize(outPoint);
//        } else {
//            // exclude navigation bar
//            display.getSize(outPoint);
//        }
//        if (outPoint.y > outPoint.x) {
//            mRealSizeHeight = outPoint.y;
//            //mRealSizeWidth = outPoint.x;
//        } else {
//            mRealSizeHeight = outPoint.x;
//            //mRealSizeWidth = outPoint.y;
//        }
//        return mRealSizeHeight;
//    }


    public void setShowExtendedKeyboard(Boolean showExtendedKeyboard) {
        if (recyclerView != null) {
            recyclerView.setVisibility(showExtendedKeyboard ? VISIBLE : GONE);
        }
    }

    public void initEditor(String code, String lang) {
        setText(code, 1);
        setLanguage(LanguageProvider.getLanguage(lang));
    }

    public void refreshEditor() {
        if (editor != null) {
            editor.setTextSize(setting.getFontSize());
            mHorizontallyScrolling = !setting.getWrapContent();
            editor.setHorizontallyScrolling(mHorizontallyScrolling);
            editor.setShowLineNumbers(setting.getShowLineNumbers());
            editor.setBracketMatching(setting.getBracketMatching());
            editor.setHighlightCurrentLine(setting.getHighlightCurrentLine());
            editor.setCodeCompletion(setting.getCodeCompletion());
            editor.setPinchZoom(setting.getPinchZoom());
            editor.setInsertBrackets(setting.getInsertBracket());
            editor.setIndentLine(setting.getIndentLine());
            editor.refreshTypeface();
            editor.refreshInputType();
            editor.setMovementMethod(new ScrollingMovementMethod());
            editor.setTextIsSelectable(true);
        }
    }

    public void setHorizontallyScrolling(boolean yes) {
        mHorizontallyScrolling = yes;
        editor.setHorizontallyScrolling(yes);
    }

    public boolean getHorizontallyScrolling() {
        return mHorizontallyScrolling;
    }

    private void initEditor() {
        setting = new DefaultSetting(context);
        lineNumbers = new LinesCollection();
    }

    public ITextProcessorSetting getSetting() {
        return setting;
    }

    public void setSetting(ITextProcessorSetting setting) {
        this.setting = setting;
    }

    private void setDirty(boolean dirty) {
        isDirty = dirty;
        //тут будет добавление "*" после названия файла если документ был изменен
    }

    public String getText() {
        if (text != null)
            return text.toString();
        else
            return "";
    }

    @WorkerThread
    @Nullable
    public Language getLanguage() {
        return language;
    }

    public void setLanguage(@Nullable Language language) {
        this.language = language;
    }

    //region METHODS_DOC

    /**
     * Методы для редактора, чтобы менять их в "Runtime".
     */

    public void setReadOnly(boolean readOnly) {
        if (editor != null)
            editor.setReadOnly(readOnly);
    }

    public void setSyntaxHighlight(boolean syntaxHighlight) {
        if (editor != null)
            editor.setSyntaxHighlight(syntaxHighlight);
    }

    //endregion METHODS_DOC

    //region LINES

    public void setLineStartsList(LinesCollection list) {
        lineNumbers = list;
    }

    public LinesCollection getLinesCollection() {
        return lineNumbers;
    }

    public int getLineCount() {
        return lineNumbers.getLineCount();
    }

    public int getLineForIndex(int index) {
        return lineNumbers.getLineForIndex(index);
    }

    public int getIndexForStartOfLine(int line) {
        return lineNumbers.getIndexForLine(line);
    }

    public int getIndexForEndOfLine(int line) {
        if (line == getLineCount() - 1) {
            return text.length();
        }
        return lineNumbers.getIndexForLine(line + 1) - 1;
    }

    public void replaceText(int start, int end, Editable text) {
        replaceText(start, end, text.toString());
    }

    public void replaceText(int start, int end, String text) {
        int i;
        if (this.text == null) {
            this.text = Editable.Factory.getInstance().newEditable("");
        }
        if (end >= this.text.length()) {
            end = this.text.length();
        }
        int newCharCount = text.length() - (end - start);
        int startLine = getLineForIndex(start);
        for (i = start; i < end; i++) {
            if (this.text.charAt(i) == '\n') {
                lineNumbers.remove(startLine + 1);
            }
        }
        lineNumbers.shiftIndexes(getLineForIndex(start) + 1, newCharCount);
        for (i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineNumbers.add(getLineForIndex(start + i) + 1, (start + i) + 1);
            }
        }
        if (start > end) {
            end = start;
        }
        if (start > this.text.length()) {
            start = this.text.length();
        }
        if (end > this.text.length()) {
            end = this.text.length();
        }
        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }
        this.text.replace(start, end, text);
        setDirty(true);
    }

    public void setText(String text, int flag) {
        if (text != null) {
            setText(Editable.Factory.getInstance().newEditable(text), flag);
        } else {
            setText(Editable.Factory.getInstance().newEditable(""), flag);
        }
    }

    public void setText(Editable text, int flag) {
        if (flag == 1) {
            // this.text = text;
            if (editor != null)
                editor.setText(text);
            return;
        }
        int length = 0;
        if (text != null) {
            length = text.length();
        }
        replaceText(0, length, text);
        setDirty(false);
    }

    //endregion LINES

    //region METHODS

    public void insert(@NonNull CharSequence text) {
        if (editor != null)
            editor.insert(text);
    }

    public void cut() throws TextNotFoundException {
        if (editor != null)
            editor.cut();
        else
            throw new TextNotFoundException();
    }

    public void copy() throws TextNotFoundException {
        if (editor != null)
            editor.copy();
        else
            throw new TextNotFoundException();
    }

    public void paste() throws TextNotFoundException {
        if (editor != null)
            editor.paste();
        else
            throw new TextNotFoundException();
    }

    public void undo() throws TextNotFoundException {
        if (editor != null)
            editor.undo();
        else
            throw new TextNotFoundException();
    }

    public void redo() throws TextNotFoundException {
        if (editor != null)
            editor.redo();
        else
            throw new TextNotFoundException();
    }

    public void selectAll() throws TextNotFoundException {
        if (editor != null)
            editor.selectAll();
        else
            throw new TextNotFoundException();
    }

    public void selectLine() throws TextNotFoundException {
        if (editor != null)
            editor.selectLine();
        else
            throw new TextNotFoundException();
    }

    public void deleteLine() throws TextNotFoundException {
        if (editor != null)
            editor.deleteLine();
        else
            throw new TextNotFoundException();
    }

    public void duplicateLine() throws TextNotFoundException {
        if (editor != null)
            editor.duplicateLine();
        else
            throw new TextNotFoundException();
    }

    public void find(String what, boolean matchCase, boolean regex, boolean wordOnly, Runnable onComplete) throws TextNotFoundException {
        if (editor != null && !what.equals("")) {
            editor.find(what, matchCase, regex, wordOnly, editor.getEditableText());
            onComplete.run();
        } else {
            throw new TextNotFoundException();
        }
    }

    public void replaceAll(String what, String with, Runnable onComplete) throws TextNotFoundException {
        if (editor != null && !what.equals("") && !with.equals("")) {
            editor.replaceAll(what, with);
            onComplete.run();
        } else {
            throw new TextNotFoundException();
        }
    }

    public void gotoLine(int line) throws TextNotFoundException {
        if (editor != null)
            editor.gotoLine(line);
        else
            throw new TextNotFoundException();
    }

    public void showToast(String string, boolean b) {

    }

    public interface ICodeEditorTextChange {
        void onTextChange(String str);
    }


    public void addSuggestionItems(String... suggestionItems) {
        List<SuggestionItem> suggestionItemList = new ArrayList<>();
        for (String suggestionItem : suggestionItems) {
            suggestionItemList.add(new SuggestionItem(SuggestionType.TYPE_KEYWORD, suggestionItem));
        }
        if (editor != null && setting.getCodeCompletion()) {
            editor.setSuggestionItems(suggestionItemList);
            editor.setCodeCompletion(setting.getCodeCompletion());
        }
    }
}
