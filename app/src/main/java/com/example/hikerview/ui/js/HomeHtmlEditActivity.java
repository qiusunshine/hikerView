package com.example.hikerview.ui.js;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.hikerview.R;
import com.example.hikerview.service.parser.JSEngine;
import com.example.hikerview.ui.base.BaseStatusActivity;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.rules.HighLightEditActivity;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.example.hikerview.utils.DebugUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringFindUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.WebUtil;
import com.hiker.editor.factory.EditorFactory;
import com.lxj.xpopup.XPopup;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.example.hikerview.ui.view.colorDialog.PromptDialog.DIALOG_TYPE_WARNING;

/**
 * 作者：By 15968
 * 日期：On 2019/10/9
 * 时间：At 20:22
 */
public class HomeHtmlEditActivity extends BaseStatusActivity {
    private static final String TAG = "HomeHtmlEditActivity";
    private EditorFactory editorFactory;
    private EditText domEditView;
    private TextView searchInfo;
    private EditText search_edit;
    private boolean notSaved = false;
    private String lang;
    private boolean overLimit = false;
    private StringFindUtil.SearchFindResult findResult = new StringFindUtil.SearchFindResult();

    @Override
    protected void initLayout(Bundle savedInstanceState) {
        setContentView(R.layout.activity_file_edit);
    }

    @Override
    protected void initView() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        domEditView = findView(R.id.js_edit_dom);


