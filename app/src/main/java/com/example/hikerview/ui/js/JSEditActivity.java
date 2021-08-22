package com.example.hikerview.ui.js;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.R;
import com.example.hikerview.service.parser.JSEngine;
import com.example.hikerview.ui.base.BaseStatusActivity;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.js.model.ViaJsPlugin;
import com.example.hikerview.ui.rules.HighLightEditActivity;
import com.example.hikerview.ui.view.colorDialog.PromptDialog;
import com.example.hikerview.utils.HeavyTaskUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringFindUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ToastMgr;
import com.hiker.editor.factory.EditorFactory;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import timber.log.Timber;

import static com.example.hikerview.ui.view.colorDialog.PromptDialog.DIALOG_TYPE_WARNING;

/**
 * 作者：By 15968
 * 日期：On 2019/10/9
 * 时间：At 20:22
 */
public class JSEditActivity extends BaseStatusActivity {
    private static final String TAG = "JSEditActivity";
    private EditorFactory editorFactory;
    private EditText domEditView;
    private String defaultJs = "";
    private TextView searchInfo;
    private EditText search_edit, js_edit_name;
    private StringFindUtil.SearchFindResult findResult = new StringFindUtil.SearchFindResult();
    private boolean overLimit = false;

    @Override
    protected void initLayout(Bundle savedInstanceState) {
        setContentView(R.layout.activity_js_edit);
    }

