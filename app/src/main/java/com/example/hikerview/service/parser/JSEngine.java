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
import com.example.hikerview.event.home.OnRefreshPageEvent;
import com.example.hikerview.event.home.ToastEvent;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.service.http.CharsetStringConvert;
import com.example.hikerview.service.http.HikerRuleUtil;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.SearchEngine;
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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private String lastResCode = "";
    private MovieRule movieRule;
    private OnFindCallBack<String> stringOnFindCallBack;
    private OnFindCallBack<List<SearchResult>> searchFindCallBack;
    private OnFindCallBack<List<ArticleList>> homeCallBack;
    private volatile static LoadMode loadMode = LoadMode.MOVIE_FIND;
    private String rule = null;
    private String ruleObject = null;
    private Map<String, String> varMap = new HashMap<>();
    private String url;
    private String input = "";
    private OkHttpClient noRedirectHttpClient;

    private enum LoadMode {
        SEARCH,
        MOVIE_FIND,
        CHAPTER,
        HOME,
        JS_PRE_RULE
    }

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

    private void destroyCallbacks() {
        homeCallBack = null;
        stringOnFindCallBack = null;
        searchFindCallBack = null;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public synchronized void parseSearchRes(String url, String res, SearchEngine searchEngine, OnFindCallBack<List<SearchResult>> searchJsCallBack) {
        parseSearchRes(url, res, searchEngine.toMovieRule(), searchJsCallBack);
    }

    private synchronized void parseSearchRes(String url, String res, MovieRule movieRule, OnFindCallBack<List<SearchResult>> searchJsCallBack) {
        destroyCallbacks();
        this.url = url;
        loadMode = LoadMode.SEARCH;
        this.searchFindCallBack = searchJsCallBack;
        this.lastResCode = res;
        this.movieRule = movieRule;
        rule = null;
        if (!movieRule.getSearchFind().startsWith("js:")) {
            searchJsCallBack.showErr(movieRule.getTitle() + "---搜索结果解析失败！请检查规则");
        } else {
            try {
                runScript("var MY_URL = '" + Utils.escapeJavaScriptString(url) + "';\n" + movieRule.getSearchFind().replaceFirst("js:", ""));
            } catch (Exception e) {
                Timber.e(e, "parseSearchRes: ");
                setError("运行出错：" + e.toString());
            }
        }
    }

    public synchronized void parsePreRule(ArticleListRule articleListRule) {
        loadMode = LoadMode.JS_PRE_RULE;
        try {
            runScript("var my_rule = '" + Utils.escapeJavaScriptString(JSON.toJSONString(articleListRule)) + "';\n var MY_RULE = JSON.parse(my_rule);\n" + articleListRule.getPreRule());
        } catch (Exception e) {
            setError("运行出错：" + e.toString());
            Timber.e(e, "parsePreRule: ");
        }
    }


    public synchronized void parsePreRule(SearchEngine articleListRule) {
        loadMode = LoadMode.JS_PRE_RULE;
        try {
            runScript("var my_rule = '" + Utils.escapeJavaScriptString(JSON.toJSONString(articleListRule)) + "';\n var MY_RULE = JSON.parse(my_rule);\n" + articleListRule.getPreRule());
        } catch (Exception e) {
            setError("运行出错：" + e.toString());
            Timber.e(e, "parsePreRule: ");
        }
    }

    public synchronized void parseStr(String input, String js, MovieRule movieRule, OnFindCallBack<String> callBack) {
        destroyCallbacks();
        loadMode = LoadMode.SEARCH;
        this.stringOnFindCallBack = callBack;
        this.lastResCode = input;
        this.movieRule = movieRule;
        rule = null;
        this.ruleObject = JSON.toJSONString(movieRule);
        try {
            runScript(js);
        } catch (Exception e) {
            setError("运行出错：" + e.toString());
            Timber.e(e, "parseStr: ");
        }
    }

    public synchronized void parseHome(String url, String input, ArticleListRule articleListRule, String js, OnFindCallBack<List<ArticleList>> callBack) {
        destroyCallbacks();
        this.url = url;
        loadMode = LoadMode.HOME;
        this.homeCallBack = callBack;
        this.lastResCode = input;
        this.rule = js.replaceFirst("js:", "");
        this.movieRule = null;
        this.ruleObject = JSON.toJSONString(articleListRule, JSONPreFilter.getSimpleFilter());
        try {
            runScript("\nvar MY_URL = '" + Utils.escapeJavaScriptString(url) + "';\n" + rule);
        } catch (Exception e) {
            setError("运行出错：" + e.toString());
            Timber.e(e, "parseHome: ");
        }
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
    public String getResCode() {
        return this.lastResCode;
    }

    /**
     * 供js获取相关信息
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String getUrl() {
        return this.url;
    }

    /**
     * 供js获取相关信息
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String getVar(Object o, Object defaultVal) {
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

    private boolean isUndefined(Object input){
        if(input instanceof String && "undefined".equals(input)){
            return true;
        }
        return Undefined.isUndefined(input);
    }

    public void putVar(Object o) {
        putVar(o, null);
    }

    @JSAnnotation
    public void putVar(Object o, Object o2) {
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
    public void putVar2(Object o1, Object o2) {
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
    public String decodeStr(Object o1, Object o2) {
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
    public String encodeStr(Object o1, Object o2) {
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
    public String base64Decode(Object o1) {
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
    public String base64Encode(Object o1) {
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
    public String aesDecode(Object o1, Object o2) {
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
    public String aesEncode(Object o1, Object o2) {
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
    public String getRule() {
        if (!TextUtils.isEmpty(ruleObject)) {
            return ruleObject;
        }
        if (this.movieRule != null) {
            return JSON.toJSONString(this.movieRule);
        }
        if (!TextUtils.isEmpty(rule)) {
            return rule;
        }
        return "";
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
    public void setStrResult(Object o) {
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            if (this.stringOnFindCallBack != null) {
                this.stringOnFindCallBack.showErr(movieRule.getTitle() + "---视频解析失败！请检查规则");
            }
            return;
        }
        try {
            if (this.stringOnFindCallBack != null) {
                this.stringOnFindCallBack.onSuccess((String) res);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.stringOnFindCallBack.showErr(movieRule.getTitle() + "---视频解析失败！请检查规则");
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setSearchResult(Object o) {
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof JSONObject)) {
            if (this.searchFindCallBack != null) {
                this.searchFindCallBack.showErr(movieRule.getTitle() + "---搜索结果解析失败！请检查规则");
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
                    searchResult.setDesc(this.movieRule.getTitle());
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
            this.searchFindCallBack.onSuccess(results);
        } catch (Exception e) {
            e.printStackTrace();
            this.searchFindCallBack.showErr(movieRule.getTitle() + "---搜索结果解析失败！请检查规则");
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setHomeResult(Object o) {
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof JSONObject)) {
            if (this.homeCallBack != null) {
                this.homeCallBack.showErr("---分类结果解析失败！请检查规则：setHomeResult is not JSONObject");
            }
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
            this.homeCallBack.onSuccess(results);
        } catch (Exception e) {
            e.printStackTrace();
            this.homeCallBack.showErr("---分类结果解析失败！请检查规则：" + e.getMessage());
        }
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setError(Object o) {
        Object res = argsNativeObjectAdjust(o);
        if (res instanceof String) {
            Timber.d("setError: %s", res.toString());
            if (loadMode == LoadMode.HOME && homeCallBack != null) {
                homeCallBack.showErr("---解析失败！请检查规则：" + res);
            } else if (loadMode == LoadMode.SEARCH && searchFindCallBack != null) {
                searchFindCallBack.showErr("---解析失败！请检查规则：" + res);
            } else {
                EventBus.getDefault().post(new ToastEvent("---解析失败！请检查规则：" + res));
            }
        } else {
            Timber.d("setError: %s", JSON.toJSONString(res));
            if (loadMode == LoadMode.HOME && homeCallBack != null) {
                homeCallBack.showErr("---解析失败！请检查规则：" + JSON.toJSONString(res));
            } else if (loadMode == LoadMode.SEARCH && searchFindCallBack != null) {
                searchFindCallBack.showErr("---解析失败！请检查规则：" + res);
            } else {
                EventBus.getDefault().post(new ToastEvent("---解析失败！请检查规则：" + JSON.toJSONString(res)));
            }
        }
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

//    public String request(String url, Object options) {
//        try {
//            if (StringUtil.isEmpty(url)) {
//                return "";
//            }
//            if (url.startsWith("hiker://files/")) {
//                String fileName = url.replace("hiker://files/", "");
//                File file = new File(SettingConfig.rootDir + File.separator + fileName);
//                if (file.exists()) {
//                    return FileUtil.fileToString(file.getAbsolutePath());
//                } else {
//                    return "";
//                }
//            } else if (url.startsWith("file://")) {
//                url = url.replace("file://", "");
//                File file = new File(url);
//                if (file.exists()) {
//                    return FileUtil.fileToString(file.getAbsolutePath());
//                } else {
//                    return "";
//                }
//            }
//            if (url.startsWith("hiker://")) {
//                try {
//                    return HikerRuleUtil.getRulesByHiker(url);
//                } catch (Exception e) {
//                    return "";
//                }
//            }
//            String contentType = null;
//            boolean withHeaders = false;
//            boolean redirect = true;
//            try {
//                Request.Builder request = new Request.Builder().url(url);
//                if (options != null) {
//                    Map op = (Map) argsNativeObjectAdjust(options);
//                    Map<String, String> headers = (Map<String, String>) op.get("headers");
//                    if (headers == null) {
//                        headers = (Map<String, String>) op.get("header");
//                    }
//                    if (op.containsKey("withHeaders")) {
//                        try {
//                            withHeaders = (Boolean) op.get("withHeaders");
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    if (op.containsKey("redirect")) {
//                        try {
//                            redirect = (Boolean) op.get("redirect");
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    String body = "";
//                    if (headers != null) {
//                        contentType = headers.get("content-type");
//                        body = headers.get("body");
//                        request = request.headers(Headers.of(headers));
//                    }
//                    if (op.containsKey("body")) {
//                        body = (String) op.get("body");
//                    }
//                    String method = (String) op.get("method");
//                    if (body != null && method != null && method.equalsIgnoreCase("POST")) {
//                        if ((contentType != null && contentType.equals("application/json")) || StringUtil.isEmpty(body)) {
//                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charst=utf-8"), body);
//                            request = request.post(requestBody);
//                        } else {
//                            FormBody.Builder builder = new FormBody.Builder();
//                            for (String form : body.split("&")) {
//                                int split = form.indexOf("=");
//                                builder.add(StringUtil.decodeConflictStr(form.substring(0, split)), StringUtil.decodeConflictStr(form.substring(split + 1)));
//                            }
//                            request = request.post(builder.build());
//                        }
//                    }
//                }
//                try (Response response = new OkHttpClient().newBuilder()
//                        .followRedirects(redirect)
//                        .followSslRedirects(redirect)
//                        .build().newCall(request.build()).execute();
//                     ResponseBody body = response.body()) {
//                    Map<String, List<String>> headers = response.headers().toMultimap();
//                    String s;
//                    if (body != null) {
//                        if (contentType != null && contentType.split("charst=").length > 1) {
//                            String code = contentType.split("charst=")[1];
//                            Log.d(TAG, "fetch: " + code);
//                            s = new String(body.bytes(), code);
//                        } else {
//                            s = body.string();
//                        }
//                        //                Log.d(TAG, "fetch: " + s);
//                    } else {
//                        s = "";
//                    }
//                    if (withHeaders) {
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("body", s);
//                        jsonObject.put("headers", headers);
//                        return jsonObject.toJSONString();
//                    }
//                    return s;
//                }
//            } catch (Throwable e) {
//                return "";
//            }
//        } catch (Throwable e) {
//            return "";
//        }
//    }

    /**
     * 供js使用fetch
     * 参考自https://github.com/mabDc/MyBookshelf/blob/master/app/src/main/java/com/kunfei/bookshelf/model/analyzeRule/AnalyzeRule.java
     *
     * @return 源码
     */
    @JSAnnotation(returnType = 1)
    public String fetch(String url, Object options) {
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

                String charst = "UTF-8";
                if (contentType != null && contentType.split("charst=").length > 1) {
                    charst = contentType.split("charst=")[1];
                }
                boolean finalWithHeaders = withHeaders;
                boolean finalWithStatusCode = withStatusCode;
                request.headers(httpHeaders);
                //需要禁止重定向，必须单独使用client
                if (!redirect && noRedirectHttpClient != null) {
                    request.client(noRedirectHttpClient);
                }
                request.retryCount(0);
                Call<String> call = request.converter(new CharsetStringConvert(charst)).adapt();
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
    public String fetchCookie(String url, Object options) {
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
    public void writeFile(String filePath, String content) {
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
    public String parseDomForHtml(Object o1, Object o2) {
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
    public String parseDomForArray(Object o1, Object o2) {
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
    public String parseDom(Object o1, Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String) || !(oo2 instanceof String)) {
            return "";
        }
        String html = (String) oo1;
        String rule = (String) oo2;
        return CommonParser.parseDomForUrl(html, rule, this.url);
    }

    @JSAnnotation(returnType = 1)
    public String getUaObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mobileUa", UAEnum.MOBILE.getContent());
        jsonObject.put("pcUa", UAEnum.PC.getContent());
        return jsonObject.toJSONString();
    }

    /**
     * 编解码
     *
     * @return
     */
    @EvalJSAnnotation(returnType = 1)
    public String getInputVal() {
        return input;
    }

    /**
     * 执行JS
     *
     * @param js js执行代码 eg: "var v1 = getValue('Ta');setValue(‘key’，v1);"
     */
    private synchronized void runScript(String js) {
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
        }
    }

    private String getReplaceJS(String js) {
        if (StringUtil.isNotEmpty(js)) {
            js = js.replace("if (b != null && b.length() > 0) {", "if (b != null && b.length > 0) {");
            return "try{\n" + js + "\n}catch(e){\nsetError(JSON.stringify(e));\n}";
        }

        return js;
    }

    public synchronized String evalJS(String jsStr, String input) {
        destroyCallbacks();
        this.input = input;
        String js = evalFunctions + "\n var input = getInputVal();" + StringUtil.decodeConflictStr(jsStr);//运行js = allFunctions + js
//        String js = "var input = `" + input + "`;" + StringUtil.decodeConflictStr(jsStr);
//        Log.d(TAG, "evalJS: " + js);
        js = allFunctions + "\n" + getReplaceJS(js);//运行js = allFunctions + js
        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        rhino.setOptimizationLevel(-1);
        rhino.setLanguageVersion(200);
        try {
            Scriptable scope = rhino.initStandardObjects();
            ScriptableObject.putProperty(scope, "javaContext", org.mozilla.javascript.Context.javaToJS(this, scope));//配置属性 javaContext:当前类JSEngine的上下文
            ScriptableObject.putProperty(scope, "javaLoader", org.mozilla.javascript.Context.javaToJS(clazz.getClassLoader(), scope));//配置属性 javaLoader:当前类的JSEngine的类加载器
            Object re = rhino.evaluateString(scope, js, clazz.getSimpleName(), 1, null);
//            Log.d(TAG, "evalJS: " + re);
            if (re instanceof String) {
                return (String) re;
            } else {
                return re + "";
            }
        } finally {
            org.mozilla.javascript.Context.exit();
        }
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
        if (parmTypeArray != null && parmTypeArray.length > 0) {
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

        Class returnType = method.getReturnType();
        String returnStr = returnType.getSimpleName().equals("void") ? "" : "return";//是否有返回值

        String methodStr = String.format(" var method_%s = ScriptAPI.getMethod(\"%s\"%s);\n", functionName, functionName, paramsTypeString);
        String functionStr = "";
        if (type == 1) {
            //返回字符串
            functionStr = String.format(
                    " function %s(%s){\n" +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return retStr + '';\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == 2) {
            //返回对象
            functionStr = String.format(
                    " function %s(%s){\n" +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    var ret = {} ;\n" +
                            "    eval('ret='+retStr);\n" +
                            "    return ret;\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else {
            //非返回对象
            functionStr = String.format(
                    " function %s(%s){\n" +
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

        if(Undefined.isUndefined(input)){
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
}
