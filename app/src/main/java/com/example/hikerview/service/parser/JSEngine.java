package com.example.hikerview.service.parser;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.constants.TimeConstants;
import com.example.hikerview.constants.UAEnum;
import com.example.hikerview.event.OnBackEvent;
import com.example.hikerview.event.home.OnRefreshPageEvent;
import com.example.hikerview.event.home.OnRefreshWebViewEvent;
import com.example.hikerview.event.home.OnRefreshX5HeightEvent;
import com.example.hikerview.event.home.ToastEvent;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.service.http.CharsetStringConvert;
import com.example.hikerview.service.http.HikerRuleUtil;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.SearchEngine;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.home.model.ArticleList;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.SearchResult;
import com.example.hikerview.ui.rules.model.AccountPwd;
import com.example.hikerview.ui.rules.model.SubscribeRecord;
import com.example.hikerview.ui.rules.service.HomeRulesSubService;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.utils.AesUtil;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.UriUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.adapter.Call;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.request.PostRequest;
import com.lzy.okgo.request.PutRequest;

import org.adblockplus.libadblockplus.android.Utils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.litepal.LitePal;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;
import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2018/12/10
 * 时间：At 12:04
 */
public class JSEngine {
    private static final String TAG = "JSEngine";
    private Class clazz;
    private String allFunctions;
    private String evalFunctions;
    private volatile static JSEngine engine;
    private Map<String, String> varMap = new HashMap<>();
    private OkHttpClient noRedirectHttpClient;
    private Map<String, OnFindCallBack<?>> callbackMap = new ConcurrentHashMap<>();
    private Map<String, String> resCodeMap = new ConcurrentHashMap<>();
    private String jsPlugin;

    private JSEngine() {
        this.clazz = JSEngine.class;
        //生成js语法
        allFunctions = String.format(getAllFunctions(), clazz.getName()) +
                "\n var MY_UA = JSON.parse(getUaObject());" +
                "\n var MOBILE_UA =  MY_UA.mobileUa;" +
                "\n var PC_UA = MY_UA.pcUa" +
                "\n eval(getJsPlugin())";
        evalFunctions = String.format(getEvalFunctions(), clazz.getName());//生成js语法
    }

    public static JSEngine getInstance() {
        if (engine == null) {
            synchronized (JSEngine.class) {
                if (engine == null) {
                    engine = new JSEngine();
                }
            }
        }
        return engine;
    }

    public void parseSearchRes(String url, String res, SearchEngine searchEngine, OnFindCallBack<List<SearchResult>> searchJsCallBack) {
        parseSearchRes(url, res, searchEngine.toMovieRule(), searchJsCallBack);
    }

    private void parseSearchRes(String url, String res, MovieRule movieRule, OnFindCallBack<List<SearchResult>> searchJsCallBack) {
        String callbackKey = UUIDUtil.genUUID();
        callbackMap.put(callbackKey, searchJsCallBack);
        resCodeMap.put(callbackKey, res);
        if (!movieRule.getSearchFind().startsWith("js:")) {
            searchJsCallBack.showErr(movieRule.getTitle() + "---搜索结果解析失败！请检查规则");
        } else {
            try {
                runScript(getMyCallbackKey(callbackKey) + getMyRule(movieRule) + getMyUrl(url) + getMyType("search") + getMyJs(movieRule.getSearchFind()), callbackKey);
            } catch (Exception e) {
                Timber.e(e, "parseSearchRes: ");
                setError("运行出错：" + e.toString(), callbackKey);
            }
        }
    }