        //搜索
        searchInfo = findViewById(R.id.search_count);
        search_edit = findViewById(R.id.search_edit);
        View search_close = findViewById(R.id.search_close);
        View search_forward = findViewById(R.id.search_forward);
        View search_back = findViewById(R.id.search_back);
        search_forward.setOnClickListener(v -> {
            if (CollectionUtil.isNotEmpty(findResult.getIndexList()) && findResult.getSelectPos() < findResult.getIndexList().size() - 1) {
                findResult.setSelectPos(findResult.getSelectPos() + 1);
                updateByFindResult(findResult);
            }
        });
        search_back.setOnClickListener(v -> {
            if (CollectionUtil.isNotEmpty(findResult.getIndexList()) && findResult.getSelectPos() > 0) {
                findResult.setSelectPos(findResult.getSelectPos() - 1);
                updateByFindResult(findResult);
            }
        });
        search_close.setOnClickListener(v -> {
            findView(R.id.search_bg).setVisibility(View.GONE);
            try {
                ((InputMethodManager) Objects.requireNonNull(HomeHtmlEditActivity.this.getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(editorFactory.getTextView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            search_edit.setText("");
            String content = search_edit.getText().toString();
            findAllAsync(content);
        });
        findViewById(R.id.search_ok).setOnClickListener(v -> {
            String content = search_edit.getText().toString();
            try {
                ((InputMethodManager) Objects.requireNonNull(HomeHtmlEditActivity.this.getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(editorFactory.getTextView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            findAllAsync(content);
        });
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        //现在dom已经是fileName（无文件格式后缀）
        String file = getIntent().getStringExtra("file");
        String text = "";
        if (!TextUtils.isEmpty(file)) {
            if ("home".equals(file)) {
                domEditView.setText("主页");
                file = WebUtil.getLocalHomePath(getContext());
            } else {
                domEditView.setText(file);
            }
            if (new File(file).exists()) {
                text = FileUtil.fileToString(file);
            }
            domEditView.setFocusable(false);
        } else {
            ToastMgr.shortBottomCenter(getContext(), "文件路径为空");
            finish();
        }
        lang = "html";
        if (!text.trim().startsWith("<")) {
            lang = "js";
        }
        FrameLayout editor_wrapper = findView(R.id.editor_wrapper);
        int editor = PreferenceMgr.getInt(getContext(), "editor", 0);
        editorFactory = new EditorFactory(this)
                .use(text.length() > 50000 && text.length() < 200000 ? EditorFactory.Editor.PureText : EditorFactory.get(editor), editor_wrapper);
        if (text.length() > 50000) {
            ToastMgr.shortBottomCenter(getContext(), "文本内容过多，已关闭高亮模式");
        } else {
            editorFactory.setLanguage(lang);
        }
        if (text.length() > 200000) {
            overLimit = true;
            ToastMgr.shortBottomCenter(getContext(), "文本内容超出限制，请用第三方应用编辑");
            editorFactory.setText(text.substring(0, 200000) + "\n...文本内容超出限制，请用第三方应用编辑");
            return;
        }
        editorFactory.setText(text);
        editorFactory.loadSuggestions(HighLightEditActivity.getSuggestions());

        editorFactory.getTextView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                notSaved = true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_edit_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.js_edit_save:
                saveNow();
                break;
            case R.id.beautify:
                String code = editorFactory.getText() == null ? "" : editorFactory.getText();
                beautifyJs(code);
                break;
            case R.id.wrap_mode:
                editorFactory.setBreakWord(!editorFactory.isBreakWord());
                break;
            case R.id.undo:
                editorFactory.undo();
                break;
            case R.id.redo:
                editorFactory.redo();
                break;
            case R.id.show_search:
                findView(R.id.search_bg).setVisibility(View.VISIBLE);
                search_edit.requestFocus();
                try {
                    ((InputMethodManager) Objects.requireNonNull(getContext().getSystemService(INPUT_METHOD_SERVICE)))
                            .showSoftInput(search_edit, InputMethodManager.SHOW_FORCED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.editor_js:
                changeEditor(EditorFactory.Editor.JSEditEditor);
                break;
            case R.id.editor_multi:
                changeEditor(EditorFactory.Editor.MultiCodeEditor);
                break;
            case R.id.editor_pure:
                changeEditor(EditorFactory.Editor.PureText);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeEditor(EditorFactory.Editor editor){
        String text = editorFactory.getText();
        editorFactory.change(editor, findView(R.id.editor_wrapper));
        editorFactory.setLanguage(lang);
        editorFactory.setText(text);
        editorFactory.loadSuggestions(HighLightEditActivity.getSuggestions());
        PreferenceMgr.put(getContext(), "editor", EditorFactory.getCode(editor));
    }

    private void beautifyJs(String finalCode) {
        HeavyTaskUtil.executeNewTask(() -> {
            String result = JSEngine.getInstance().evalJS("var window = {}; eval(fetch('hiker://assets/beautify.js')); window.js_beautify(input)", finalCode);
            if (!isFinishing()) {
                if (StringUtil.isEmpty(result)) {
                    runOnUiThread(() -> ToastMgr.shortCenter(getContext(), "格式化失败，返回为空"));
                } else if (result.startsWith("error:")) {
                    runOnUiThread(() -> ToastMgr.shortCenter(getContext(), "格式化失败：" + result));
                } else {
                    runOnUiThread(() -> {
                        editorFactory.setText(result);
                    });
                }
            }
        });
    }

    private void saveNow() {
        if (overLimit) {
            ToastMgr.shortCenter(getContext(), "文本内容超出限制，无法编辑和保存");
            return;
        }
        notSaved = false;
        String dom = domEditView.getText().toString();
        if (TextUtils.isEmpty(dom)) {
            ToastMgr.shortBottomCenter(getContext(), "文件路径不能为空！");
            return;
        }
        if (dom.equals("主页")) {
            dom = WebUtil.getLocalHomePath(getContext());
        }
        String js = editorFactory.getText();
        String notice = "确定保存吗？保存后原文件内容将会被覆盖无法恢复";
        String finalDom = dom;
        new PromptDialog(getContext())
                .setDialogType(DIALOG_TYPE_WARNING)
                .setTitleText("温馨提示")
                .setContentText(notice)
                .setPositiveListener("确定", dialog -> {
                    dialog.dismiss();
                    try {
                        String path = getIntent().getStringExtra("file");
                        if ("home".equals(path)) {
                            WebUtil.saveLocalHomeContent(getContext(), js);
                            Log.d(TAG, "onOptionsItemSelected: ======" + finalDom);
                            PreferenceMgr.put(getContext(), "home", "html");
                            ToastMgr.shortBottomCenter(getContext(), "首页网页文件已保存");
                            WebUtil.goLocalHome(getContext());
                            finish();
                        } else {
                            FileUtil.stringToFile(js, finalDom);
                            ToastMgr.shortBottomCenter(getContext(), "文件已保存");
                            finish();
                        }
                    } catch (IOException e) {
                        DebugUtil.showErrorMsg(HomeHtmlEditActivity.this, getContext(), "写入文件失败", e.getMessage(), "500", e);
                    }
                }).show();
    }


    private void findAllAsync(String find) {
        StringFindUtil.findAllAsync(findResult, editorFactory.getText(), find, findResult1 -> {
            if (isFinishing()) {
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                updateByFindResult(findResult1);
            });
        });
    }


    private void updateByFindResult(StringFindUtil.SearchFindResult findResult) {
        if (editorFactory.getText() == null) {
            return;
        }
        try {
            search_edit.clearFocus();
            editorFactory.requestFocus();
            if (StringUtil.isEmpty(findResult.getFindKey())) {
//                editor.setSelectBackgroundColor(0x33ffffff);
//                editor.setselect(0, 0);
                Selection.setSelection(getEditable(), 0, 0);
                searchInfo.setText("0/0");
                return;
            }
            searchInfo.setText(CollectionUtil.isNotEmpty(findResult.getIndexList()) ? String.format("%d/%d", (findResult.getSelectPos() + 1), findResult.getIndexList().size()) : "0/0");
            if (CollectionUtil.isEmpty(findResult.getIndexList())) {
//                editor.setSelectBackgroundColor(0x33ffffff);
                Selection.setSelection(getEditable(), 0, 0);
//                editor.setSelection(0, 0);
                return;
            }
//            editor.setSelectBackgroundColor(getResources().getColor(R.color.greenAction));
            int start = findResult.getIndexList().get(findResult.getSelectPos());
            if (start < 0) {
                return;
            }
            Selection.setSelection(getEditable(), start, start + findResult.getFindKey().length());
//            editor.setSelection(start, start + findResult.getFindKey().length());
            Layout layout = editorFactory.getTextView().getLayout();
            Rect rect = new Rect();
            int line = layout.getLineForOffset(start);
            layout.getLineBounds(line > 0 ? line - 1 : line, rect);
            editorFactory.getTextView().scrollTo(rect.left > 50 ? rect.left - 50 : 0, rect.bottom);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }


    public Editable getEditable() {
        return editorFactory.getEditable();
    }

    @Override
    public void finish() {
        if (notSaved && !overLimit) {
            new XPopup.Builder(getContext())
                    .asConfirm("温馨提示", "有未保存的内容，建议先保存再退出页面，是否直接退出该页面？",
                            "保存后退出", "直接退出", () -> {
                                notSaved = false;
                                finish();
                            }, this::saveNow, false).show();
            return;
        }
        super.finish();
    }
}
