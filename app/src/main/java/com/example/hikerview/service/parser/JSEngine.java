package com.example.hikerview.service.parser;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.constants.TimeConstants;
import com.example.hikerview.constants.UAEnum;
import com.example.hikerview.event.home.OnRefreshPageEvent;
import com.example.hikerview.event.home.ToastEvent;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.service.http.CharsetStringConvert;
import com.example.hikerview.service.http.HikerRuleUtil;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.SearchEngine;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.home.model.ArticleList;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.SearchResult;
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

import org.adblockplus.libadblockplus.android.Utils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.OkHttpClient;
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

    private JSEngine() {
        this.clazz = JSEngine.class;
        allFunctions = String.format(getAllFunctions(), clazz.getName()) +
                "\n var MY_UA = JSON.parse(getUaObject());" +
                "\n var MOBILE_UA =  MY_UA.mobileUa;" +
                "\n var PC_UA = MY_UA.pcUa";//生成js语法
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
                runScript(getMyCallbackKey(callbackKey) + getMyRule(movieRule) + getMyUrl(url) + getMyJs(movieRule.getSearchFind()), callbackKey);
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
            runScript(getMyCallbackKey(callbackKey) + getMyRule(movieRule) + getMyJs(js), callbackKey);
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
            runScript("\n" + getMyRule(articleListRule) + getMyCallbackKey(callbackKey) + getMyUrl(url) + getMyJs(js), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parseHome: ");
        }
    }

    public void parsePreRule(ArticleListRule articleListRule) {
        String callbackKey = UUIDUtil.genUUID();
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyRule(articleListRule) + articleListRule.getPreRule(), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parsePreRule: ");
        }
    }


    public void parsePreRule(SearchEngine articleListRule) {
        String callbackKey = UUIDUtil.genUUID();
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyRule(articleListRule) + articleListRule.getPreRule(), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey);
            Timber.e(e, "parsePreRule: ");
        }
    }

    public String evalJS(String jsStr, String input) {
        String js = evalFunctions + "\n" + getMyInput(input) + getMyCallbackKey(UUIDUtil.genUUID()) + StringUtil.decodeConflictStr(jsStr);//运行js = allFunctions + js
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
            } else {
                return re + "";
            }
        } finally {
            org.mozilla.javascript.Context.exit();
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

    private String getMyRule(Object rule) {
        return "var my_rule = '" + Utils.escapeJavaScriptString(JSON.toJSONString(rule)) + "';\n var MY_RULE = JSON.parse(my_rule);\n";
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


    /**
     * 供js获取相关信息
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String getResCode(@Parameter("callbackKey") Object callbackKey) {
        return resCodeMap.get((String) argsNativeObjectAdjust(callbackKey));
    }

    /**
     * 供js获取相关信息
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String getUrl(@Parameter("urlKey") Object urlKey) {
        return (String) argsNativeObjectAdjust(urlKey);
    }

    /**
     * 供js获取相关信息
     *
     * @return 源码
     */
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
        return new String(Base64.decode((String) oo1, Base64.DEFAULT));
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
        return new String(Base64.encode(((String) oo1).getBytes(), Base64.DEFAULT));
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
     * 供js回调
     */
    @JSAnnotation
    public void refreshPage() {
        EventBus.getDefault().post(new OnRefreshPageEvent());
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
    public void setSearchResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey, @Parameter("ruleKey") Object ruleKey) {
        Object res = argsNativeObjectAdjust(o);
        Object rule = argsNativeObjectAdjust(ruleKey);
        String movieTitle = ((JSONObject) rule).getString("Title");

        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }
        callbackMap.remove(callbackStr);

        if (!(res instanceof JSONObject)) {
            if (onFindCallBack != null) {
                onFindCallBack.showErr(movieTitle + "---搜索结果解析失败！请检查规则");
            }
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
            onFindCallBack.showErr(movieTitle + "---搜索结果解析失败！请检查规则");
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setHomeResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey) {
        Object res = argsNativeObjectAdjust(o);
        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }
        callbackMap.remove(callbackStr);

        if (!(res instanceof JSONObject)) {
            onFindCallBack.showErr("---分类结果解析失败！请检查规则：setHomeResult is not JSONObject");
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
            onFindCallBack.showErr("---分类结果解析失败！请检查规则：" + e.getMessage());
        }
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
            try {
                return HikerRuleUtil.getRulesByHiker(url);
            } catch (Exception e) {
                return "";
            }
        }
        return false;
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
            Map<String, String> headerMap = null;
            Map<String, String> params = new HashMap<>();
            String fetchResult = "";

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
                    String method = (String) op.get("method");
                    if ("POST".equalsIgnoreCase(method)) {
                        if (StringUtil.isEmpty(body)) {
                            request = OkGo.post(url);
                        } else if (contentType != null && contentType.equals("application/json")) {
                            request = OkGo.post(url);
                            ((PostRequest<?>) request).upJson(body);
                        } else {
                            for (String form : body.split("&")) {
                                int split = form.indexOf("=");
                                params.put(StringUtil.decodeConflictStr(form.substring(0, split)), StringUtil.decodeConflictStr(form.substring(split + 1)));
                            }
                            request = OkGo.post(url);
                            ((PostRequest<?>) request).params(params);
                        }
                    }
                }
                if (!redirect && noRedirectHttpClient == null) {
                    buildOkHttpClient();
                }

                HttpHeaders httpHeaders = new HttpHeaders();
                if (headerMap != null && !headerMap.isEmpty()) {
                    for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                        httpHeaders.put(entry.getKey(), entry.getValue());
                    }
                }

                String charset = "UTF-8";
                if (contentType != null && contentType.split("charset=").length > 1) {
                    charset = contentType.split("charset=")[1];
                } else if (contentType != null && contentType.split("charst=").length > 1) {
                    //自己挖的坑，总是要填的
                    charset = contentType.split("charst=")[1];
                }
                boolean finalWithHeaders = withHeaders;
                boolean finalWithStatusCode = withStatusCode;
                request.headers(httpHeaders);
                //需要禁止重定向，必须单独使用client
                if (!redirect && noRedirectHttpClient != null) {
                    request.client(noRedirectHttpClient);
                }
                request.retryCount(0);
                Call<String> call = request.converter(new CharsetStringConvert(charset)).adapt();
                com.lzy.okgo.model.Response<String> response = call.execute();
                long end = System.currentTimeMillis();
                Timber.d("js, fetch: consume=%s, %s", (end - start), url);
                Map<String, List<String>> headers = response.headers() == null ? null : response.headers().toMultimap();
                if (response.getException() != null) {
                    Timber.e(response.getException());
                    fetchResult = "";
                } else {
                    fetchResult = response.body();
                }
                if (finalWithHeaders || finalWithStatusCode) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("body", fetchResult);
                    jsonObject.put("headers", headers);
                    jsonObject.put("statusCode", response.code());
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
                            "    var ret = {} ;\n" +
                            "    eval('ret='+retStr);\n" +
                            "    return ret;\n" +
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