    public void parseStr(String input, String js, MovieRule movieRule, OnFindCallBack<String> callBack) {
        String callbackKey = UUIDUtil.genUUID();
        callbackMap.put(callbackKey, callBack);
        resCodeMap.put(callbackKey, input);
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyType("string") + getMyRule(movieRule) + getMyJs(js), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parseStr: ");
        }
    }

    public void parseHome(String url, String input, ArticleListRule articleListRule, String js, OnFindCallBack<List<ArticleList>> callBack) {
        String callbackKey = UUIDUtil.genUUID();
        callbackMap.put(callbackKey, callBack);
        resCodeMap.put(callbackKey, input);
        try {
            runScript("\n" + getMyRule(articleListRule) + getMyType("home") + getMyCallbackKey(callbackKey) + getMyUrl(url) + getMyJs(js), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parseHome: ");
        }
    }

    public void parsePreRule(ArticleListRule articleListRule) {
        String callbackKey = UUIDUtil.genUUID();
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyType("preHome") + getMyRule(articleListRule) + articleListRule.getPreRule(), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parsePreRule: ");
        }
    }


    public void parsePreRule(SearchEngine articleListRule) {
        String callbackKey = UUIDUtil.genUUID();
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyType("preEngine") + getMyRule(articleListRule) + articleListRule.getPreRule(), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parsePreRule: ");
        }
    }

    public String parsePublishRule(Object rule, String publishCode, AccountPwd accountPwd) {
        String js = getMyRule(rule) + generateMY("MY_ACCOUNT", accountPwd.getAccount()) +
                generateMY("MY_PASSWORD", accountPwd.getPassword()) + publishCode;
        return evalJS(js, "", false);
    }

    public String evalJS(String jsStr, String input) {
        return evalJS(jsStr, input, true);
    }

    public String evalJS(String jsStr, String input, boolean decodeConflict) {
        //运行js = allFunctions + js
        String js = evalFunctions + "\n" + getMyInput(input) + getMyType("eval") + getMyCallbackKey(UUIDUtil.genUUID());
        if (decodeConflict) {
            js = js + StringUtil.decodeConflictStr(jsStr);
        } else {
            js = js + jsStr;
        }
        js = allFunctions + "\n" + getReplaceJS(js);
        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        rhino.setOptimizationLevel(-1);
        rhino.setLanguageVersion(200);
        try {
            Scriptable scope = rhino.initStandardObjects();
            ScriptableObject.putProperty(scope, "javaContext", org.mozilla.javascript.Context.javaToJS(this, scope));//配置属性 javaContext:当前类JSEngine的上下文
            ScriptableObject.putProperty(scope, "javaLoader", org.mozilla.javascript.Context.javaToJS(clazz.getClassLoader(), scope));//配置属性 javaLoader:当前类的JSEngine的类加载器
            Object re = rhino.evaluateString(scope, js, clazz.getSimpleName(), 1, null);
            if (re instanceof String) {
                return (String) re;
            } else if (re instanceof Undefined) {
                return "undefined";
            } else {
                return re + "";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }


    /**
     * 执行JS
     *
     * @param js js执行代码 eg: "var v1 = getValue('Ta');setValue(‘key’，v1);"
     */
    private void runScript(String js, String callbackKey) {
        String runJSStr = allFunctions + "\n" + getReplaceJS(js);//运行js = allFunctions + js
        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        rhino.setLanguageVersion(200);
        rhino.setOptimizationLevel(-1);
        try {
            Scriptable scope = rhino.initStandardObjects();
            ScriptableObject.putProperty(scope, "javaContext", org.mozilla.javascript.Context.javaToJS(this, scope));//配置属性 javaContext:当前类JSEngine的上下文
            ScriptableObject.putProperty(scope, "javaLoader", org.mozilla.javascript.Context.javaToJS(clazz.getClassLoader(), scope));//配置属性 javaLoader:当前类的JSEngine的类加载器
            rhino.evaluateString(scope, runJSStr, clazz.getSimpleName(), 1, null);
        } catch (Exception e) {
            setError("JS编译出错：" + e.getMessage(), callbackKey);
        } finally {
            org.mozilla.javascript.Context.exit();
            try {
                if (!StringUtil.isEmpty(callbackKey)) {
                    resCodeMap.remove(callbackKey);
                    callbackMap.remove(callbackKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getMyJs(String js) {
        return StringUtils.replaceOnce(js, "js:", "");
    }

    private String getMyInput(String input) {
        return "var input = '" + Utils.escapeJavaScriptString(input) + "';\n";
    }

    private String getMyCallbackKey(String callbackKey) {
        return "var CALLBACK_KEY = '" + callbackKey + "';\n";
    }

    private String getMyUrl(String url) {
        return "var MY_URL = '" + Utils.escapeJavaScriptString(url) + "';\n";
    }

    private String getMyType(String type) {
        return "var MY_TYPE = '" + type + "';\n";
    }

    private String getMyRule(Object rule) {
        return "var my_rule = '" + Utils.escapeJavaScriptString(JSON.toJSONString(rule, JSONPreFilter.getSimpleFilter())) + "';\n var MY_RULE = JSON.parse(my_rule);\n";
    }

    public String generateMY(String var, String value) {
        if (value == null) {
            return "var " + var + " = null;\n";
        }
        return "var " + var + " = '" + Utils.escapeJavaScriptString(value) + "';\n";
    }

    /**
     * 将源码直接暴露的js加载进去
     *
     * @param s 源码
     * @return
     */
    private String getDomScripts(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        if (s.startsWith("[") || s.startsWith("{")) {
            return "";
        }
        Document doc = Jsoup.parse(s);
        if (doc == null) {
            return "";
        }
        Elements elements = doc.getElementsByTag("script");
        if (elements == null || elements.size() < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Element element : elements) {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(wrapTryScript(element.html()));
        }
        return sb.toString();
    }

    /**
     * 避免出错
     *
     * @param script js
     * @return
     */
    private String wrapTryScript(String script) {
        return "try {" + script + "}catch(err){}";
    }


    @JSAnnotation(returnType = 1)
    public String getResCode(@Parameter("callbackKey") Object callbackKey) {
        return resCodeMap.get((String) argsNativeObjectAdjust(callbackKey));
    }

    @JSAnnotation(returnType = 1)
    public String getUrl(@Parameter("urlKey") Object urlKey) {
        return (String) argsNativeObjectAdjust(urlKey);
    }

    @JSAnnotation(returnType = 1)
    public String getJsPlugin() {
        if (jsPlugin == null) {
            jsPlugin = FilesInAppUtil.getAssetsString(Application.getContext(), "Hikerurl.js");
        }
        return jsPlugin;
    }

    @JSAnnotation(returnType = 1)
    public String getVar(@Parameter("o") Object o, @Parameter("defaultVal") Object defaultVal) {
        Object res = argsNativeObjectAdjust(o);
        Object val = "";
        if (defaultVal != null) {
            val = argsNativeObjectAdjust(defaultVal);
        }
        if (val == null || isUndefined(val)) {
            val = "";
        }
        if (!(res instanceof String)) {
            return (String) val;
        }
        String name = (String) res;
        if (varMap.containsKey(name)) {
            Timber.d("getVar: " + name + "===" + varMap.get(name));
            return varMap.get(name);
        }
        return (String) val;
    }

    private boolean isUndefined(Object input) {
        if (input instanceof String && "undefined".equals(input)) {
            return true;
        }
        return Undefined.isUndefined(input);
    }

    public void putVar(Object o) {
        putVar(o, null);
    }

    @JSAnnotation
    public void putVar(@Parameter("o") Object o, @Parameter("o2") Object o2) {
        Object res = argsNativeObjectAdjust(o);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (oo2 != null && !isUndefined(oo2) && res instanceof String && oo2 instanceof String) {
            putVar2(res, oo2);
            return;
        }
        if (!(res instanceof JSONObject)) {
            return;
        }
        JSONObject map = (JSONObject) res;
        varMap.put(map.getString("key"), map.getString("value"));
    }

    @JSAnnotation
    public void putVar2(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return;
        }
        String str = (String) oo1;
        if (StringUtil.isEmpty(str)) {
            return;
        }
        String code = (String) oo2;
        varMap.put(str, code);
    }

    /**
     * 编解码
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String decodeStr(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String str = (String) oo1;
        String code = (String) oo2;
        String res = HttpParser.decodeUrl(str, code);
        Log.d(TAG, "decodeUrl: " + str);
        Log.d(TAG, "decodeUrl: " + code);
        Log.d(TAG, "decodeUrl: " + res);
        return res;
    }

    /**
     * 编解码
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String encodeStr(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String str = (String) oo1;
        String code = (String) oo2;
        String res = HttpParser.encodeUrl(str, code);
        Log.d(TAG, "encodeStr: " + str);
        Log.d(TAG, "encodeStr: " + code);
        Log.d(TAG, "encodeStr: " + res);
        return res;
    }

    /**
     * 编解码
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String base64Decode(@Parameter("o1") Object o1) {
        Object oo1 = argsNativeObjectAdjust(o1);
        if (!(oo1 instanceof String)) {
            return "";
        }
        return new String(Base64.decode((String) oo1, Base64.NO_WRAP));
    }

    /**
     * 编解码
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String base64Encode(@Parameter("o1") Object o1) {
        Object oo1 = argsNativeObjectAdjust(o1);
        if (!(oo1 instanceof String)) {
            return "";
        }
        return new String(Base64.encode(((String) oo1).getBytes(), Base64.NO_WRAP));
    }

    /**
     * 编解码
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String aesDecode(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String key = (String) oo1;
        String code = (String) oo2;
        try {
            return AesUtil.decrypt(key, code);
        } catch (Exception e) {
            return "";
        }
    }

    @JSAnnotation(returnType = 1)
    public String getCryptoJS() {
        try {
            return FilesInAppUtil.getAssetsString(Application.getContext(), "aes.js");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 编解码
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String aesEncode(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String key = (String) oo1;
        String code = (String) oo2;
        try {
            return AesUtil.encrypt(key, code);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 供js获取相关信息
     *
     * @return 规则
     */
    @JSAnnotation(returnType = 1)
    public String getRule(@Parameter("ruleStrKey") Object ruleStrKey) {
        return (String) argsNativeObjectAdjust(ruleStrKey);
    }

    /**
     * 刷新webview
     */
    @JSAnnotation
    public void refreshX5WebView(@Parameter("url") Object url) {
        EventBus.getDefault().post(new OnRefreshWebViewEvent((String) argsNativeObjectAdjust(url)));
    }

    /**
     * 刷新webview
     */
    @JSAnnotation
    public void refreshX5Desc(@Parameter("desc") Object desc) {
        EventBus.getDefault().post(new OnRefreshX5HeightEvent((String) argsNativeObjectAdjust(desc)));
    }

    /**
     * 刷新页面
     *
     * @param scrollTop 是否回到顶部
     */
    @JSAnnotation
    public void refreshPage(@Parameter("scrollTop") Object scrollTop) {
        Object top = argsNativeObjectAdjust(scrollTop);
        boolean toTop = top != null && !isUndefined(top) && top instanceof Boolean ? (Boolean) top : true;
        EventBus.getDefault().post(new OnRefreshPageEvent(toTop));
    }

    /**
     * 返回上一页
     *
     * @param refreshPage 是否返回后刷新
     */
    @JSAnnotation
    public void back(@Parameter("refreshPage") Object refreshPage) {
        Object refresh = argsNativeObjectAdjust(refreshPage);
        boolean toRefresh = refresh != null && !isUndefined(refresh) && refresh instanceof Boolean ? (Boolean) refresh : true;
        EventBus.getDefault().post(new OnBackEvent(toRefresh, false));
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setStrResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey, @Parameter("ruleKey") Object ruleKey) {
        Object res = argsNativeObjectAdjust(o);
        Object rule = argsNativeObjectAdjust(ruleKey);
        String movieTitle = ((JSONObject) rule).getString("Title");
        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }
        callbackMap.remove(callbackStr);
        if (!(res instanceof String)) {
            onFindCallBack.showErr(movieTitle + "---视频解析失败！请检查规则");
            return;
        }
        try {
            onFindCallBack.onSuccess((String) res);
        } catch (Exception e) {
            e.printStackTrace();
            onFindCallBack.showErr(movieTitle + "---视频解析失败！请检查规则");
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey,
                          @Parameter("ruleKey") Object ruleKey, @Parameter("typeKey") Object typeKey) {
        if ("search".equals(argsNativeObjectAdjust(typeKey))) {
            callbackSearchResult(o, callbackKey, ruleKey, true);
        } else {
            callbackHomeResult(o, callbackKey, ruleKey, true);
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setHomeResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey
            , @Parameter("ruleKey") Object ruleKey, @Parameter("typeKey") Object typeKey) {
        if ("search".equals(argsNativeObjectAdjust(typeKey))) {
            callbackSearchResult(o, callbackKey, ruleKey, true);
        } else {
            callbackHomeResult(o, callbackKey, ruleKey, true);
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setSearchResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey,
                                @Parameter("ruleKey") Object ruleKey, @Parameter("typeKey") Object typeKey) {
        if ("home".equals(argsNativeObjectAdjust(typeKey))) {
            callbackHomeResult(o, callbackKey, ruleKey, true);
        } else {
            callbackSearchResult(o, callbackKey, ruleKey, true);
        }
    }


    private void callbackHomeResult(Object o, Object callbackKey, Object ruleKey, boolean reCallback) {
        Object res = argsNativeObjectAdjust(o);
        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }

        if (!(res instanceof JSONObject)) {
            if (res instanceof JSONArray) {
                JSONObject object = new JSONObject();
                object.put("data", res);
                callbackHomeResult(object, callbackKey, ruleKey, reCallback);
                return;
            }
            onFindCallBack.showErr("---分类结果解析失败！请检查规则：setHomeResult is not JSONObject");
            callbackMap.remove(callbackStr);
            return;
        }
        try {
            JSONArray array = ((JSONObject) res).getJSONArray("data");
            List<ArticleList> results = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                try {
                    ArticleList searchResult = new ArticleList();
                    searchResult.setTitle(array.getJSONObject(i).getString("title"));
                    if (array.getJSONObject(i).containsKey("img")) {
                        searchResult.setPic(array.getJSONObject(i).getString("img"));
                    } else {
                        searchResult.setPic(array.getJSONObject(i).getString("pic_url"));
                    }
                    searchResult.setDesc(array.getJSONObject(i).getString("desc"));
                    try {
                        searchResult.setUrl(array.getJSONObject(i).getString("url"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (!TextUtils.isEmpty(array.getJSONObject(i).getString("col_type"))) {
                        searchResult.setType(array.getJSONObject(i).getString("col_type"));
                    }
                    results.add(searchResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            onFindCallBack.onSuccess(results);
        } catch (Exception e) {
            e.printStackTrace();
            //reCallback：是否允许再次回调，避免两个callback之间循环调用
            if (e.getClass() == ClassCastException.class && reCallback) {
                callbackSearchResult(o, callbackKey, ruleKey, false);
            } else {
                onFindCallBack.showErr("---分类结果解析失败！请检查规则：" + e.getMessage());
            }
        }
        callbackMap.remove(callbackStr);
    }

    private void callbackSearchResult(Object o, Object callbackKey, Object ruleKey, boolean reCallback) {
        Object res = argsNativeObjectAdjust(o);
        Object rule = argsNativeObjectAdjust(ruleKey);
        String movieTitle = ((JSONObject) rule).getString("Title");

        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }

        if (!(res instanceof JSONObject)) {
            if (res instanceof JSONArray) {
                JSONObject object = new JSONObject();
                object.put("data", res);
                callbackSearchResult(object, callbackKey, ruleKey, reCallback);
                return;
            }
            if (onFindCallBack != null) {
                onFindCallBack.showErr(movieTitle + "---搜索结果解析失败！请检查规则");
            }
            callbackMap.remove(callbackStr);
            return;
        }
        try {
            JSONArray array = ((JSONObject) res).getJSONArray("data");
            List<SearchResult> results = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                try {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setTitle(array.getJSONObject(i).getString("title"));
                    searchResult.setUrl(array.getJSONObject(i).getString("url"));
                    searchResult.setDesc(movieTitle);
                    if (array.getJSONObject(i).containsKey("desc")) {
                        searchResult.setDescMore(array.getJSONObject(i).getString("desc"));
                    }
                    if (array.getJSONObject(i).containsKey("content")) {
                        searchResult.setContent(array.getJSONObject(i).getString("content"));
                    }
                    if (array.getJSONObject(i).containsKey("img")) {
                        searchResult.setImg(array.getJSONObject(i).getString("img"));
                    } else if (array.getJSONObject(i).containsKey("pic_url")) {
                        searchResult.setImg(array.getJSONObject(i).getString("pic_url"));
                    }
                    searchResult.setType("video");
                    results.add(searchResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            onFindCallBack.onSuccess(results);
        } catch (Exception e) {
            e.printStackTrace();
            //reCallback：是否允许再次回调，避免两个callback之间循环调用
            if (e.getClass() == ClassCastException.class && reCallback) {
                callbackHomeResult(o, callbackKey, ruleKey, false);
            } else {
                onFindCallBack.showErr(movieTitle + "---搜索结果解析失败！请检查规则");
            }
        }
        callbackMap.remove(callbackStr);
    }


    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setError(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey) {
        Object res = argsNativeObjectAdjust(o);
        Timber.d("setError: %s", JSON.toJSONString(res));

        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            EventBus.getDefault().post(new ToastEvent("---解析失败！请检查规则：" + JSON.toJSONString(res)));
            return;
        }
        callbackMap.remove(callbackStr);

        onFindCallBack.showErr("---解析失败！请检查规则：" + res);
    }

    private Object fetchByHiker(String url) {
        if (url.startsWith("hiker://files/")) {
            String fileName = url.replace("hiker://files/", "");
            File file = new File(SettingConfig.rootDir + File.separator + fileName);
            if (file.exists()) {
                return FileUtil.fileToString(file.getAbsolutePath());
            } else {
                return "";
            }
        } else if (url.startsWith("file://")) {
            url = url.replace("file://", "");
            File file = new File(url);
            if (file.exists()) {
                return FileUtil.fileToString(file.getAbsolutePath());
            } else {
                return "";
            }
        }
        if (url.startsWith("hiker://")) {
            if (url.startsWith("hiker://assets/")) {
                try {
                    return HikerRuleUtil.getAssetsFileByHiker(url);
                } catch (Exception e) {
                    return "";
                }
            }
            try {
                return HikerRuleUtil.getRulesByHiker(url);
            } catch (Exception e) {
                return "";
            }
        }
        return false;
    }

    @JSAnnotation(returnType = 2)
    public String batchFetch(@Parameter("params") Object params, @Parameter("threadNum") Object threadNum) {
        Object res = argsNativeObjectAdjust(params);
        if (!(res instanceof JSONArray)) {
            return "[]";
        }
        JSONArray jsonArray = (JSONArray) res;
        return JSON.toJSONString(batchRequest(jsonArray));
    }


    private String batchFetch2(@Parameter("params") Object params, @Parameter("threadNum") Object threadNum) {
        Object res = argsNativeObjectAdjust(params);
        if (!(res instanceof JSONArray)) {
            return "[]";
        }
        JSONArray jsonArray = (JSONArray) res;
        int size = jsonArray.size();
        int maxCount = 16;
        if (jsonArray.size() > maxCount) {
            List<String> data = new ArrayList<>();
            int batch = size / maxCount + 1;
            for (int i = 0; i < batch; i++) {
                int start = i * maxCount;
                int end = (i + 1) * maxCount;
                if (end > size) {
                    end = size;
                }
                JSONArray jsonArray1 = new JSONArray(new ArrayList<>(jsonArray.subList(start, end)));
                data.addAll(batchRequest(jsonArray1));
            }
            return JSON.toJSONString(data);
        } else {
            return JSON.toJSONString(batchRequest(jsonArray));
        }
    }

    private List<String> batchRequest(JSONArray jsonArray) {
        Map<Integer, String> indexMap = new ConcurrentHashMap<>();
        int maxThread = Math.min(jsonArray.size(), 16);
        ExecutorService jsExecutorService = new ThreadPoolExecutor(maxThread, maxThread,
                1L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(4096));
        CountDownLatch countDownLatch = new CountDownLatch(jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) (jsonArray.get(i));
            String url = jsonObject.getString("url");
            Object options = jsonObject.get("options");
            if (StringUtil.isEmpty(url)) {
                countDownLatch.countDown();
                continue;
            }
            int finalI = i;
            jsExecutorService.execute(() -> {
                long start = System.currentTimeMillis();
                try {
                    String s = fetch(url, options);
                    indexMap.put(finalI, s);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                    Timber.d("js task end, used " + (System.currentTimeMillis() - start) + "毫秒");
                }
            });
        }
        try {
            countDownLatch.await(jsonArray.size() / 16 * 10 + 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            if (!jsExecutorService.isShutdown() && !jsExecutorService.isTerminated()) {
                jsExecutorService.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] data = new String[jsonArray.size()];
        for (Map.Entry<Integer, String> entry : indexMap.entrySet()) {
            data[entry.getKey()] = entry.getValue();
        }
        return new ArrayList<>(Arrays.asList(data));
    }

    @JSAnnotation(returnType = 1)
    public String request(@Parameter("url") String url, @Parameter("options") Object options) {
        if (options != null && !isUndefined(options)) {
            Map op = (Map) argsNativeObjectAdjust(options);
            Map<String, String> headerMap = (Map<String, String>) op.get("headers");
            if (headerMap == null) {
                headerMap = (Map<String, String>) op.get("header");
            }
            if (headerMap == null) {
                headerMap = new HashMap<>();
                op.put("headers", headerMap);
            }
            if (!headerMap.containsKey(HttpHeaders.HEAD_KEY_USER_AGENT)) {
                headerMap.put(HttpHeaders.HEAD_KEY_USER_AGENT, UAEnum.MOBILE.getContent());
            }
            return fetch(url, op);
        } else {
            Map op = new HashMap<>();
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put(HttpHeaders.HEAD_KEY_USER_AGENT, UAEnum.MOBILE.getContent());
            op.put("headers", headerMap);
            return fetch(url, op);
        }
    }

    /**
     * 供js使用fetch
     * 参考自https://github.com/mabDc/MyBookshelf/blob/master/app/src/main/java/com/kunfei/bookshelf/model/analyzeRule/AnalyzeRule.java
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String fetch(@Parameter("url") String url, @Parameter("options") Object options) {
        Timber.d("fetch, %s", url);
        long start = System.currentTimeMillis();
        try {
            if (StringUtil.isEmpty(url)) {
                return "";
            }
            Object hiker = fetchByHiker(url);
            if (hiker instanceof String) {
                return (String) hiker;
            }
            String contentType = null;
            boolean withHeaders = false;
            boolean withStatusCode = false;
            boolean redirect = true;
            int timeout = -1;
            Map<String, String> headerMap = null;
            Map<String, String> params = new HashMap<>();
            String fetchResult = "";

            //默认为空，okhttp自动识别
            String charset = null;
            String method = "GET";

            com.lzy.okgo.request.base.Request<String, ?> request = OkGo.get(url);
            try {
                if (options != null && !isUndefined(options)) {
                    Map op = (Map) argsNativeObjectAdjust(options);
                    headerMap = (Map<String, String>) op.get("headers");
                    if (headerMap == null) {
                        headerMap = (Map<String, String>) op.get("header");
                    }
                    if (op.containsKey("withHeaders")) {
                        try {
                            withHeaders = (Boolean) op.get("withHeaders");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("withStatusCode")) {
                        try {
                            withStatusCode = (Boolean) op.get("withStatusCode");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("redirect")) {
                        try {
                            redirect = (Boolean) op.get("redirect");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (op.containsKey("timeout")) {
                        try {
                            timeout = Integer.parseInt(JSON.toJSONString(op.get("timeout")));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    String body = null;
                    if (headerMap != null) {
                        contentType = headerMap.get("content-type");
                        if (StringUtil.isEmpty(contentType)) {
                            contentType = headerMap.get("Content-Type");
                        }
                        if (StringUtil.isEmpty(contentType)) {
                            contentType = headerMap.get("Content-type");
                        }
                        body = headerMap.get("body");
                    }
                    if (op.containsKey("body")) {
                        body = (String) op.get("body");
                    }
                    method = (String) op.get("method");
                    if (contentType != null && contentType.split("charset=").length > 1) {
                        charset = contentType.split("charset=")[1];
                    } else if (contentType != null && contentType.split("charst=").length > 1) {
                        //自己挖的坑，总是要填的
                        charset = contentType.split("charst=")[1];
                    }
                    if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                        request = "PUT".equalsIgnoreCase(method) ? OkGo.put(url) : OkGo.post(url);
                        if (StringUtil.isEmpty(body)) {
                            //empty
                        } else if (contentType != null && contentType.contains("application/json")) {
                            if ("PUT".equalsIgnoreCase(method)) {
                                ((PutRequest<?>) request).upJson(body);
                            } else {
                                ((PostRequest<?>) request).upJson(body);
                            }
                        } else {
                            for (String form : body.split("&")) {
                                int split = form.indexOf("=");
                                params.put(StringUtil.decodeConflictStr(form.substring(0, split)), StringUtil.decodeConflictStr(form.substring(split + 1)));
                            }
                            request.params(params);
                        }
                    }
                }
                if (!redirect && noRedirectHttpClient == null && timeout <= 0) {
                    buildOkHttpClient();
                }

                HttpHeaders httpHeaders = new HttpHeaders();
                if (headerMap != null && !headerMap.isEmpty()) {
                    for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                        httpHeaders.put(entry.getKey(), entry.getValue());
                    }
                }
                if (StringUtil.isNotEmpty(charset) && !"UTF-8".equalsIgnoreCase(charset)
                        && (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)))) {
                    FormBody.Builder bodyBuilder = new FormBody.Builder(Charset.forName(charset));
                    for (String key : request.getParams().urlParamsMap.keySet()) {
                        List<String> urlValues = request.getParams().urlParamsMap.get(key);
                        if (CollectionUtil.isNotEmpty(urlValues)) {
                            for (String value : urlValues) {
                                bodyBuilder.add(key, value);
                            }
                        }
                    }
                    if ("PUT".equalsIgnoreCase(method)) {
                        ((PutRequest<?>) request).upRequestBody(bodyBuilder.build());
                    } else {
                        ((PostRequest<?>) request).upRequestBody(bodyBuilder.build());
                    }
                    request.removeAllParams();
                }
                boolean finalWithHeaders = withHeaders;
                boolean finalWithStatusCode = withStatusCode;
                request.headers(httpHeaders);
                //需要禁止重定向，必须单独使用client
                if (!redirect && noRedirectHttpClient != null) {
                    request.client(noRedirectHttpClient);
                }
                if (timeout > 0) {
                    request.client(buildOkHttpClient(timeout, redirect));
                } else {
                    if (!redirect && noRedirectHttpClient != null) {
                        request.client(noRedirectHttpClient);
                    }
                }
                request.retryCount(0);
                Call<String> call = request.converter(new CharsetStringConvert(charset)).adapt();
                com.lzy.okgo.model.Response<String> response = call.execute();
                long end = System.currentTimeMillis();
                Timber.d("js, fetch: consume=%s, %s", (end - start), url);
                Map<String, List<String>> headers = response.headers() == null ? null : response.headers().toMultimap();
                String error = null;
                if (response.getException() != null) {
                    Timber.e(response.getException());
                    fetchResult = "";
                    error = response.getException().getMessage();
                } else {
                    fetchResult = response.body();
                }
                if (finalWithHeaders || finalWithStatusCode) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("body", fetchResult);
                    jsonObject.put("headers", headers);
                    jsonObject.put("statusCode", response.code());
                    jsonObject.put("error", error);
                    fetchResult = jsonObject.toJSONString();
                }
                return fetchResult == null ? "" : fetchResult;
            } catch (Throwable e) {
                Timber.e(e);
                return "";
            }
        } catch (Throwable e) {
            Timber.e(e);
            return "";
        }
    }

    private void buildOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setColorLevel(Level.INFO);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        noRedirectHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(BrotliInterceptor.INSTANCE)
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier)
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(loggingInterceptor)
                .readTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .writeTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .connectTimeout(TimeConstants.HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .build();
    }

    private OkHttpClient buildOkHttpClient(int timeout, boolean redirect) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setColorLevel(Level.INFO);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        if (!redirect) {
            builder.followRedirects(false)
                    .followSslRedirects(false);
        }
        return builder.addInterceptor(loggingInterceptor)
                .addInterceptor(BrotliInterceptor.INSTANCE)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }


    /**
     * 供js使用fetch
     * 参考自https://github.com/mabDc/MyBookshelf/blob/master/app/src/main/java/com/kunfei/bookshelf/model/analyzeRule/AnalyzeRule.java
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String fetchCookie(@Parameter("url") String url, @Parameter("options") Object options) {
        try {
            Map op;
            if (options == null) {
                op = new HashMap<>();
            } else {
                op = (Map) argsNativeObjectAdjust(options);
            }
            op.put("withHeaders", true);
            String result = fetch(url, op);
            JSONObject jsonObject = JSON.parseObject(result);
            if (jsonObject.containsKey("headers")) {
                JSONObject jsonObject1 = jsonObject.getJSONObject("headers");
                JSONArray cookies = jsonObject1.getJSONArray("set-cookie");
                if (cookies == null) {
                    cookies = jsonObject1.getJSONArray("set-Cookie");
                }
                if (cookies == null) {
                    cookies = jsonObject1.getJSONArray("Set-Cookie");
                }
                if (cookies == null) {
                    return "";
                } else {
                    return cookies.toJSONString();
                }
            }
            return "";
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    @JSAnnotation
    public void writeFile(@Parameter("filePath") String filePath, @Parameter("content") String content) {
        if (filePath.startsWith("hiker://files/")) {
            String fileName = filePath.replace("hiker://files/", "");
            filePath = UriUtils.getRootDir(Application.application.getApplicationContext()) + File.separator + fileName;
        } else if (filePath.startsWith("file://")) {
            filePath = filePath.replace("file://", "");
        }
        if (filePath.startsWith(UriUtils.getRootDir(Application.application.getApplicationContext()))) {
            try {
                FileUtil.stringToFile(content, filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析dom
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String parseDomForHtml(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String html = (String) oo1;
        String rule = (String) oo2;
        return CommonParser.parseDomForUrl(html, rule, "");
    }

    /**
     * 解析dom
     *
     * @return
     */
    @JSAnnotation(returnType = 2)
    public String parseDomForArray(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String html = (String) oo1;
        String rule = (String) oo2;
        return JSON.toJSONString(CommonParser.parseDomForList(html, rule));
    }

    /**
     * 解析dom
     *
     * @return
     */
    @JSAnnotation(returnType = 1)
    public String parseDom(@Parameter("o1") Object o1, @Parameter("o2") Object o2, @Parameter("urlKey") Object urlKey) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String html = (String) oo1;
        String rule = (String) oo2;
        return CommonParser.parseDomForUrl(html, rule, (String) argsNativeObjectAdjust(urlKey));
    }

    @JSAnnotation(returnType = 1)
    public String getUaObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mobileUa", UAEnum.MOBILE.getContent());
        jsonObject.put("pcUa", UAEnum.PC.getContent());
        return jsonObject.toJSONString();
    }

    @JSAnnotation(returnType = 3)
    public String hasHomeSub(@Parameter("url") Object url) {
        Object oo1 = argsNativeObjectAdjust(url);
        if (!(oo1 instanceof String) || StringUtil.isEmpty((String) oo1)) {
            return Boolean.FALSE.toString();
        }
        String urls = (String) oo1;
        List<SubscribeRecord> records = HomeRulesSubService.getSubRecords();
        if (CollectionUtil.isNotEmpty(records)) {
            for (SubscribeRecord record : records) {
                if (urls.equals(record.getUrl())) {
                    return Boolean.TRUE.toString();
                }
            }
        }
        return Boolean.FALSE.toString();
    }

    @JSAnnotation(returnType = 2)
    public String getHomeSub() {
        List<SubscribeRecord> records = HomeRulesSubService.getSubRecords();
        return JSON.toJSONString(records);
    }


    @JSAnnotation(returnType = 2)
    public String getLastRules(@Parameter("c") Object c) {
        int count;
        if (c == null || Undefined.isUndefined(c)) {
            count = Integer.MAX_VALUE;
        } else {
            Object oo1 = argsNativeObjectAdjust(c);
            if (oo1 instanceof Integer) {
                count = (Integer) oo1;
            } else if (oo1 instanceof Double) {
                count = ((Double) oo1).intValue();
            } else {
                return "[]";
            }
            if (count == -1) {
                count = Integer.MAX_VALUE;
            }
        }
        List<ArticleListRule> rules = LitePal.findAll(ArticleListRule.class);
        List<ArticleListRule> result = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(rules)) {
            Collections.sort(rules, (o1, o2) -> Long.compare(o2.getLastUseTime(), o1.getLastUseTime()));
            for (int i = 0; i < rules.size() && i < count; i++) {
                if (rules.get(i).getLastUseTime() <= 0) {
                    continue;
                }
                result.add(rules.get(i));
            }
        }
        return JSON.toJSONString(result, JSONPreFilter.getSimpleFilter());
    }

    private String getReplaceJS(String js) {
        if (StringUtil.isNotEmpty(js)) {
            js = js.replace("if (b != null && b.length() > 0) {", "if (b != null && b.length > 0) {");
            return "try{\n" + js + "\n}catch(e){\nsetError(JSON.stringify(e));\n}";
        }

        return js;
    }


    /**
     * 通过注解自动生成js方法语句
     */
    private String getAllFunctions() {
        String funcStr = " var ScriptAPI = java.lang.Class.forName(\"%s\", true, javaLoader);\n";
        Class cls = this.getClass();
        for (Method method : cls.getDeclaredMethods()) {
            JSAnnotation an = method.getAnnotation(JSAnnotation.class);
            if (an == null) continue;
            funcStr = getFunctionStr(funcStr, an.returnType(), method);
        }
        return funcStr;
    }

    /**
     * 通过注解自动生成js方法语句
     */
    private String getEvalFunctions() {
        String funcStr = " var ScriptAPI = java.lang.Class.forName(\"%s\", true, javaLoader);\n";
        Class cls = this.getClass();
        for (Method method : cls.getDeclaredMethods()) {
            EvalJSAnnotation an = method.getAnnotation(EvalJSAnnotation.class);
            if (an == null) continue;
            funcStr = getFunctionStr(funcStr, an.returnType(), method);
        }
        return funcStr;
    }

    private String getFunctionStr(String funcStr, int type, Method method) {
        String functionName = method.getName();
        String paramsTypeString = "";//获取function的参数类型
        String paramsNameString = "";//获取function的参数名称
        String paramsNameInvokeString = "";
        Class[] parmTypeArray = method.getParameterTypes();

        if (parmTypeArray.length > 0) {
            String[] parmStrArray = new String[parmTypeArray.length];
            String[] parmNameArray = new String[parmTypeArray.length];
            for (int i = 0; i < parmTypeArray.length; i++) {
                parmStrArray[i] = parmTypeArray[i].getName();
                parmNameArray[i] = "param" + i;
            }
            paramsTypeString = String.format(",[%s]", TextUtils.join(",", parmStrArray));
            paramsNameString = TextUtils.join(",", parmNameArray);
            paramsNameInvokeString = "," + paramsNameString;
        }

        StringBuilder paramCheck = new StringBuilder();

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (Parameter.class.equals(annotation.annotationType())) {
                    String parameterName = ((Parameter) annotation).value();
                    switch (parameterName) {
                        case "callbackKey":
                            paramCheck.append("param").append(i).append(" = CALLBACK_KEY;\n");
                            break;
                        case "ruleKey":
                            paramCheck.append("param").append(i).append(" = MY_RULE;\n");
                            break;
                        case "urlKey":
                            paramCheck.append("param").append(i).append(" = MY_URL;\n");
                            break;
                        case "ruleStrKey":
                            paramCheck.append("param").append(i).append(" = my_rule;\n");
                            break;
                        case "typeKey":
                            paramCheck.append("param").append(i).append(" = MY_TYPE;\n");
                            break;
                    }
                }
            }
        }

        Class returnType = method.getReturnType();
        String returnStr = returnType.getSimpleName().equals("void") ? "" : "return";//是否有返回值

        String methodStr = String.format(" var method_%s = ScriptAPI.getMethod(\"%s\"%s);\n", functionName, functionName, paramsTypeString);
        String functionStr = "";
        if (type == 1) {
            //返回字符串
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return retStr + '';\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == 2) {
            //返回对象
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return JSON.parse(retStr);\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == 3) {
            //返回布尔类型
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return retStr + '' == 'true';\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else {
            //非返回对象
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    %s method_%s.invoke(javaContext%s);\n" +
                            " }\n", functionName, paramsNameString, returnStr, functionName, paramsNameInvokeString);
        }
        return funcStr + methodStr + functionStr;
    }


    /**
     * 参数调整：
     * 存在问题：从js传入的JSON 对象，类型变为 NativeObject；而NativeObject 中的String类型可能被js转为
     * ConsString 类型；用 Gson.toJson(xxx) 处理带有ConsString 类型的数据会出现异常。其中的ConsString
     * 类型的数据转化出来并不是 String 类型，而是一个特殊对象。
     * 解决方案：遍历 NativeObject 对象，将其中的 ConsString 类型的数据转为 String 类型
     *
     * @param input
     * @return
     */
    private Object argsNativeObjectAdjust(Object input) {
        if (Undefined.isUndefined(input)) {
            return input;
        }

        if (input instanceof NativeObject) {
            JSONObject bodyJson = new JSONObject();
            NativeObject nativeBody = (NativeObject) input;
            for (Object key : nativeBody.keySet()) {
                Object value = nativeBody.get(key);

                value = argsNativeObjectAdjust(value);
                try {
                    bodyJson.put((String) key, value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return bodyJson;
        }

        if (input instanceof NativeArray) {
            JSONArray jsonArray = new JSONArray();
            NativeArray nativeArray = (NativeArray) input;
            for (int i = 0; i < nativeArray.size(); i++) {
                Object value = nativeArray.get(i);
                value = argsNativeObjectAdjust(value);
                jsonArray.add(value);
            }

            return jsonArray;
        }

        if (input instanceof ConsString) {
            return input.toString();
        }

        if (input instanceof NativeJavaObject) {
            return input.toString();
        }
        return input;
    }

    public interface OnFindCallBack<T> {
        void onSuccess(T data);

        void showErr(String msg);
    }

    /**
     * 注解
     */
    @Target(value = ElementType.METHOD)
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface JSAnnotation {
        int returnType() default 0;//是否返回对象，默认为false 不返回，1：字符串
    }

    /**
     * 注解
     */
    @Target(value = ElementType.METHOD)
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface EvalJSAnnotation {
        int returnType() default 0;//是否返回对象，默认为false 不返回，1：字符串
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Parameter {
        String value() default "";
    }
}
