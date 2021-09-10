package com.hiker.editor.editor.jsc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionAdapter;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionType;
import com.github.ahmadaghazadeh.editor.processor.utils.Converter;
import com.github.ahmadaghazadeh.editor.processor.utils.Logger;
import com.github.ahmadaghazadeh.editor.processor.utils.text.SymbolsTokenizer;
import com.hiker.editor.R;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorsText extends AppCompatMultiAutoCompleteTextView {
    private static final String TAG = "ColorsText";
    static final int VERY_WIDE = 1024 * 1024; // XXX should be much larger
    private Object colorLock = new Object();
    // 代码高亮颜色数组
    private int[] codeColors;
    // 滑动组件
    private View scrollView;
    // 最大行号：根据\n数量得到
    private int mMaxLineNumber;
    // 行号内边距
    private int mNumberPadding;
    private int mTextPadding;
    private int mLineNumberBgStrokeWidth;
    private int defaultTextColor = 0xffffffff;
    private int lineNumberColor = 0x99ffffff;
    private int lineNumberBackgroundColor = 0x99ffffff;
    private int lineNumberSplitColor = 0x99ffffff;
    private int lineNumberSplitWidth = 1;
    private int cursorColor = 0xffffffff;
    private int selectBackgroundColor = 0x33ffffff;
    int h;
    int mCharHeight = 0;
    int mGutterWidth;
    int mIdealMargin;

    public void setLineNumberColor(int lineNumberColor) {
        this.lineNumberColor = lineNumberColor;
    }

    public void setLineNumberBackgroundColor(int lineNumberBackgroundColor) {
        this.lineNumberBackgroundColor = lineNumberBackgroundColor;
    }

    public void setLineNumberSplitColor(int lineNumberSplitColor) {
        this.lineNumberSplitColor = lineNumberSplitColor;
    }

    public void setSelectBackgroundColor(int selectBackgroundColor) {
        this.selectBackgroundColor = selectBackgroundColor;
    }


    public void setDefaultTextColor(int defaultTextColor) {
        this.defaultTextColor = defaultTextColor;
    }

    public ColorsText(Context context) {
        super(context);
        init();
    }

    public ColorsText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // 设置光标颜色
    public void setCursorColor(int cursorColor) {
        this.cursorColor = cursorColor;
    }

    private void init() {
        mNumberPadding = DpiUtils.dip2px(getContext(), 5);
        mTextPadding = DpiUtils.dip2px(getContext(), 4);
        mLineNumberBgStrokeWidth = DpiUtils.dip2px(getContext(), 2);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
        setGravity(Gravity.START);
        // 设值背景透明
        setBackgroundColor(Color.TRANSPARENT);
        // 设值字体大小
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        // 设置字体颜色(透明是为了兼容不能反射绘制光标以及选择文字背景的情况)
        setTextColor(Color.TRANSPARENT);
        setTypeface(Typeface.MONOSPACE);
        setPadding(0, DpiUtils.dip2px(getContext(), 2), DpiUtils.dip2px(getContext(), 8), DpiUtils.dip2px(getContext(), 48));
        mIdealMargin = Converter.dpAsPixels(this, 4);
        postInit();
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onPopupChangePosition();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        post(() -> {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            int width = outMetrics.widthPixels;
            int height = outMetrics.heightPixels;
            height = height > width ? (int) (height * 0.5f) : width;
            // 1/2 width of screen
            setDropDownHeight((int) (height * .5f));
            // 0.5 height of screen
            setDropDownWidth((int) (width * 0.5f));
        });
    }

    public void loadSuggestions(List<String> data) {
        setSuggestData(data.toArray(new String[0]));
        SymbolsTokenizer mTokenizer = new SymbolsTokenizer();
        setTokenizer(mTokenizer);
        setThreshold(2); //задержка перед отображением
    }


    protected void onPopupChangePosition() {
        try {
            Layout layout = getLayout();
            if (layout != null) {

                int pos = getSelectionStart();
                int line = layout.getLineForOffset(pos);
                int baseline = layout.getLineBaseline(line);
                int ascent = layout.getLineAscent(line);

                Rect bounds = new Rect();
                Paint textPaint = getPaint();
                String sample = "A";
                textPaint.getTextBounds(sample, 0, sample.length(), bounds);
                float x = layout.getPrimaryHorizontal(pos);
                float y = baseline + ascent;

                int offsetHorizontal = (int) x + mGutterWidth;
                setDropDownHorizontalOffset(offsetHorizontal);

                int offsetVertical = (int) ((y + mCharHeight) - getScrollY());
                int tmp = -h + ((offsetVertical * 2 / (mCharHeight)) * (mCharHeight / 2)) + (mCharHeight / 2);
                setDropDownVerticalOffset(tmp);
            }
        } catch (Exception e) {
            Logger.error(TAG, e);
        }
    }

    /**
     * Обновление панели с номерами линий.
     */
    public void updateGutter() {
        int max = 3;
        if (getLayout() != null) {
            TextPaint paint = getLayout().getPaint();
            if (paint != null) {
                int mLineNumberDigitCount = Integer.toString(getLineCount()).length();
                int widestNumber = 0;
                float widestWidth = 0.0f;
                for (int i = 0; i <= 9; i++) {
                    float width = paint.measureText(Integer.toString(i));
                    if (width > widestWidth) {
                        widestNumber = i;
                        widestWidth = width;
                    }
                }
                StringBuilder builder = new StringBuilder();
                if (mLineNumberDigitCount >= 3) {
                    max = mLineNumberDigitCount;
                }
                for (int i = 0; i < max; i++) {
                    builder.append(widestNumber);
                }
                mGutterWidth = (int) paint.measureText(builder.toString());
                mGutterWidth += mIdealMargin;
            }
        }
    }

    protected void postInit() {
        postInvalidate();
        refreshDrawableState();
        invalidateCharHeight();
    }


    protected void invalidateCharHeight() {
        mCharHeight = (int) Math.ceil(getPaint().getFontSpacing());
        mCharHeight = (int) getPaint().measureText("M");
    }

    protected int getHeightVisible() {
        Rect rect = new Rect();
        getWindowVisibleDisplayFrame(rect);
        return rect.bottom - rect.top;
    }

    /**
     * 生成每行文字对应的行号，如果行首为换行符则需要显示行号
     *
     * @return
     */
    public Map<Integer, String> getLineNumbers() {
        Map<Integer, String> maps = new HashMap<>();
        Layout layout = getLayout();
        if (layout == null) {
            return maps;
        }
        int lineNumber = 1;
        maps.put(0, Integer.toString(lineNumber));
        int lineCount = getLineCount();
        mMaxLineNumber = 1;
        for (int i = 1; i < lineCount; i++) {
            int charPos = layout.getLineStart(i);
            if (getText().charAt(charPos - 1) == '\n') {
                lineNumber++;
                maps.put(i, Integer.toString(lineNumber));
                mMaxLineNumber = lineNumber;
            }
        }
        return maps;
    }


    /**
     * 获取可视范围的文字首行与尾行
     *
     * @param rect
     * @param ret
     */
    public void getLineRangeForDraw(Rect rect, int[] ret) {
        Layout layout = getLayout();
        final int top = Math.max(rect.top, 0);
        final int bottom = Math.min(layout.getLineTop(layout.getLineCount()), rect.bottom);
        if (top >= bottom) {
            ret[0] = -1;
            ret[1] = -1;
            return;
        }
        ret[0] = layout.getLineForVertical(top);
        ret[1] = layout.getLineForVertical(bottom);
    }


    /**
     * 计算可视范围文字的首字位置
     *
     * @param widths
     * @return [位置，偏移]
     */
    public float[] getLineFirstCharPosForDraw(float[] widths) {
        float max = (getViewScrollX() - getPaddingLeft());
        int count = 0;
        float size = 0;
        for (int i = 0; i < widths.length; i++) {

            if (size + widths[i] >= max) {
                break;
            }
            size += widths[i];
            count++;
        }
        return new float[]{count, size};
    }


    Method getUpdatedHighlightPathMthod;

    /**
     * 获取绘制选择文字背景的Path路径
     *
     * @return
     * @throws InvocationTargetException
     */
    Path getUpdatedHighlightPath() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        if (getUpdatedHighlightPathMthod == null) {
            getUpdatedHighlightPathMthod = TextView.class
                    .getDeclaredMethod("getUpdatedHighlightPath");
            getUpdatedHighlightPathMthod.setAccessible(true);
        }
        return (Path) getUpdatedHighlightPathMthod.invoke(this);
    }


    Field mHighlightPaintField;

    /**
     * 获取绘制选择文字背景的Paint画笔
     *
     * @return
     */
    Paint getHighlightPaint() throws IllegalAccessException,
            NoSuchFieldException {
        if (mHighlightPaintField == null) {
            mHighlightPaintField = TextView.class
                    .getDeclaredField("mHighlightPaint");
            mHighlightPaintField.setAccessible(true);
        }
        return (Paint) mHighlightPaintField.get(this);
    }

    Field mEditorField;

    /**
     * 获取用于编辑的Editor
     *
     * @return
     */
    Object getEditor() throws NoSuchFieldException, IllegalAccessException {
        if (mEditorField == null) {
            mEditorField = TextView.class.getDeclaredField("mEditor");
            mEditorField.setAccessible(true);
        }
        return mEditorField.get(this);
    }


    /**
     * 绘制光标以及文字选择背景
     *
     * @param canvas
     * @throws InvocationTargetException
     */
    void drawCursorAndSelectPath(Canvas canvas) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            NoSuchFieldException {

        final int selectionStart = getSelectionStart();
        final int selectionEnd = getSelectionEnd();
        Path highlight = getUpdatedHighlightPath();
        Paint mHighlightPaint = getHighlightPaint();
        //设置选择文字背景颜色
        if (selectBackgroundColor != 0) {
            mHighlightPaint.setColor(selectBackgroundColor);
        }
        canvas.save();

        //getCompoundPaddingLeft获取真正的左内边距，getExtendedPaddingTop获取真正的上外边距
        canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());
        try {

            if (highlight != null) {
                if (selectionEnd == selectionStart) {
                    //绘制光标
                    Paint paint = getPaint();
                    paint.setColor(cursorColor);
                    canvas.drawRect(getCursorRect(), paint);
                } else {
                    //绘制选择文字阴影
                    canvas.drawPath(highlight, mHighlightPaint);
                }
            }

        } finally {
            canvas.restore();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            // 优化速度绘制光标以及选择文字背景
            drawCursorAndSelectPath(canvas);
        } catch (Exception e) {
            // 反射调用失败，使用默认的方法绘制背景，会绘制文字
            super.onDraw(canvas);
        }
        // 画文字
        canvas.save();
        canvas.translate(0, getExtendedPaddingTop());
        synchronized (colorLock) {
            drawText(canvas);
        }
        canvas.restore();
        // 绘制分割线
        Paint paint = getPaint();
        paint.setStrokeWidth(lineNumberSplitWidth);
        paint.setColor(lineNumberSplitColor);
        canvas.drawLine(getPaddingLeft() - mTextPadding, 0, getPaddingLeft() - mTextPadding, getHeight(), paint);

        // 根据行号计算左边距padding
        String max = Integer.toString(mMaxLineNumber);
        float lineNumberSize = getPaint().measureText(max) + mNumberPadding + mNumberPadding + mTextPadding;

        if (getPaddingLeft() != lineNumberSize) {
            setPadding((int) lineNumberSize, getPaddingTop(), getPaddingRight(), getPaddingBottom());
            invalidate();
        }
        updateGutter();
    }


    // 获取选择行，如果多余一行，返回-1
    public int getSelectLine() {
        Layout layout = getLayout();
        if (getSelectionStart() != getSelectionEnd()) {
            return -1;
        }
        return layout.getLineForOffset(getSelectionStart());
    }

    // 绘制文本着色
    private void drawText(Canvas canvas) {
        Map<Integer, String> lineNumbles = getLineNumbers();
        Layout layout = getLayout();
        int selectLine = getSelectLine();
        int range[] = new int[4];
        {// 计算需要绘制的行号所需要的范围

            int clipLeft = 0;
            int clipTop = (getScrollView().getScrollY() == 0) ? 0
                    : getExtendedPaddingTop() + getScrollView().getScrollY()
                    - getScrollView().getPaddingTop();
            int clipRight = getWidth();
            int clipBottom = clipTop + getScrollView().getHeight();
            Rect rect = new Rect(clipLeft, clipTop, clipRight, clipBottom);
            getLineRangeForDraw(rect, range);
        }
        int firstLine = range[0];
        int lastLine = range[1];
        if (firstLine < 0) {
            return;
        }
        int previousLineBottom = layout.getLineTop(firstLine);
        int previousLineEnd = layout.getLineStart(firstLine);
        int lineCount = getLineCount();
        Paint paint = getPaint();


        for (int lineNum = firstLine; lineNum <= lastLine
                && lineNum < lineCount; lineNum++) {
            int start = previousLineEnd;

            previousLineEnd = layout.getLineStart(lineNum + 1);

            int end = layout.getLineVisibleEnd(lineNum);
            int ltop = previousLineBottom;
            int lbottom = layout.getLineTop(lineNum + 1);
            previousLineBottom = lbottom;
            int lbaseline = lbottom - layout.getLineDescent(lineNum);
            int left = getPaddingLeft();
            // 绘制选择行背景
            if (lineNum == selectLine) {
                paint.setColor(lineNumberBackgroundColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(mLineNumberBgStrokeWidth);
                canvas.drawRect(getPaddingLeft() - mLineNumberBgStrokeWidth, ltop, getRight() - getPaddingRight() + mLineNumberBgStrokeWidth, lbottom, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            // 绘制行号
            String lineNumberText = lineNumbles.get(lineNum);
            if (lineNumberText != null) {
                paint.setColor(lineNumberColor);
                canvas.drawText(lineNumberText, 0, lineNumberText.length(), mNumberPadding,
                        lbaseline, paint);
            }
            int textLength = getText().length();
            // 绘制文字
            if (start < textLength) {
                //计算需要绘制的文字位置
                //获取改行所有文字宽度
                float[] widths = new float[end - start + 1];
                paint.getTextWidths(getText(), start, end, widths);
                //计算获取看到的文字第一个位置，和对应的偏移x
                float firstNeedDrawPos[] = getLineFirstCharPosForDraw(widths);
                int firstPos = (int) firstNeedDrawPos[0];
                float offsetX = firstNeedDrawPos[1];
                int maxOffX = getViewScrollX() + getVisibleWidth();
                // 文字着色
                for (int i = start + firstPos; i < end && i < textLength; ) {
                    if (offsetX > maxOffX) {
                        break;
                    }
                    int color = getCodeColor(i);
                    {
                        float fontWidths = widths[i - start];
                        int fontCount = 1;
                        for (int j = i + 1; j < end && j < textLength; j++) {
                            if (color == getCodeColor(j)) {
                                fontCount++;
                                fontWidths += widths[j - start];
                            } else {
                                break;
                            }
                        }
                        if (color == 0) {
                            color = defaultTextColor;
                        }

                        paint.setColor(color);
                        canvas.drawText(getText(), i, i + fontCount, left + offsetX, lbaseline, paint);
                        i += fontCount;
                        offsetX += fontWidths;

                    }
                }
            }
        }

    }

    private int getCodeColor(int i) {
        if (codeColors != null && i < codeColors.length) {
            return codeColors[i];
        }
        return 0;
    }

    public int[] getCodeColors() {
        int textLength = getText().length();
        if (codeColors == null) {
            codeColors = new int[textLength];
        }
        //如果文字长度小于颜色长度，生成新的颜色数组
        if (textLength >= codeColors.length) {
            int newColors[] = new int[textLength + 500];
            for (int i = 0; i < codeColors.length; i++) {
                newColors[i] = codeColors[i];
            }
            codeColors = newColors;
        }
        return codeColors;
    }

    public int getViewScrollX() {
        return getScrollView().getScrollX();
    }

    public int getViewScrollY() {
        return getScrollView().getScrollY();
    }

    public View getScrollView() {
        return scrollView == null ? this : scrollView;
    }

    public void setScrollView(View scrollView) {
        this.scrollView = scrollView;
    }

    public int getVisibleWidth() {
        return Math.min(getWidth(), getScrollView().getWidth());
    }

    public int getVisibleHeight() {
        return Math.min(getHeight(), getScrollView().getHeight());
    }


    Rect getCursorRect() {

        Layout layout = getLayout();
        final int offset = getSelectionStart();
        final int line = layout.getLineForOffset(offset);
        final int top = layout.getLineTop(line);
        final int bottom = layout.getLineTop(line + 1);

        float horizontal = layout.getPrimaryHorizontal(offset);
        final int left = clampHorizontalPosition(horizontal);
        return new Rect(left, top, left + DpiUtils.dip2px(getContext(), 1), bottom);
    }

    private int clampHorizontalPosition(float horizontal) {
        horizontal = Math.max(0.5f, horizontal - 0.5f);
        Rect mTempRect = new Rect();

        int drawableWidth = DpiUtils.dip2px(getContext(), 1);

        int scrollX = getScrollX();
        float horizontalDiff = horizontal - scrollX;
        int viewClippedWidth = getWidth() - getCompoundPaddingLeft()
                - getCompoundPaddingRight();

        final int left;
        if (horizontalDiff >= (viewClippedWidth - 1f)) {
            // at the rightmost position
            left = viewClippedWidth + scrollX - (drawableWidth - mTempRect.right);
        } else if (Math.abs(horizontalDiff) <= 1f
                || (TextUtils.isEmpty(getText())
                && (VERY_WIDE - scrollX) <= (viewClippedWidth + 1f)
                && horizontal <= 1f)) {
            // at the leftmost position
            left = scrollX - mTempRect.left;
        } else {
            left = (int) horizontal - mTempRect.left;
        }
        return left;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:

                if (event.getY() > getHeight() - getPaddingBottom()) {
                    int len = getText().length();
                    if (len > 0 && getText().charAt(len - 1) != '\n') {

                    }
                    append("\n");

                }
                break;
        }
        return super.onTouchEvent(event);
    }

    protected void setSuggestData(ArrayList<SuggestionItem> data) {
        SuggestionAdapter mAdapter =
                new SuggestionAdapter(getContext(), R.layout.item_list_suggest, data);
        setAdapter(mAdapter);
    }


    public void setSuggestData(String... suggestionItems) {
        ArrayList<SuggestionItem> suggestionItemList = new ArrayList<>();
        for (String suggestionItem : suggestionItems) {
            suggestionItemList.add(new SuggestionItem(SuggestionType.TYPE_KEYWORD, suggestionItem));
        }
        setSuggestData(suggestionItemList);
    }
}