    @Override
    protected void initView() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        domEditView = findView(R.id.js_edit_dom);
        js_edit_name = findView(R.id.js_edit_name);

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
                ((InputMethodManager) Objects.requireNonNull(getSystemService(INPUT_METHOD_SERVICE)))
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
                ((InputMethodManager) Objects.requireNonNull(getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(editorFactory.getTextView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            findAllAsync(content);
        });

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
        if (editorFactory.getEditable() == null) {
            return;
        }
        try {
            search_edit.clearFocus();
            editorFactory.requestFocus();
            if (StringUtil.isEmpty(findResult.getFindKey())) {
                editorFactory.setSelectBackgroundColor(0x33ffffff);
//                codePane.getCodeText().setselect(0, 0);
                Selection.setSelection(editorFactory.getEditable(), 0, 0);
                searchInfo.setText("0/0");
                return;
            }
            searchInfo.setText(CollectionUtil.isNotEmpty(findResult.getIndexList()) ? String.format("%d/%d", (findResult.getSelectPos() + 1), findResult.getIndexList().size()) : "0/0");
            if (CollectionUtil.isEmpty(findResult.getIndexList())) {
                editorFactory.setSelectBackgroundColor(0x33ffffff);
                Selection.setSelection(editorFactory.getEditable(), 0, 0);
//                codePane.getCodeText().setSelection(0, 0);
                return;
            }
            editorFactory.setSelectBackgroundColor(getResources().getColor(R.color.greenAction));
            int start = findResult.getIndexList().get(findResult.getSelectPos());
            if (start < 0) {
                return;
            }
            Selection.setSelection(editorFactory.getEditable(), start, start + findResult.getFindKey().length());
//            codePane.getCodeText().setSelection(start, start + findResult.getFindKey().length());
            Layout layout = editorFactory.getTextView().getLayout();
            Rect rect = new Rect();
            int line = layout.getLineForOffset(start);
            layout.getLineBounds(line > 0 ? line - 1 : line, rect);
            editorFactory.getTextView().scrollTo(rect.left > 50 ? rect.left - 50 : 0, rect.bottom);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        //现在dom已经是fileName（无文件格式后缀）
        String dom = getIntent().getStringExtra("dom");
        String jsPrefix = "";
        String viaJsId = "";
        if (getIntent().getBooleanExtra("via", false)) {
            String viaJs = getIntent().getStringExtra("viaJs");
            if (!TextUtils.isEmpty(viaJs)) {
                Log.d(TAG, "initData: viaJs ： " + viaJs);
                ViaJsPlugin viaJsPlugin = JSON.parseObject(viaJs, ViaJsPlugin.class);
                if (!TextUtils.isEmpty(viaJsPlugin.getCode())) {
                    String code = new String(Base64.decode(viaJsPlugin.getCode(), Base64.NO_WRAP));
                    viaJsId = "//==========via-plugin:" + viaJsPlugin.getId() + "==========\n";
                    jsPrefix = viaJsId +
                            "//==========" + viaJsPlugin.getName() + "==========\n" +
                            code +
                            "\n" + viaJsId;
                    dom = viaJsPlugin.getUrl();
                    Log.d(TAG, "initData: viaJsPlugin.getUrl:" + dom);
                    if ("*".equals(dom)) {
                        dom = "global";
                    }
                    String[] content = jsPrefix.split("@name: ");
                    if (content.length > 1) {
                        String title = content[1].split("\n")[0];
                        if (StringUtil.isNotEmpty(title)) {
                            dom = dom + "_" + title;
                        }
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(dom)) {
            dom = StringUtil.getDom(dom);
            String[] names = dom.split("_");
            String title = names.length <= 1 ? names[0] : StringUtil.arrayToString(names, 1, "_");
            domEditView.setText(names[0]);
            if (names.length > 1) {
                js_edit_name.setText(title);
            }
            if (JSManager.instance(getContext()).hasJs(dom)) {
                String nowJs = JSManager.instance(getContext()).getJsByFileName(dom);
                if (nowJs == null) {
                    nowJs = "";
                }
                if (TextUtils.isEmpty(viaJsId)) {
                    setDefaultText(nowJs);
                } else {
                    if (!nowJs.contains(viaJsId)) {
                        ToastMgr.shortBottomCenter(getContext(), "检测到via插件！");
                        setDefaultText(jsPrefix + nowJs);
                    } else {
//                        Log.d(TAG, "initData: + nowJs : " + nowJs + ",viaJsId》" + viaJsId);
                        String[] strings = StringUtils.splitByWholeSeparatorPreserveAllTokens(nowJs, viaJsId);
                        Log.d(TAG, "initData: strings ; " + strings.length);
                        if (strings.length == 3) {
                            ToastMgr.shortBottomCenter(getContext(), "检测到via插件更新！");
                            setDefaultText(strings[0] + jsPrefix + strings[2]);
                        } else {
                            ToastMgr.shortBottomCenter(getContext(), "via插件识别失败！");
                            setDefaultText(nowJs);
                        }
                    }
                }
            } else {
                setDefaultText("".equals(jsPrefix) ? defaultJs : jsPrefix);
            }
        } else {
            setDefaultText("".equals(jsPrefix) ? defaultJs : jsPrefix);
        }
    }

    private void setDefaultText(String text) {
        if (text == null) {
            text = "";
        }
        Timber.d("setDefaultText length: %s", text.length());
        FrameLayout editor_wrapper = findView(R.id.editor_wrapper);
        int editor = PreferenceMgr.getInt(getContext(), "editor", 0);
        editorFactory = new EditorFactory(this)
                .use(text.length() > 50000 && text.length() < 200000 ? EditorFactory.Editor.PureText : EditorFactory.get(editor), editor_wrapper);
        if (text.length() > 200000) {
            overLimit = true;
            ToastMgr.shortBottomCenter(getContext(), "文本内容超出限制，请用第三方应用编辑");
            editorFactory.setText(text.substring(0, 200000) + "\n...文本内容超出限制，请用第三方应用编辑");
            return;
        }
        if (text.length() > 50000) {
            ToastMgr.shortBottomCenter(getContext(), "文本内容过多，已关闭高亮模式");
        }
        editorFactory.setText(text);
        editorFactory.loadSuggestions(HighLightEditActivity.getSuggestions());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.js_edit_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.beautify:
                String code = editorFactory.getText() == null ? "" : editorFactory.getText();
                beautifyJs(code);
                break;
            case R.id.undo:
                editorFactory.undo();
                break;
            case R.id.redo:
                editorFactory.redo();
                break;
            case R.id.js_edit_save:
                if (overLimit) {
                    ToastMgr.shortCenter(getContext(), "文本内容超出限制，无法编辑和保存");
                    break;
                }
                String dom = StringUtil.getDom(domEditView.getText().toString());
                if (StringUtil.isEmpty(dom)) {
                    dom = "global";
                    domEditView.setText(dom);
                } else if (dom.contains("*")) {
                    dom = dom.replace("*", "global");
                    domEditView.setText(dom);
                }
                String desc = js_edit_name.getText().toString();
                if (StringUtil.isNotEmpty(desc)) {
                    dom = dom + "_" + desc;
                }
                String js = editorFactory.getText();
                if (TextUtils.isEmpty(js)) {
                    deleteJs(dom);
                } else {
                    String notice = JSManager.instance(getContext()).hasJs(dom) ? "确定更新该域名（该JS文件）下的插件？更新后将无法找回旧版！" : "确定保存该插件？保存后将立即生效，不要保存无用插件！";
                    String finalDom = dom;
                    new PromptDialog(getContext())
                            .setDialogType(DIALOG_TYPE_WARNING)
                            .setTitleText("温馨提示")
                            .setContentText(notice)
                            .setPositiveListener("确定", dialog -> {
                                dialog.dismiss();
                                boolean ok = JSManager.instance(getContext()).updateJs(finalDom, js);
                                if (ok) {
                                    ToastMgr.shortBottomCenter(getContext(), "保存成功！");
                                } else {
                                    ToastMgr.shortBottomCenter(getContext(), "保存失败！");
                                }
                            }).show();
                }
                break;
//            case R.id.js_edit_delete:
//                String dom2 = StringUtil.getDom(domEditView.getTextNoDelay().toString());
//                if (TextUtils.isEmpty(dom2)) {
//                    ToastMgr.shortBottomCenter(getContext(), "网站域名不能为空！");
//                    break;
//                }
//                deleteJs(dom2);
//                break;
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

    private void changeEditor(EditorFactory.Editor editor) {
        String text = editorFactory.getText();
        editorFactory.change(editor, findView(R.id.editor_wrapper));
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

    private void deleteJs(String dom) {
        new PromptDialog(getContext())
                .setDialogType(DIALOG_TYPE_WARNING)
                .setTitleText("温馨提示")
                .setContentText("您是否想要删除" + dom + "下的JS插件？")
                .setPositiveListener("确定删除", dialog -> {
                    dialog.dismiss();
                    boolean ok = JSManager.instance(getContext()).deleteJs(dom);
                    if (ok) {
                        ToastMgr.shortBottomCenter(getContext(), "删除成功！");
                        editorFactory.setText(defaultJs);
                    } else {
                        ToastMgr.shortBottomCenter(getContext(), "删除失败！");
                    }
                }).show();
    }
}
