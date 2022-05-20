package com.example.hikerview.service.parser;

import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.constants.UAEnum;
import com.example.hikerview.event.OnBackEvent;
import com.example.hikerview.event.home.CopyEvent;
import com.example.hikerview.event.home.LoadingEvent;
import com.example.hikerview.event.home.OnRefreshPageEvent;
import com.example.hikerview.event.home.OnRefreshWebViewEvent;
import com.example.hikerview.event.home.OnRefreshX5HeightEvent;
import com.example.hikerview.event.home.RuleModifiedEvent;
import com.example.hikerview.event.home.SetPageLastChapterRuleEvent;
import com.example.hikerview.event.home.SetPagePicEvent;
import com.example.hikerview.event.home.SetPageTitleEvent;
import com.example.hikerview.event.home.ToastEvent;
import com.example.hikerview.event.rule.ClsItemsFindEvent;
import com.example.hikerview.event.rule.ConfirmEvent;
import com.example.hikerview.event.rule.ItemFindEvent;
import com.example.hikerview.event.rule.ItemModifyEvent;
import com.example.hikerview.model.BigTextDO;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.service.auth.AuthBridgeEvent;
import com.example.hikerview.service.auth.AuthStorageItem;
import com.example.hikerview.service.auth.RhinoClassShutter;
import com.example.hikerview.service.exception.ParseException;
import com.example.hikerview.service.http.CodeUtil;
import com.example.hikerview.service.http.HikerRuleUtil;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.SearchEngine;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.download.util.UUIDUtil;
import com.example.hikerview.ui.home.model.ArticleList;
import com.example.hikerview.ui.home.model.ArticleListPageRule;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.SearchResult;
import com.example.hikerview.ui.home.model.article.extra.BaseExtra;
import com.example.hikerview.ui.rules.model.AccountPwd;
import com.example.hikerview.ui.rules.model.SubscribeRecord;
import com.example.hikerview.ui.rules.service.HomeRulesSubService;
import com.example.hikerview.ui.rules.service.RuleImporterManager;
import com.example.hikerview.ui.rules.service.require.RequireUtils;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.ui.webdlan.LocalServerParser;
import com.example.hikerview.utils.FileUtil;
import com.example.hikerview.utils.FilesInAppUtil;
import com.example.hikerview.utils.ImgUtil;
import com.example.hikerview.utils.M3u8Utils;
import com.example.hikerview.utils.StringUtil;
import com.example.hikerview.utils.ThreadTool;
import com.example.hikerview.utils.TimeUtil;
import com.example.hikerview.utils.ToastMgr;
import com.example.hikerview.utils.UriUtils;
import com.example.hikerview.utils.encrypt.AesUtil;
import com.example.hikerview.utils.encrypt.MyBase64;
import com.example.hikerview.utils.encrypt.rsa.RSAEncrypt;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lzy.okgo.model.HttpHeaders;

import org.adblockplus.libadblockplus.android.Utils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.litepal.LitePal;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import dalvik.system.DexClassLoader;
import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2018/12/10
 * 时间：At 12:04
 */
public class JSEngine {
    static {
        ContextFactory.initGlobal(new ContextFactory() {
            @Override
            protected Context makeContext() {
                Context context = super.makeContext();
                context.setClassShutter(new RhinoClassShutter());
                return context;
            }
        });
    }

    private static final String TAG = "JSEngine";
    private Class clazz;
    private String allFunctions = "";
    private volatile static JSEngine engine;
    private Map<String, String> varMap = new HashMap<>();
    private Map<String, OnFindCallBack<?>> callbackMap = new ConcurrentHashMap<>();
    private Map<String, String> resCodeMap = new ConcurrentHashMap<>();
    private String jsPlugin;
    private String jsLazyPlugin;
    private List<String> logs = new ArrayList<>();
    private static Cache<String, String> ticketCache = CacheBuilder.newBuilder()
            // 设置初始容量为100
            .initialCapacity(100)
            //3分钟自动失效
            .expireAfterAccess(3, TimeUnit.MINUTES)
            .build();
    public static final String AES_DEFAULT_KEY = "1234567890kkkk";

    public boolean isTraceLog() {
        return traceLog;
    }

    private boolean traceLog;
    private RC4 rc4;
    private MyBase64 myBase64;
    private List<ClassLoader> classloaders;
    private List<String> methodList;

    private JSEngine() {
        this.clazz = JSEngine.class;
        this.rc4 = new RC4();
        this.myBase64 = new MyBase64();
        //生成js语法
        methodList = new ArrayList<>();
        allFunctions = String.format(getAllFunctions(), clazz.getName()) +
                "\n var MY_UA = JSON.parse(getUaObject());" +
                "\n var MOBILE_UA =  MY_UA.mobileUa;" +
                "\n var PC_UA = MY_UA.pcUa" +
                "\n eval(getJsPlugin())";
        updateTraceLog();
        classloaders = new ArrayList<>();
    }

    public void updateTraceLog() {
        traceLog = BigTextDO.getTraceLog();
    }

    public List<String> getLogs() {
        return logs;
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

    public void parseSearchRes(String url, String res, SearchEngine searchEngine, Map<String, Object> injectMap, OnFindCallBack<List<SearchResult>> searchJsCallBack) {
        parseSearchRes(url, res, searchEngine.toMovieRule(), injectMap, searchJsCallBack);
    }

    private void parseSearchRes(String url, String res, MovieRule movieRule, Map<String, Object> injectMap, OnFindCallBack<List<SearchResult>> searchJsCallBack) {
        String callbackKey = UUIDUtil.genUUID();
        callbackMap.put(callbackKey, searchJsCallBack);
        resCodeMap.put(callbackKey, res);
        if (!movieRule.getSearchFind().startsWith("js:")) {
            searchJsCallBack.showErr(movieRule.getTitle() + "---搜索结果解析失败！请检查规则");
        } else {
            try {
                runScript(getMyCallbackKey(callbackKey)
                        + getMyRule(movieRule)
                        + generateInjectMap(injectMap)
                        + generateMyParams(movieRule.getParams())
                        + getMyUrl(url) + getMyType("search") + getMyJs(movieRule.getSearchFind()), callbackKey);
            } catch (Exception e) {
                Timber.e(e, "parseSearchRes: ");
                setError("运行出错：" + e.toString(), callbackKey, JSON.toJSON(movieRule));
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
            setError("运行出错：" + e.getMessage(), callbackKey, JSON.toJSON(movieRule));
            Timber.e(e, "parseStr: ");
        }
    }

    public void parseHome(String url, String input, ArticleListRule articleListRule, String js, Map<String, Object> injectMap, OnFindCallBack<List<ArticleList>> callBack) {
        String callbackKey = UUIDUtil.genUUID();
        callbackMap.put(callbackKey, callBack);
        resCodeMap.put(callbackKey, input);
        try {
            runScript("\n" + getMyRule(articleListRule)
                    + generateInjectMap(injectMap)
                    + generateMyParams(articleListRule.getParams())
                    + getMyType("home") + getMyCallbackKey(callbackKey) + getMyUrl(url) + getMyJs(js), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.getMessage(), callbackKey, JSON.toJSON(articleListRule));
            Timber.e(e, "parseHome: ");
        }
    }

    public void parsePreRule(ArticleListRule articleListRule) {
        parsePreRule(articleListRule, false);
    }

    public void parsePreRule(ArticleListRule articleListRule, boolean isCustomCatchError) {
        String callbackKey = UUIDUtil.genUUID();
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyType("preHome")
                    + getMyRule(articleListRule)
                    + generateMyParams(articleListRule.getParams())
                    + articleListRule.getPreRule(), callbackKey);
        } catch (Exception e) {
            Timber.e(e, "parsePreRule: ");
            if (!isCustomCatchError) {
                setError("运行出错：" + e.getMessage(), callbackKey, JSON.toJSON(articleListRule));
            } else {
                throw e;
            }
        }
    }

    public void parseLastChapterRule(String url, String input, ArticleListRule articleListRule, String js, OnFindCallBack<String> callBack) {
        String callbackKey = UUIDUtil.genUUID();
        callbackMap.put(callbackKey, callBack);
        resCodeMap.put(callbackKey, input);
        try {
            runScript("\n" + getMyRule(articleListRule)
                    + generateMyParams(articleListRule.getParams())
                    + getMyType("lastChapter") + getMyCallbackKey(callbackKey) + getMyUrl(url) + getMyJs(js), callbackKey, true);
        } catch (Exception e) {
            callBack.showErr(e.getMessage());
            Timber.e(e, "parseLastChapter: ");
        }
    }

    public void parsePreRule(SearchEngine articleListRule) {
        String callbackKey = UUIDUtil.genUUID();
        try {
            runScript(getMyCallbackKey(callbackKey) + getMyType("preEngine") + getMyRule(articleListRule) + articleListRule.getPreRule(), callbackKey);
        } catch (Exception e) {
            setError("运行出错：" + e.toString(), callbackKey, JSON.toJSON(articleListRule));
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
        String ru = jsStr.contains("const my_rule = '") ? "" : getMyRule(null);
        String js = getMyInput(input) + ru + getMyType("eval") + getMyCallbackKey(UUIDUtil.genUUID());
        if (decodeConflict) {
            js = js + StringUtil.decodeConflictStr(jsStr);
        } else {
            js = js + jsStr;
        }
        js = allFunctions + "\n" + getReplaceJS(js);
        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        initRhino(rhino);
        try {
            Scriptable scope = rhino.initStandardObjects();
            ImporterTopLevel.init(rhino, scope, false);
            ScriptableObject.putProperty(scope, "rc4", org.mozilla.javascript.Context.javaToJS(rc4, scope));
            ScriptableObject.putProperty(scope, "_base64", org.mozilla.javascript.Context.javaToJS(myBase64, scope));
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

    private void initRhino(org.mozilla.javascript.Context rhino) {
        rhino.setOptimizationLevel(-1);
        rhino.setLanguageVersion(200);
    }


    /**
     * 执行JS
     *
     * @param js js执行代码 eg: "var v1 = getValue('Ta');setValue(‘key’，v1);"
     */
    private void runScript(String js, String callbackKey) {
        runScript(js, callbackKey, false);
    }

    private void runScript(String js, String callbackKey, boolean isCustomCatchError) {
        String runJSStr = allFunctions + "\n" + getReplaceJS(js);//运行js = allFunctions + js
        org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
        initRhino(rhino);
        try {
            Scriptable scope = rhino.initStandardObjects();
            ImporterTopLevel.init(rhino, scope, false);
            ScriptableObject.putProperty(scope, "rc4", org.mozilla.javascript.Context.javaToJS(rc4, scope));
            ScriptableObject.putProperty(scope, "_base64", org.mozilla.javascript.Context.javaToJS(myBase64, scope));
            ScriptableObject.putProperty(scope, "javaContext", org.mozilla.javascript.Context.javaToJS(this, scope));//配置属性 javaContext:当前类JSEngine的上下文
            ScriptableObject.putProperty(scope, "javaLoader", org.mozilla.javascript.Context.javaToJS(clazz.getClassLoader(), scope));//配置属性 javaLoader:当前类的JSEngine的类加载器
            rhino.evaluateString(scope, runJSStr, clazz.getSimpleName(), 1, null);
        } catch (Exception e) {
            if (!isCustomCatchError) {
                setError("JS编译出错：" + e.getMessage(), callbackKey, new JSONObject());
            } else {
                throw e;
            }
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
        if (url == null) {
            url = "";
        }
        String url0 = Utils.escapeJavaScriptString(url);
        return "var MY_URL = '" + url0 + "';\n" + "var MY_HOME = '" + StringUtil.getHome(url0) + "';\n";
    }

    private String getMyType(String type) {
        return "var MY_TYPE = '" + type + "';\n";
    }

    public static String getMyRule(Object rule) {
        String ruleTitle = "";
        String displayName = null;
        if (rule instanceof ArticleListRule) {
            ruleTitle = ((ArticleListRule) rule).getTitle();
            displayName = ((ArticleListRule) rule).getDisplayName();
        } else if (rule instanceof SearchEngine) {
            ruleTitle = ((SearchEngine) rule).getTitle();
            displayName = ((SearchEngine) rule).getDisplayName();
        } else if (rule instanceof MovieRule) {
            ruleTitle = ((MovieRule) rule).getTitle();
            displayName = ((MovieRule) rule).getDisplayName();
        }
        return "const my_rule = '" + Utils.escapeJavaScriptString(JSON.toJSONString(rule, JSONPreFilter.getSimpleFilter()))
                + "';\n const MY_RULE = JSON.parse(my_rule);\n " +
                (StringUtil.isNotEmpty(displayName) ? "const _displayName = '" + Utils.escapeJavaScriptString(displayName) + "';\n" : "") +
                "const MY_TICKET = '"
                + Utils.escapeJavaScriptString(generateTicket(ruleTitle)) + "';\n"
                + "eval(getJsLazyPlugin());\n";
    }

    private static String generateInjectMap(Map<String, Object> injectMap) {
        String js = "";
        if (injectMap == null) {
            return js;
        }
        for (Map.Entry<String, Object> entry : injectMap.entrySet()) {
            Object value = entry.getValue();
            js = js + "const " + entry.getKey() + " = ";
            if (value instanceof String) {
                js = js + "'" + Utils.escapeJavaScriptString((String) value) + "';\n";
            } else if (value == null) {
                js = js + "null;\n";
            } else if (value instanceof Integer || value instanceof Float || value instanceof Boolean) {
                js = js + value + ";\n";
            } else {
                js = js + "JSON.parse('" + Utils.escapeJavaScriptString(JSON.toJSONString(value)) + "');\n";
            }
        }
        return js;
    }

    private static String generateTicket(String ruleTitle) {
        if (StringUtil.isEmpty(ruleTitle)) {
            return ruleTitle;
        }
        String uuid = UUIDUtil.genUUID();
        ticketCache.put(uuid, ruleTitle);
        return uuid;
    }

    private static String getRuleTitle(String ticket) {
        if (StringUtil.isEmpty(ticket)) {
            return ticket;
        }
        try {
            return ticketCache.get(ticket, () -> ticket);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ticket;
        }
    }

    private String generateMyParams(String params) {
        if (StringUtil.isEmpty(params)) {
            return "var MY_PARAMS = {};\n";
        }
        return "var _my_params = '" + Utils.escapeJavaScriptString(params) + "';\n var MY_PARAMS = JSON.parse(_my_params);\n";
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


    @JSAnnotation(returnType = ReturnType.STRING, alias = "getCode")
    public String getResCode(@Parameter("callbackKey") Object callbackKey) {
        return resCodeMap.get((String) argsNativeObjectAdjust(callbackKey));
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getUrl(@Parameter("urlKey") Object urlKey) {
        return (String) argsNativeObjectAdjust(urlKey);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public Object getParam(@Parameter("key") Object key, @Parameter("defaultValue") Object defaultValue, @Parameter("urlKey") Object urlKey) {
        Map<String, String> paramsMap = HttpParser.getParamsByUrl((String) argsNativeObjectAdjust(urlKey));
        String k = (String) argsNativeObjectAdjust(key);
        if (isUndefined(defaultValue)) {
            defaultValue = Undefined.instance;
        }
        return paramsMap.containsKey(k) ? StringUtil.decodeConflictStr(paramsMap.get(k)) : defaultValue;
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getJsPlugin() {
        if (jsPlugin == null) {
            jsPlugin = FilesInAppUtil.getAssetsString(Application.getContext(), "Hikerurl.js");
        }
        return jsPlugin;
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getJsLazyPlugin() {
        if (jsLazyPlugin == null) {
            jsLazyPlugin = FilesInAppUtil.getAssetsString(Application.getContext(), "plugin.js");
        }
        return jsLazyPlugin;
    }

    @JSAnnotation(returnType = ReturnType.STRING)
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

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getMyVar(@Parameter("o") Object o, @Parameter("defaultVal") Object defaultVal, @Parameter("ruleTitleKey") Object r) {
        String rule = getRuleTitle(getString(r));
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            return "";
        }
        return getVar(rule + "@" + res, defaultVal);
    }

    @JSAnnotation
    public void putMyVar(@Parameter("o") Object o, @Parameter("o2") Object o2, @Parameter("ruleTitleKey") Object r) {
        String rule = getRuleTitle(getString(r));
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            return;
        }
        putVar2(rule + "@" + res, o2);
    }

    @JSAnnotation
    public void clearMyVar(@Parameter("o") Object o, @Parameter("ruleTitleKey") Object r) {
        String rule = getRuleTitle(getString(r));
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            return;
        }
        varMap.remove(rule + "@" + res);
    }

    private boolean isUndefined(Object input) {
        if ("undefined".equals(input)) {
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
        if (oo2 != null && !isUndefined(oo2) && res instanceof String) {
            if (!(oo2 instanceof String)) {
                oo2 = JSON.toJSONString(oo2);
            }
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
    public void clearVar(@Parameter("o") Object o) {
        Object oo1 = argsNativeObjectAdjust(o);
        if (!(oo1 instanceof String)) {
            return;
        }
        String str = (String) oo1;
        varMap.remove(str);
    }

    @JSAnnotation
    public void showLoading(@Parameter("o") Object o) {
        Object oo1 = argsNativeObjectAdjust(o);
        if (!(oo1 instanceof String)) {
            return;
        }
        String str = (String) oo1;
        EventBus.getDefault().post(new LoadingEvent(str, true));
    }

    @JSAnnotation
    public void hideLoading() {
        EventBus.getDefault().post(new LoadingEvent(null, false));
    }

    @JSAnnotation
    public void putVar2(@Parameter("o1") Object o1, @Parameter("o2") Object o2) {
        Object oo1 = argsNativeObjectAdjust(o1);
        Object oo2 = argsNativeObjectAdjust(o2);
        if (!(oo1 instanceof String)) {
            return;
        }
        if (!(oo2 instanceof String)) {
            oo2 = JSON.toJSONString(oo2);
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
    @JSAnnotation(returnType = ReturnType.STRING)
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
    @JSAnnotation(returnType = ReturnType.STRING)
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
    @JSAnnotation(returnType = ReturnType.STRING)
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
    @JSAnnotation(returnType = ReturnType.STRING)
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
    @JSAnnotation(returnType = ReturnType.STRING)
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

    @JSAnnotation(returnType = ReturnType.STRING)
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
    @JSAnnotation(returnType = ReturnType.STRING)
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
     * RSA 加密
     *
     * @param data    要加密的数据
     * @param key     密钥，type 为 1 则公钥，type 为 2 则私钥
     * @param options 加密的选项，包含加密配置和类型：{ config: "RSA/ECB/PKCS1Padding", type: 1, long: 1 }
     *                config 加密的配置，默认 RSA/ECB/PKCS1Padding （可选）
     *                type 加密类型，1 公钥加密 私钥解密，2 私钥加密 公钥解密（可选，默认 1）
     *                long 加密方式，1 普通，2 分段（可选，默认 1）
     *                block 分段长度，false 固定117，true 自动（可选，默认 true ）
     * @return 返回加密结果
     */
    @JSAnnotation(returnType = ReturnType.STRING)
    public String rsaEncrypt(@Parameter("data") Object data, @Parameter("key") Object key, @Parameter("options") Object options) throws Exception {
        Object oData = argsNativeObjectAdjust(data);
        Object oKey = argsNativeObjectAdjust(key);
        if (!(oData instanceof String) || !(oKey instanceof String)) {
            return "";
        }
        int mLong = 1;
        int mType = 1;
        boolean mBlock = true;
        String mConfig = null;
        if (options != null && !isUndefined(options)) {
            Map op = (Map) argsNativeObjectAdjust(options);
            if (op.containsKey("config")) {
                try {
                    mConfig = (String) op.get("config");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.containsKey("type")) {
                try {
                    mType = ((Double) op.get("type")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.containsKey("long")) {
                try {
                    mLong = ((Double) op.get("long")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.containsKey("block")) {
                try {
                    mBlock = (Boolean) op.get("block");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        String mData = (String) oData;
        String mKey = (String) oKey;
        try {
            switch (mType) {
                case 1:
                    if (mConfig != null) {
                        return RSAEncrypt.encryptByPublicKey(mData, mKey, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.encryptByPublicKey(mData, mKey, mLong, mBlock);
                    }
                case 2:
                    if (mConfig != null) {
                        return RSAEncrypt.encryptByPrivateKey(mData, mKey, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.encryptByPrivateKey(mData, mKey, mLong, mBlock);
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * RSA 解密
     *
     * @param encryptBase64Data 加密后的 Base64 字符串
     * @param key               密钥，type 为 1 则私钥，type 为 2 则公钥
     * @param options           解密的选项，包含解密配置和类型：{ config: "RSA/ECB/PKCS1Padding", type: 1, long: 1 }
     *                          config 解密的配置，默认 RSA/ECB/PKCS1Padding （可选）
     *                          type 解密类型，1 公钥加密 私钥解密，2 私钥加密 公钥解密（可选，默认 1）
     *                          long 解密方式，1 普通，2 分段（可选，默认 1）
     *                          block 分段长度，false 固定128，true 自动（可选，默认 true ）
     * @return 返回解密结果
     */
    @JSAnnotation(returnType = ReturnType.STRING)
    public String rsaDecrypt(@Parameter("encryptBase64Data") Object encryptBase64Data, @Parameter("key") Object key, @Parameter("options") Object options) throws Exception {
        Object oData = argsNativeObjectAdjust(encryptBase64Data);
        Object oKey = argsNativeObjectAdjust(key);
        if (!(oData instanceof String) || !(oKey instanceof String)) {
            return "";
        }
        int mLong = 1;
        int mType = 1;
        boolean mBlock = true;
        String mConfig = null;
        if (options != null && !isUndefined(options)) {
            Map op = (Map) argsNativeObjectAdjust(options);
            if (op.containsKey("config")) {
                try {
                    mConfig = (String) op.get("config");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.containsKey("type")) {
                try {
                    mType = ((Double) op.get("type")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.containsKey("long")) {
                try {
                    mLong = ((Double) op.get("long")).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (op.containsKey("block")) {
                try {
                    mBlock = (Boolean) op.get("block");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        String mData = (String) oData;
        String mKey = (String) oKey;
        try {
            switch (mType) {
                case 1:
                    if (mConfig != null) {
                        return RSAEncrypt.decryptByPrivateKey(mData, mKey, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.decryptByPrivateKey(mData, mKey, mLong, mBlock);
                    }
                case 2:
                    if (mConfig != null) {
                        return RSAEncrypt.decryptByPublicKey(mData, mKey, mConfig, mLong, mBlock);
                    } else {
                        return RSAEncrypt.decryptByPublicKey(mData, mKey, mLong, mBlock);
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 供js获取相关信息
     *
     * @return 规则
     */
    @JSAnnotation(returnType = ReturnType.STRING)
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
    @JSAnnotation(alias = "refresh")
    public void refreshPage(@Parameter("scrollTop") Object scrollTop) {
        Object top = argsNativeObjectAdjust(scrollTop);
        boolean toTop = top != null && !isUndefined(top) && top instanceof Boolean ? (Boolean) top : true;
        EventBus.getDefault().post(new OnRefreshPageEvent(toTop));
    }

    @JSAnnotation(alias = "refresh")
    public void toast(@Parameter("str") Object str) throws Exception {
        String msg = (String) argsNativeObjectAdjust(str);
        ThreadTool.INSTANCE.runOnUI(() -> ToastMgr.shortBottomCenter(Application.application, msg));
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
        String movieTitle = ((JSONObject) rule).getString("title");
        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }
        callbackMap.remove(callbackStr);
        if (!(res instanceof String)) {
            onFindCallBack.showErr(movieTitle + "---解析失败！请检查规则");
            return;
        }
        try {
            onFindCallBack.onSuccess((String) res);
        } catch (Exception e) {
            e.printStackTrace();
            onFindCallBack.showErr(movieTitle + "---解析失败！请检查规则");
        }
    }

    @JSAnnotation
    public void log(@Parameter("o") Object o, @Parameter("ruleKey") Object ruleKey) {
        if (!traceLog) {
            return;
        }
        Object res = argsNativeObjectAdjust(o);
        Object rule = argsNativeObjectAdjust(ruleKey);
        String log;
        String time = TimeUtil.formatTime(System.currentTimeMillis(), "HH:mm:ss.SSS");
        String msg;
        if (res instanceof String) {
            msg = (String) res;
        } else {
            msg = JSON.toJSONString(res);
        }
        if (rule == null || isUndefined(rule)) {
            log = String.format("%s: %s", time, msg);
        } else {
            if (rule instanceof JSONObject) {
                String movieTitle = ((JSONObject) rule).getString("title");
                if (StringUtil.isEmpty(movieTitle)) {
                    log = String.format("%s: %s", time, msg);
                } else {
                    log = String.format("%s: %s: %s", time, movieTitle, msg);
                }
            } else {
                log = String.format("%s: %s", time, msg);
            }
        }
        if (logs.size() >= 10000) {
            logs.remove(0);
        }
        logs.add(log);
    }


    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setLastChapterResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey, @Parameter("ruleKey") Object ruleKey) {
        setStrResult(o, callbackKey, ruleKey);
    }

    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void setResult(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey,
                          @Parameter("ruleKey") Object ruleKey, @Parameter("typeKey") Object typeKey) {
        if ("lastChapter".equals(argsNativeObjectAdjust(typeKey))) {
            setLastChapterResult(o, callbackKey, ruleKey);
        } else if ("search".equals(argsNativeObjectAdjust(typeKey))) {
            callbackSearchResult(o, callbackKey, ruleKey, true);
        } else {
            callbackHomeResult(o, callbackKey, ruleKey, true);
        }
    }


    @JSAnnotation(alias = "listen")
    public void addListener(@Parameter("event") Object event, @Parameter("listener") Object listener, @Parameter("callbackKey") Object callbackKey) {
        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        if (onFindCallBack == null) {
            return;
        }
        Object event1 = argsNativeObjectAdjust(event);
        Object listener1 = argsNativeObjectAdjust(listener);
        if (event1 instanceof String && listener1 instanceof String) {
            onFindCallBack.onUpdate((String) event1, (String) listener1);
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


    @JSAnnotation
    public void updateItem(@Parameter("id") Object o1, @Parameter("o") Object o) throws Exception {
        Object res = argsNativeObjectAdjust(o);
        Object res1 = argsNativeObjectAdjust(o1);
        String id = null;
        if (res1 instanceof JSONObject) {
            res = res1;
        } else if (res1 instanceof String) {
            id = (String) res1;
        }
        if (!(res instanceof JSONObject)) {
            throw new Exception("updateItem：格式有误，只支持json");
        }
        ArticleList articleList = convertToArticleList((JSONObject) res);
        ItemModifyEvent event = new ItemModifyEvent(articleList, ItemModifyEvent.Action.UPDATE);
        if (StringUtil.isNotEmpty(id)) {
            event.setAnchorId(id);
            if (StringUtil.isNotEmpty(articleList.getExtra())) {
                //传了extra，然后extra又没传ID，那么把ID设置一下
                if (StringUtil.isEmpty(articleList.getBaseExtra().getId())) {
                    JSONObject jsonObject = JSON.parseObject(articleList.getExtra());
                    jsonObject.put("id", id);
                    articleList.setExtra(jsonObject.toJSONString());
                }
            }
        } else {
            event.setAnchorId(articleList.getBaseExtra().getId());
        }
        EventBus.getDefault().post(event);
    }

    @JSAnnotation
    public void addItemAfter(@Parameter("id") Object id, @Parameter("o") Object o) throws Exception {
        addItem0(id, o, true);
    }

    @JSAnnotation
    public void addItemBefore(@Parameter("id") Object id, @Parameter("o") Object o) throws Exception {
        addItem0(id, o, false);
    }

    private void addItem0(Object id, Object o, boolean after) throws Exception {
        Object res1 = argsNativeObjectAdjust(id);
        if (!(res1 instanceof String)) {
            throw new Exception("addItemAfter：格式有误，请传入ID字符串");
        }
        String id1 = (String) res1;

        Object res = argsNativeObjectAdjust(o);
        if (res instanceof JSONArray) {
            JSONArray array = (JSONArray) res;
            List<ArticleList> articleLists = new ArrayList<>();
            ItemModifyEvent updateEvent = new ItemModifyEvent(articleLists, ItemModifyEvent.Action.ADD);
            for (int i = 0; i < array.size(); i++) {
                if (array.getJSONObject(i) == null) {
                    continue;
                }
                articleLists.add(convertToArticleList(array.getJSONObject(i)));
            }
            updateEvent.setAnchorId(id1);
            updateEvent.setAfter(after);
            EventBus.getDefault().post(updateEvent);
            return;
        } else if (!(res instanceof JSONObject)) {
            throw new Exception("addItem：格式有误，只支持json");
        }
        ArticleList articleList = convertToArticleList((JSONObject) res);
        ItemModifyEvent updateEvent = new ItemModifyEvent(articleList, ItemModifyEvent.Action.ADD);
        updateEvent.setAnchorId(id1);
        updateEvent.setAfter(after);
        EventBus.getDefault().post(updateEvent);
    }

    @JSAnnotation
    public void deleteItem(@Parameter("o") Object o, @Parameter("ruleKey") Object ruleKey) throws Exception {
        Object res = argsNativeObjectAdjust(o);
        if (res instanceof JSONArray) {
            JSONArray array = (JSONArray) res;
            List<ArticleList> articleLists = new ArrayList<>();
            ItemModifyEvent updateEvent = new ItemModifyEvent(articleLists, ItemModifyEvent.Action.DELETE);
            for (int i = 0; i < array.size(); i++) {
                String id = (String) array.get(i);
                ArticleList articleList = new ArticleList();
                BaseExtra baseExtra = new BaseExtra();
                baseExtra.setId(id);
                articleList.setExtra(JSON.toJSONString(baseExtra));
                articleLists.add(articleList);
            }
            EventBus.getDefault().post(updateEvent);
            return;
        } else if (!(res instanceof String)) {
            throw new Exception("deleteItem：格式有误，请传入ID字符串");
        }
        String id = (String) res;
        ArticleList articleList = new ArticleList();
        BaseExtra baseExtra = new BaseExtra();
        baseExtra.setId(id);
        articleList.setExtra(JSON.toJSONString(baseExtra));
        EventBus.getDefault().post(new ItemModifyEvent(articleList, ItemModifyEvent.Action.DELETE));
    }

    @JSAnnotation
    public void deleteItemByCls(@Parameter("o") Object o) throws Exception {
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            throw new Exception("deleteItemByCls：格式有误，请传入cls字符串");
        }
        String cls = (String) res;
        ItemModifyEvent event = new ItemModifyEvent((ArticleList) null, ItemModifyEvent.Action.DELETE);
        event.setCls(cls);
        EventBus.getDefault().post(event);
    }

    @JSAnnotation(returnType = ReturnType.JSON)
    public Object _findItem(@Parameter("o") Object o) throws Exception {
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            throw new Exception("findItem：格式有误，请传入ID字符串");
        }
        String id = (String) res;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ItemFindEvent event = new ItemFindEvent(id, countDownLatch);
        EventBus.getDefault().post(event);
        countDownLatch.await(3, TimeUnit.SECONDS);
        return JSON.toJSONString(event.getArticleList());
    }

    @JSAnnotation(returnType = ReturnType.JSON)
    public Object _findItemsByCls(@Parameter("o") Object o) throws Exception {
        Object res = argsNativeObjectAdjust(o);
        if (!(res instanceof String)) {
            throw new Exception("findItemsByCls：格式有误，请传入cls字符串");
        }
        String cls = (String) res;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ClsItemsFindEvent event = new ClsItemsFindEvent(cls, countDownLatch);
        EventBus.getDefault().post(event);
        countDownLatch.await(3, TimeUnit.SECONDS);
        return JSON.toJSONString(event.getArticleLists());
    }

    private ArticleList convertToArticleList(JSONObject jsonObject) {
        ArticleList searchResult = new ArticleList();
        searchResult.setTitle(jsonObject.getString("title"));
        if (jsonObject.containsKey("img")) {
            searchResult.setPic(jsonObject.getString("img"));
        } else if (jsonObject.containsKey("pic")) {
            searchResult.setPic(jsonObject.getString("pic"));
        } else {
            searchResult.setPic(jsonObject.getString("pic_url"));
        }
        searchResult.setDesc(jsonObject.getString("desc"));
        if (jsonObject.containsKey("content")) {
            searchResult.setContent(jsonObject.getString("content"));
        }
        try {
            searchResult.setUrl(jsonObject.getString("url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonObject.containsKey("extra")) {
            Object extra = jsonObject.get("extra");
            if (extra instanceof String) {
                searchResult.setExtra((String) extra);
            } else if (extra == null) {
                searchResult.setExtra(null);
            } else {
                searchResult.setExtra(JSON.toJSONString(extra));
            }
        }

        if (!TextUtils.isEmpty(jsonObject.getString("col_type"))) {
            searchResult.setType(jsonObject.getString("col_type"));
        }
        return searchResult;
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
                    JSONObject jsonObject = array.getJSONObject(i);
                    if (jsonObject == null) {
                        continue;
                    }
                    results.add(convertToArticleList(jsonObject));
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
        JSONObject jsonRule = (JSONObject) rule;
        String movieTitle = jsonRule.getString("title");
        if (jsonRule.containsKey("displayName") && StringUtil.isNotEmpty(jsonRule.getString("displayName"))) {
            movieTitle = jsonRule.getString("displayName");
        }

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
                    JSONObject jsonObject = array.getJSONObject(i);
                    SearchResult searchResult = new SearchResult();
                    searchResult.setTitle(jsonObject.getString("title"));
                    searchResult.setUrl(jsonObject.getString("url"));
                    searchResult.setDesc(movieTitle);
                    if (jsonObject.containsKey("desc")) {
                        searchResult.setDescMore(jsonObject.getString("desc"));
                    }
                    if (jsonObject.containsKey("content")) {
                        searchResult.setContent(jsonObject.getString("content"));
                    }
                    if (jsonObject.containsKey("img")) {
                        searchResult.setImg(jsonObject.getString("img"));
                    } else if (jsonObject.containsKey("pic")) {
                        searchResult.setImg(jsonObject.getString("pic"));
                    } else if (jsonObject.containsKey("pic_url")) {
                        searchResult.setImg(jsonObject.getString("pic_url"));
                    }

                    if (jsonObject.containsKey("extra")) {
                        Object extra = jsonObject.get("extra");
                        if (extra instanceof String) {
                            searchResult.setExtra((String) extra);
                        } else if (extra == null) {
                            searchResult.setExtra(null);
                        } else {
                            searchResult.setExtra(JSON.toJSONString(extra));
                        }
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
    @JSAnnotation(alias = "error")
    public void setError(@Parameter("o") Object o, @Parameter("callbackKey") Object callbackKey,
                         @Parameter("ruleKey") Object ruleKey) {
        Object res = argsNativeObjectAdjust(o);
        String re = null;
        if (!(res instanceof String)) {
//            if ("org.mozilla.javascript.NativeError".equals(res.getClass().getName())) {
//                re = res.toString();
//            } else
            if (res instanceof JavaScriptException) {
                JavaScriptException rhinoException = (JavaScriptException) res;
                re = "\n来源：" + rhinoException.sourceName() + "\n" +
                        "代码：" + rhinoException.lineSource() + "\n" +
                        "行数：" + rhinoException.lineNumber() + "\n" +
                        "详情：" + res.toString();
            } else if (res instanceof RhinoException) {
                RhinoException rhinoException = (RhinoException) res;
                re = "\n来源：" + rhinoException.sourceName() + "\n" +
                        "代码：" + rhinoException.lineSource() + "\n" +
                        "行数：" + rhinoException.lineNumber() + "\n" +
                        "详情：" + res.toString();
            } else if (res instanceof IdScriptableObject) {
                IdScriptableObject scriptableObject = (IdScriptableObject) res;
                Object fileName = scriptableObject.get("fileName");
                Object lineNumber = scriptableObject.get("lineNumber");
                Object lineSource = scriptableObject.get("lineSource");
                re = "\n来源：" + fileName + "\n" +
                        "代码：" + lineSource + "\n" +
                        "行数：" + lineNumber + "\n" +
                        "详情：" + res.toString();
            } else if (res instanceof Exception) {
                re = ((Exception) res).getMessage();
            } else {
                String tos = res.toString();
                if (StringUtil.isNotEmpty(tos) && !tos.startsWith(res.getClass().getName())) {
                    //应该重写了toString方法
                    re = res.toString();
                } else {
                    re = JSON.toJSONString(res);
                }
            }
        } else {
            re = (String) res;
        }
        if (re != null && re.length() > 2000) {
            re = re.substring(0, 2000) + "...文本过长被截断";
        }
        Object rule = argsNativeObjectAdjust(ruleKey);
        Timber.d("setError: %s", re);

        String callbackStr = (String) argsNativeObjectAdjust(callbackKey);
        OnFindCallBack onFindCallBack = callbackMap.get(callbackStr);
        String msg = "解析失败！";
//        log(re, ruleKey);
        if (rule instanceof JSONObject) {
            String ruleTitle = ((JSONObject) rule).getString("title");
            if (StringUtil.isNotEmpty(ruleTitle)) {
                msg = ruleTitle + msg;
            }
        }
        msg = msg + re;
        if (onFindCallBack == null) {
            EventBus.getDefault().post(new ToastEvent(msg));
            return;
        }
        callbackMap.remove(callbackStr);

        onFindCallBack.showErr(msg);
    }


    /**
     * 供js回调
     *
     * @param o 要回调的结果
     */
    @JSAnnotation
    public void copy(@Parameter("o") Object o) {
        String text = (String) argsNativeObjectAdjust(o);
        if (StringUtil.isNotEmpty(text) && !"undefined".equalsIgnoreCase(text)) {
            EventBus.getDefault().post(new CopyEvent(text));
        }
    }

    @JSAnnotation
    public void saveImage(@Parameter("url") String o2, @Parameter("path") Object o1) {
        String path = (String) argsNativeObjectAdjust(o1);
        String urls = (String) argsNativeObjectAdjust(o2);
        if (StringUtil.isNotEmpty(path) && !"undefined".equalsIgnoreCase(path)
                && StringUtil.isNotEmpty(urls) && !"undefined".equalsIgnoreCase(urls)) {
            path = FileUtil.getFilePath(path);
            if (path != null) {

                ImgUtil.downloadImgByGlide(Application.application, urls, path);
            }
        }
    }

    @JSAnnotation(returnType = ReturnType.BOOL, alias = "exist")
    public String fileExist(@Parameter("path") Object o1, @Parameter("ruleTitleKey") Object r) {
        String path = (String) argsNativeObjectAdjust(o1);
        if (StringUtil.isNotEmpty(path) && !"undefined".equalsIgnoreCase(path)) {
            String rule = getRuleTitle(getString(r));
            if (StringUtil.isNotEmpty(rule) && !path.contains(File.separator)) {
                path = getFilesDir() + rule + File.separator + path;
                File file = new File(path);
                if (file.exists()) {
                    return "true";
                } else {
                    return "false";
                }
            }
            path = FileUtil.getExistFilePath(path);
            if (path != null) {
                return "true";
            }
        }
        return "false";
    }

    private Object fetchByHiker(String url, Object rule, boolean toHex) {
        if (url.startsWith("hiker://files/") || url.startsWith("file://")) {
            String filePath = getFilePath(url);
            File file = new File(filePath);
            if (!file.exists()) {
                return "";
            }
            if (toHex) {
                byte[] bytes = FileUtil.fileToBytes(filePath);
                return StringUtil.bytesToHexString(bytes);
            }
            return fileToString(file, null);
        } else if (url.startsWith("hiker://page/")) {
            try {
                String r = JSON.toJSONString(rule);
                ArticleListRule articleListRule = null;
                try {
                    if (r.startsWith("[") && r.endsWith("]")) {
                        articleListRule = JSON.parseArray(r, ArticleListRule.class).get(0);
                    } else {
                        articleListRule = JSON.parseObject(r, ArticleListRule.class);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ArticleListPageRule pageRule = PageParser.parsePageRule(articleListRule, url);
                return JSON.toJSONString(pageRule);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return "";
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

    @JSAnnotation(returnType = ReturnType.JSON, alias = "bf")
    public String batchFetch(@Parameter("params") Object params, @Parameter("threadNum") Object threadNum, @Parameter("ruleKey") Object ruleKey) {
        Object res = argsNativeObjectAdjust(params);
        if (!(res instanceof JSONArray)) {
            return "[]";
        }
        JSONArray jsonArray = (JSONArray) res;
        return JSON.toJSONString(batchExecute(jsonArray, ruleKey, (index, url, options, ruleKey1) ->
                fetch(url, options, ruleKey1)));
    }

    @JSAnnotation(returnType = ReturnType.VOID, alias = "be")
    public void batchExecute(@Parameter("tasks") Object task, @Parameter("listener0") Object listener0,
                             @Parameter(value = "success", defaultInt = 0) Object success,
                             @Parameter("ruleKey") Object ruleKey) {
        JSONArray tasks = (JSONArray) argsNativeObjectAdjust(task, true);
        JSONObject listener = listener0 == null || isUndefined(listener0) ? null : (JSONObject) argsNativeObjectAdjust(listener0, true);

        BaseFunction listenerFunc = listener != null && listener.containsKey("func") ? (BaseFunction) listener.get("func") : null;
        Object listenerParam = listener == null ? null : listener.get("param");

        Context context = Context.getCurrentContext();
        int maxThread = Math.min(tasks.size(), 16);
        int successCount = tasks.size();
        Object oo1 = argsNativeObjectAdjust(success);
        if (oo1 instanceof Integer) {
            successCount = (Integer) oo1;
        } else if (oo1 instanceof Double) {
            successCount = ((Double) oo1).intValue();
        }
        if (successCount <= 0 || successCount > tasks.size()) {
            successCount = tasks.size();
        }
        ExecutorService jsExecutorService = new ThreadPoolExecutor(maxThread, maxThread,
                1L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(4096));
        CountDownLatch countDownLatch = new CountDownLatch(tasks.size());
        AtomicInteger counter = new AtomicInteger(0);
        String tag = UUIDUtil.genUUID();
        for (int i = 0; i < tasks.size(); i++) {
            JSONObject jsonObject = (JSONObject) (tasks.get(i));
            int finalSuccessCount = successCount;
            jsExecutorService.execute(() -> {
                if (countDownLatch.getCount() > 0 && counter.get() >= finalSuccessCount) {
                    countDownLatch.countDown();
                    return;
                }
                HttpHelper.addTagForThread(tag);
                Context ctx = context.getFactory().enterContext();
                initRhino(ctx);
                long start = System.currentTimeMillis();
                String id = jsonObject.getString("id");
                String error = null;
                Object result = null;
                try {
                    BaseFunction func = (BaseFunction) jsonObject.get("func");
                    Object param = jsonObject.get("param");
                    Scriptable scope = func.getParentScope();
                    result = func.call(ctx, scope, scope, new Object[]{param});
                    counter.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    error = e.getMessage();
                } finally {
                    synchronized (countDownLatch) {
                        try {
                            if (listenerFunc != null && counter.get() <= finalSuccessCount) {
                                Timber.d("js task end, used call listener");
                                Scriptable listenerScope = listenerFunc.getParentScope();
                                Object res = listenerFunc.call(ctx, listenerScope, listenerFunc, new Object[]{listenerParam, id, error, result});
                                if (res != null && !isUndefined(res)) {
                                    Object oo2 = argsNativeObjectAdjust(res);
                                    //用户主动返回了break表示已经获取到想要的结果了
                                    if ("break".equals(oo2)) {
                                        counter.getAndSet(finalSuccessCount);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            if (countDownLatch.getCount() > 0) {
                                countDownLatch.countDown();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Timber.d("js task end, used " + (System.currentTimeMillis() - start) + "毫秒");
                        try {
                            if (Context.getCurrentContext() != null) {
                                Context.exit();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                        try {
                            if (countDownLatch.getCount() > 0 && counter.get() >= finalSuccessCount) {
                                for (long l = 0; l < countDownLatch.getCount(); l++) {
                                    countDownLatch.countDown();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        try {
            countDownLatch.await(tasks.size() / 16 * 30 + 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        HttpHelper.cancelByTag(tag);
        try {
            if (!jsExecutorService.isShutdown() && !jsExecutorService.isTerminated()) {
                jsExecutorService.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JSAnnotation
    public void confirm(@Parameter("ev") Object o) {
        Object obj = argsNativeObjectAdjust(o);
        if (obj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) obj;
            ConfirmEvent event = jsonObject.toJavaObject(ConfirmEvent.class);
            EventBus.getDefault().post(event);
        }
    }

    public List<String> getMethodList() {
        return methodList;
    }

    private interface UrlTaskExecutor {
        String execute(int index, String url, Object options, Object ruleKey);
    }

    private List<String> batchExecute(JSONArray jsonArray, Object ruleKey, UrlTaskExecutor executor) {
        Map<Integer, String> indexMap = new ConcurrentHashMap<>();
        int maxThread = Math.min(jsonArray.size(), 16);
        ExecutorService jsExecutorService = new ThreadPoolExecutor(maxThread, maxThread,
                1L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(4096));
        CountDownLatch countDownLatch = new CountDownLatch(jsonArray.size());
        String tag = UUIDUtil.genUUID();
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
                HttpHelper.addTagForThread(tag);
                long start = System.currentTimeMillis();
                try {
                    String s = executor.execute(finalI, url, options, ruleKey);
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
        HttpHelper.cancelByTag(tag);
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

    @JSAnnotation(returnType = ReturnType.STRING)
    public String request(@Parameter("url") String url, @Parameter("options") Object options, @Parameter("ruleKey") Object ruleKey) {
        Object op = generateRequestOptions(options);
        return fetch(url, op, ruleKey);
    }

    private Object generateRequestOptions(Object options) {
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
            return op;
        } else {
            Map op = new HashMap<>();
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put(HttpHeaders.HEAD_KEY_USER_AGENT, UAEnum.MOBILE.getContent());
            op.put("headers", headerMap);
            return op;
        }
    }

    private Object generateHeadersOptions(Object options) {
        if (options != null && !isUndefined(options)) {
            Map op = (Map) argsNativeObjectAdjust(options);
            op.put("withHeaders", true);
            return op;
        } else {
            Map op = new HashMap<>();
            op.put("withHeaders", true);
            return op;
        }
    }

    public Object generateRangeHeadersOptions(Object options, int range) {
        if (options != null && !isUndefined(options)) {
            Map op = (Map) argsNativeObjectAdjust(options);
            Map<String, String> headerMap = (Map<String, String>) op.get("headers");
            if (headerMap == null) {
                headerMap = (Map<String, String>) op.get("header");
            }
            if (headerMap == null) {
                headerMap = new HashMap<>();
                headerMap.put("Range", "bytes=0-" + range);
                op.put("headers", headerMap);
            } else {
                headerMap.put("Range", "bytes=0-" + range);
            }
            return op;
        } else {
            Map op = new HashMap<>();
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Range", "bytes=0-" + range);
            op.put("headers", headerMap);
            return op;
        }
    }

    private Object generateOnlyHeadersOptions(Object options) {
        if (options != null && !isUndefined(options)) {
            Map op = (Map) argsNativeObjectAdjust(options);
            op.put("onlyHeaders", true);
            return op;
        } else {
            Map op = new HashMap<>();
            op.put("onlyHeaders", true);
            return op;
        }
    }

    public String fetchWithHeaders(String url, Object options, Object ruleKey) {
        return fetch(url, generateHeadersOptions(options), ruleKey);
    }

    public String fetchOnlyHeaders(String url, Object options, Object ruleKey) {
        return fetch(url, generateOnlyHeadersOptions(options), ruleKey);
    }

    public String fetchWithHeadersInterceptor(String url, Object options, Object ruleKey, HttpHelper.HeadersInterceptor interceptor) {
        return fetch0(url, generateHeadersOptions(options), ruleKey, interceptor);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String cacheM3u8(@Parameter("url") String url, @Parameter("options") Object options,
                            @Parameter(value = "fileName", defaultValue = "video.m3u8") String fileName,
                            @Parameter("ruleKey") Object ruleKey) {
        //不加##可能导致进度记忆有问题，开发者可以自行切掉，但是必须在extra里面加id字段
        fileName = StringUtil.isEmpty(fileName) ? "video.m3u8" : fileName;
        String file = M3u8Utils.INSTANCE.downloadM3u8(url, fileName, options, ruleKey);
        return StringUtils.equals(file, url) ? url : file + "##" + url;
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String fixM3u8(@Parameter("url") String url, @Parameter("content") String content) {
        return M3u8Utils.INSTANCE.fixPath(content, url, s -> content);
    }

    @JSAnnotation(returnType = ReturnType.JSON, alias = "bcm")
    public String batchCacheM3u8(@Parameter("params") Object params, @Parameter("ruleKey") Object ruleKey) {
        Object res = argsNativeObjectAdjust(params);
        if (!(res instanceof JSONArray)) {
            return "[]";
        }
        JSONArray jsonArray = (JSONArray) res;
        return JSON.toJSONString(batchExecute(jsonArray, ruleKey, (index, url, options, ruleKey1) -> {
            String file = M3u8Utils.INSTANCE.downloadM3u8(url, "video" + index + ".m3u8", options, ruleKey1);
            return StringUtils.equals(file, url) ? url : file + "##" + url;
        }));
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String post(String url, Object options, Object ruleKey) {
        return post0(url, options, ruleKey, false);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String postRequest(String url, Object options, Object ruleKey) {
        return post0(url, options, ruleKey, true);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String buildUrl(String url, Object options) {
        String body = HttpHelper.buildParamStr((JSONObject) argsNativeObjectAdjust(options));
        if (url == null || url.isEmpty() || isUndefined(url)) {
            return body;
        } else if (url.contains("?")) {
            return url + "&" + body;
        }
        return url + "?" + body;
    }

    private String post0(String url, Object options, Object ruleKey, boolean request) {
        options = argsNativeObjectAdjust(options);
        if (isUndefined(options)) {
            options = null;
        }
        Map<String, Object> op = null;
        if (options != null) {
            op = HttpHelper.generatePostOps((Map<String, Object>) options);
        }
        if (request) {
            return fetch(url, generateRequestOptions(op), ruleKey);
        }
        return fetch(url, op, ruleKey);
    }

    /**
     * 供js使用fetch
     * 参考自https://github.com/mabDc/MyBookshelf/blob/master/app/src/main/java/com/kunfei/bookshelf/model/analyzeRule/AnalyzeRule.java
     *
     * @return 源码
     */
    @JSAnnotation(returnType = ReturnType.STRING)
    public String fetch(@Parameter("url") String url, @Parameter("options") Object options, @Parameter("ruleKey") Object ruleKey) {
        return fetch0(url, options, ruleKey, null);
    }

    /**
     * @param url
     * @param options
     * @param ruleKey
     * @param headersInterceptor 如果被拦截则body为空
     * @return
     */
    private String fetch0(@Parameter("url") String url, @Parameter("options") Object options,
                          @Parameter("ruleKey") Object ruleKey, HttpHelper.HeadersInterceptor headersInterceptor) {
        try {
            if (StringUtil.isEmpty(url)) {
                return "";
            }
            Map<String, Object> op = options == null || isUndefined(options) ? null : (Map<String, Object>) argsNativeObjectAdjust(options);
            return HttpHelper.fetch(url, op, headersInterceptor, (path, toHex) -> {
                Object rule = argsNativeObjectAdjust(ruleKey);
                return fetchByHiker(path, rule, toHex);
            });
        } catch (Throwable e) {
            Timber.e(e);
            return "";
        }
    }

    /**
     * 供js使用fetch
     * 参考自https://github.com/mabDc/MyBookshelf/blob/master/app/src/main/java/com/kunfei/bookshelf/model/analyzeRule/AnalyzeRule.java
     *
     * @return 源码
     */
    @JSAnnotation(returnType = ReturnType.STRING)
    public String fetchCookie(@Parameter("url") String url, @Parameter("options") Object options, @Parameter("ruleKey") Object ruleKey) {
        try {
            Map op;
            if (options == null || isUndefined(options)) {
                op = new HashMap<>();
            } else {
                op = (Map) argsNativeObjectAdjust(options);
            }
            op.put("withHeaders", true);
            String result = fetch(url, op, ruleKey);
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

    @JSAnnotation(returnType = ReturnType.STRING, alias = "fcbw")
    public String fetchCodeByWebView(@Parameter("url") String url, @Parameter("options") Object options) {
        Map<String, Object> op = options == null || isUndefined(options) ? null : (Map<String, Object>) argsNativeObjectAdjust(options);
        CountDownLatch lock = new CountDownLatch(1);
        WebkitFetcher fetcher = new WebkitFetcher();
        HttpHelper.FetchResponse response = new HttpHelper.FetchResponse();
        response.fetchResult = "";
        fetcher.fetch(url, op, s -> {
            response.fetchResult = s;
            lock.countDown();
        });
        try {
            int timeout = -1;
            if (op != null && op.containsKey("timeout")) {
                try {
                    timeout = Integer.parseInt(JSON.toJSONString(op.get("timeout")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (timeout >= 1000 && timeout <= 30000) {
                lock.await(timeout, TimeUnit.MILLISECONDS);
            } else {
                lock.await(30, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            fetcher.destroy();
        }
        return response.fetchResult;
    }

    @JSAnnotation
    public void writeFile(@Parameter("filePath") String filePath, @Parameter("content") String content) {
        stringToFile(content, getFilePath(filePath), null);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getPath(@Parameter("filePath") String filePath) {
        return "file://" + getFilePath(filePath);
    }

    public static String getFilePath(String filePath) {
        if (filePath.startsWith("hiker://files/")) {
            String fileName = filePath.replace("hiker://files/", "");
            return UriUtils.getRootDir(Application.application.getApplicationContext()) + File.separator + fileName;
        } else if (filePath.startsWith("file://")) {
            return filePath.replace("file://", "");
        }
        return filePath;
    }

    @JSAnnotation
    public void saveFile(@Parameter("filePath") String filePath, @Parameter("content") String content, @Parameter("ruleTitleKey") Object r) {
        String rule = getRuleTitle(getString(r));
        if (filePath.startsWith("hiker://files/")) {
            stringToFile(content, getFilePath(filePath), null);
            return;
        }
        filePath = filePath.split(File.separator)[filePath.split(File.separator).length - 1];
        filePath = getFilesDir() + rule + File.separator + filePath;
        stringToFile(content, filePath, rule);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String readFile(@Parameter("filePath") String filePath, @Parameter("ruleTitleKey") Object r) {
        String rule = getRuleTitle(getString(r));
        if (filePath.contains(File.separator)) {
            //公开读
            return fileToString(new File(getFilePath(filePath)), null);
        }
        //私有读
        filePath = filePath.split(File.separator)[filePath.split(File.separator).length - 1];
        filePath = getFilesDir() + rule + File.separator + filePath;
        return fileToString(new File(filePath), rule);
    }

    @JSAnnotation
    public void deleteFile(@Parameter("filePath") String filePath, @Parameter("ruleTitleKey") Object r) {
        String rule = getRuleTitle(getString(r));
        if (filePath.contains(File.separator)) {
            //公开读
            File file = new File(getFilePath(filePath));
            if (file.exists()) {
                file.delete();
            }
        }
        //私有读
        filePath = filePath.split(File.separator)[filePath.split(File.separator).length - 1];
        filePath = getFilesDir() + rule + File.separator + filePath;
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private String fileToString(File file, String rule) {
        if (file.exists()) {
            String path = file.getAbsolutePath();
            if (path.endsWith(".zip") || path.endsWith(".db") || path.endsWith(".apk")
                    || path.endsWith(".mp4") || path.endsWith(".png") || path.endsWith(".jpg")
                    || path.contains(File.separator + "backup" + File.separator)) {
                return "";
            }
            if (StringUtil.isNotEmpty(rule)) {
                String key = getItemNow("i--", "", rule);
                if (StringUtil.isNotEmpty(key)) {
                    return aesDecode(key, FileUtil.fileToString(path));
                }
            } else {
                String filesDir = getFilesDir();
                if (path.contains(filesDir)) {
                    return "";
                }
            }
            return FileUtil.fileToString(path);
        } else {
            return "";
        }
    }

    private void stringToFile(String content, String path, String rule) {
        try {
            if (path.endsWith(".zip") || path.endsWith(".db") || path.endsWith(".apk")
                    || path.endsWith(".mp4") || path.endsWith(".png") || path.endsWith(".jpg")
                    || path.contains(File.separator + "backup" + File.separator)) {
                return;
            }
            if (StringUtil.isNotEmpty(rule)) {
                String defaultKey = String.valueOf(System.currentTimeMillis());
                String key = getItemNow("i--", defaultKey, rule);
                if (StringUtil.isNotEmpty(key)) {
                    if (defaultKey.equals(key)) {
                        setItemNow("i--", defaultKey, rule);
                    }

                    FileUtil.stringToFile(aesEncode(key, content), path);
                    return;
                }
            } else {
                String filesDir = getFilesDir();
                if (path.contains(filesDir) || inJsDir(path)) {
                    //私有文件目录或者JS插件目录且文件名包含点的不允许写入
                    return;
                }
            }
            FileUtil.stringToFile(content, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean inJsDir(String path) {
        if (!path.contains(getJsDir())) {
            return false;
        }
        String fileName = FileUtil.getName(new File(path).getName());
        return fileName.contains(".") || fileName.startsWith("global");
    }

    public static String getFilesDir() {
        return UriUtils.getRootDir(Application.application.getApplicationContext()) + File.separator
                + "rules" + File.separator + "files" + File.separator;
    }

    private static String getJsDir() {
        return UriUtils.getRootDir(Application.application.getApplicationContext()) + File.separator
                + "rules" + File.separator + "js" + File.separator;
    }

    @JSAnnotation
    public void setPageTitle(@Parameter("title") String o) {
        EventBus.getDefault().post(new SetPageTitleEvent(o));
    }

    @JSAnnotation
    public void setPagePicUrl(@Parameter("title") String o) {
        EventBus.getDefault().post(new SetPagePicEvent(o));
    }

    @JSAnnotation
    public void setLastChapterRule(@Parameter("rule") String o) {
        EventBus.getDefault().post(new SetPageLastChapterRuleEvent(o));
    }

    /**
     * 解析dom
     *
     * @return
     */
    @JSAnnotation(returnType = ReturnType.STRING, alias = "pdfh")
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
    @JSAnnotation(returnType = ReturnType.JSON, alias = "pdfa")
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
    @JSAnnotation(returnType = ReturnType.STRING, alias = "pd")
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

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getUaObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mobileUa", UAEnum.MOBILE.getContent());
        jsonObject.put("pcUa", UAEnum.PC.getContent());
        return jsonObject.toJSONString();
    }

    @JSAnnotation(returnType = ReturnType.BOOL)
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

    @JSAnnotation(returnType = ReturnType.JSON)
    public String getHomeSub() {
        List<SubscribeRecord> records = HomeRulesSubService.getSubRecords();
        return JSON.toJSONString(records);
    }


    @JSAnnotation(returnType = ReturnType.JSON)
    public String getPastes() {
        List<String> pastes = RuleImporterManager.getSyncableImporters();
        return JSON.toJSONString(pastes);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String parsePaste(@Parameter("url") Object url) {
        return RuleImporterManager.parseSync(getString(url));
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String sharePaste(@Parameter("content") Object c, @Parameter("paste") Object p) {
        String paste = getString(p);
        if (StringUtil.isEmpty(paste) || "null".equals(paste) || isUndefined(p)) {
            List<String> pastes = RuleImporterManager.getSyncableImporters();
            paste = pastes.get(0);
        }
        return RuleImporterManager.shareSync(getString(c), paste);
    }

    @JSAnnotation(returnType = ReturnType.JSON)
    public String getLastRules(@Parameter("c") Object c) {
        int count;
        if (c == null || isUndefined(c)) {
            count = 12;
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
                if (rules.get(i).getLastUseTime() <= 0 || SettingConfig.homeName.equals(rules.get(i).getTitle())) {
                    continue;
                }
                result.add(rules.get(i));
            }
        }
        return JSON.toJSONString(result, JSONPreFilter.getShareFilter());
    }


    @JSAnnotation(returnType = ReturnType.STRING)
    public String getRuleCount() {
        return String.valueOf(LitePal.count(ArticleListRule.class));
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getItem(@Parameter("key") Object k1, @Parameter("def") Object def, @Parameter("ruleTitleKey") Object rule) {
        String key = getString(k1);
        String defaultValue = getString(def);
        String r = getRuleTitle(getString(rule));
        return getItemNow(key, defaultValue, r);
    }

    private String getItemNow(String key, String defaultValue, String r) {
        if (StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(r)) {
            ArticleListRule articleListRule = LitePal.where("title = ?", r).limit(1).findFirst(ArticleListRule.class);
            return getItemNow(key, defaultValue, articleListRule);
        }
        return defaultValue;
    }

    private String getItemNow(String key, String defaultValue, ArticleListRule articleListRule) {
        if (articleListRule != null) {
            String storage = articleListRule.getStorage();
            if (StringUtil.isEmpty(storage)) {
                return defaultValue;
            }
            JSONObject jsonObject = JSON.parseObject(storage);
            if (jsonObject != null && jsonObject.containsKey(key)) {
                return jsonObject.getString(key);
            }
        } else {
            return "";
        }
        return defaultValue;
    }

    @JSAnnotation
    public void clearItem(@Parameter("key") Object k1, @Parameter("ruleTitleKey") Object rule) {
        String key = getString(k1);
        String r = getRuleTitle(getString(rule));
        if (StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(r)) {
            ArticleListRule articleListRule = LitePal.where("title = ?", r).limit(1).findFirst(ArticleListRule.class);
            if (articleListRule != null) {
                String storage = articleListRule.getStorage();
                JSONObject jsonObject;
                if (StringUtil.isEmpty(storage)) {
                    return;
                } else {
                    jsonObject = JSON.parseObject(storage);
                }
                jsonObject.remove(key);
                articleListRule.setStorage(jsonObject.toJSONString());
                articleListRule.save();
                EventBus.getDefault().post(new RuleModifiedEvent(articleListRule));
            }
        }
    }

    @JSAnnotation
    public void setItem(@Parameter("key") Object k1, @Parameter("v") Object v, @Parameter("ruleTitleKey") Object rule) throws Exception {
        String key = tryGetString(k1);
        String value = tryGetString(v);
        String r = getRuleTitle(getString(rule));
        setItemNow(key, value, r);
    }

    private void setItemNow(String key, String value, String r) {
        if (StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(r)) {
            ArticleListRule articleListRule = LitePal.where("title = ?", r).limit(1).findFirst(ArticleListRule.class);
            setItemNow(key, value, articleListRule);
        }
    }

    private void setItemNow(String key, String value, ArticleListRule articleListRule) {
        if (articleListRule != null) {
            String storage = articleListRule.getStorage();
            JSONObject jsonObject;
            if (StringUtil.isEmpty(storage)) {
                jsonObject = new JSONObject();
            } else {
                jsonObject = JSON.parseObject(storage);
            }
            jsonObject.put(key, value);
            articleListRule.setStorage(jsonObject.toJSONString());
            articleListRule.save();
            EventBus.getDefault().post(new RuleModifiedEvent(articleListRule));
        }
    }

    @JSAnnotation(returnType = ReturnType.Num)
    public String getAppVersion() {
        int myVersion = 0;

        try {
            PackageManager packageManager = Application.getContext().getPackageManager();
            String packageName = Application.getContext().getPackageName();
            myVersion = packageManager.getPackageInfo(packageName, 0).versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(myVersion);
    }

    @JSAnnotation(returnType = ReturnType.JSON)
    public String getColTypes() {
        return JSON.toJSONString(ArticleColTypeEnum.getCodeArray());
    }

    @JSAnnotation(returnType = ReturnType.BOOL)
    public String isLogin() {
        return Boolean.TRUE.toString();
    }

    @JSAnnotation(returnType = ReturnType.VOID)
    public void requireDownload(@Parameter("u") String url, @Parameter("p") String path, @Parameter("headers") Object headers) throws Exception {
        File file = new File(getFilePath(path));
        if (file.exists()) {
            return;
        }
        downloadFile(url, path, headers);
    }

    @JSAnnotation(returnType = ReturnType.VOID)
    public void downloadFile(@Parameter("u") String url, @Parameter("p") String path, @Parameter("headers") Object headers) throws Exception {
        path = getFilePath(path);
        Map<String, String> op = headers == null || isUndefined(headers) ? null : (Map<String, String>) argsNativeObjectAdjust(headers);
        try {
            CodeUtil.downloadSync(url, path, op);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("下载文件失败：" + e.getMessage());
        }
    }

    /**
     * 获取授权
     *
     * @param rule
     * @param method
     * @return
     * @throws Exception
     */
    private AuthStorageItem getAuth(String rule, String method) throws Exception {
        ArticleListRule articleListRule = LitePal.where("title = ?", rule).limit(1).findFirst(ArticleListRule.class);
        if (articleListRule == null) {
            throw new Exception("获取规则失败");
        }
        String defaultKey = String.valueOf(System.currentTimeMillis());
        String key = getItemNow("i--", defaultKey, articleListRule);
        if (defaultKey.equals(key)) {
            setItemNow("i--", defaultKey, rule);
        }
        String auth = getItemNow("_auth", "", articleListRule);
        if (StringUtil.isEmpty(auth)) {
            return new AuthStorageItem(method, AuthBridgeEvent.AuthResult.NONE);
        }
        auth = aesDecode(key, auth);
        List<AuthStorageItem> items = JSON.parseArray(auth, AuthStorageItem.class);
        if (CollectionUtil.isNotEmpty(items)) {
            for (AuthStorageItem item : items) {
                if (method.equals(item.getMethod())) {
                    return item;
                }
            }
        }
        return new AuthStorageItem(method, AuthBridgeEvent.AuthResult.NONE);
    }

    public void updateAuth(String rule, String method, AuthBridgeEvent.AuthResult result) {
        ArticleListRule articleListRule = LitePal.where("title = ?", rule).limit(1).findFirst(ArticleListRule.class);
        if (articleListRule == null) {
            return;
        }
        String defaultKey = String.valueOf(System.currentTimeMillis());
        String key = getItemNow("i--", defaultKey, articleListRule);
        if (defaultKey.equals(key)) {
            setItemNow("i--", defaultKey, rule);
        }
        String auth = getItemNow("_auth", "", articleListRule);
        if (StringUtil.isNotEmpty(auth)) {
            auth = aesDecode(key, auth);
            List<AuthStorageItem> items = JSON.parseArray(auth, AuthStorageItem.class);
            if (CollectionUtil.isNotEmpty(items)) {
                for (AuthStorageItem item : items) {
                    if (method.equals(item.getMethod())) {
                        item.setResult(result);
                        auth = aesEncode(key, JSON.toJSONString(items));
                        setItemNow("_auth", auth, articleListRule);
                        return;
                    }
                }
                AuthStorageItem item = new AuthStorageItem(method, result);
                items.add(item);
                auth = aesEncode(key, JSON.toJSONString(items));
                setItemNow("_auth", auth, articleListRule);
                return;
            }
        }
        AuthStorageItem item = new AuthStorageItem(method, result);
        auth = aesEncode(key, JSON.toJSONString(new ArrayList<>(Collections.singletonList(item))));
        setItemNow("_auth", auth, articleListRule);
    }

    @JSAnnotation(returnType = ReturnType.OBJECT)
    public Object loadJavaClass(@Parameter("p") String path, @Parameter("c") String className, @Parameter("so") String so,
                                @Parameter("ruleTitleKey") Object r) throws Exception {
        if (StringUtil.isEmpty(path) || path.startsWith("http")) {
            return null;
        }
        String rule = getRuleTitle(getString(r));
        if (StringUtil.isEmpty(rule)) {
            throw new Exception("获取规则失败");
        }
        AuthStorageItem auth = getAuth(rule, "loadJavaClass");
        if (auth.getResult() == AuthBridgeEvent.AuthResult.DISAGREE) {
//            throw new Exception("用户拒绝使用此方法");
            return null;
        } else if (auth.getResult() == AuthBridgeEvent.AuthResult.NONE) {
            CountDownLatch lock = new CountDownLatch(1);
            AuthBridgeEvent event = new AuthBridgeEvent(rule, "动态加载Java代码（" + path + "）", "loadJavaClass", lock);
            EventBus.getDefault().post(event);
            lock.await(1, TimeUnit.MINUTES);
            auth = getAuth(rule, "loadJavaClass");
            if (auth.getResult() != AuthBridgeEvent.AuthResult.AGREE) {
                return null;
            }
        }

        try {
            try {
                Class<?> cls1 = Class.forName(className);
                return cls1.newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                if (CollectionUtil.isNotEmpty(classloaders)) {
                    Class<?> cls1 = null;
                    for (ClassLoader classloader : classloaders) {
                        try {
                            cls1 = Class.forName(className, true, classloader);
                            break;
                        } catch (ClassNotFoundException classNotFoundException) {
                            classNotFoundException.printStackTrace();
                        }
                    }
                    if (cls1 != null) {
                        return cls1.newInstance();
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        File desFile = new File(getFilePath(path));
        if (!desFile.exists()) {
            return null;
        }
        File dir = new File(getFilePath("hiker://files/cache/java"));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (StringUtil.isEmpty(so) || isUndefined(so)) {
            so = null;
        } else {
            so = getFilePath(so);
            File appLibs = Application.getContext().getDir("libs", android.content.Context.MODE_PRIVATE);
            if (appLibs.exists()) {
                File file = new File(so);
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File file1 : files) {
                            if (file1.getName().endsWith(".so")) {
                                FileUtil.copy(file1, new File(appLibs.getAbsolutePath() + File.separator + file1.getName()));
                            }
                        }
                    }
                } else {
                    if (file.exists()) {
                        FileUtil.copy(file, new File(appLibs.getAbsolutePath() + File.separator + file.getName()));
                        so = appLibs.getAbsolutePath();
                    } else {
                        so = null;
                    }
                }
            }
        }
        //下面开始加载dex class
        //1.待加载的dex文件路径，如果是外存路径，一定要加上读外存文件的权限,
        //2.解压后的dex存放位置，此位置一定要是可读写且仅该应用可读写
        //3.指向包含本地库(so)的文件夹路径，可以设为null
        //4.父级类加载器，一般可以通过Context.getClassLoader获取到，也可以通过ClassLoader.getSystemClassLoader()取到。
        DexClassLoader classLoader = new DexClassLoader(desFile.getAbsolutePath(), dir.getAbsolutePath(), so, clazz.getClassLoader());
        try {
            if(StringUtil.isNotEmpty(so)){
                File soFile = new File(so);
                String soDir = soFile.isDirectory() ? so : soFile.getParent();
                try {
                    RetroLoadLibrary.installNativeLibraryPath(classLoader, new File(soDir));
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                try {
                    RetroLoadLibrary.installNativeLibraryPath(getClass().getClassLoader(), new File(soDir));
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            Class<?> cls = classLoader.loadClass(className);
            classloaders.add(classLoader);
            if (cls != null) {
                return cls.newInstance();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JSAnnotation(returnType = ReturnType.OBJECT)
    public Object evalPrivateJS(@Parameter("c") Object c, @Parameter("evalKey") Object obj) throws Exception {
        String code = tryGetString(c);
        Context context = Context.getCurrentContext();
        Scriptable scope = ((IdFunctionObject) obj).getParentScope();
        code = AesUtil.decrypt(AES_DEFAULT_KEY, code);
        return ((IdFunctionObject) obj).call(context, scope, scope, new Object[]{code});
    }

    @JSAnnotation(returnType = ReturnType.OBJECT)
    public Object hexToBytes(@Parameter("c") Object c, @Parameter("evalKey") Object obj) throws Exception {
        String code = tryGetString(c);
        Context context = Context.getCurrentContext();
        Scriptable scope = ((IdFunctionObject) obj).getParentScope();
        code = "let hexString = \"" + code + "\";\n" +
                "let MAP_HEX = {\n" +
                "    0: 0,\n" +
                "    1: 1,\n" +
                "    2: 2,\n" +
                "    3: 3,\n" +
                "    4: 4,\n" +
                "    5: 5,\n" +
                "    6: 6,\n" +
                "    7: 7,\n" +
                "    8: 8,\n" +
                "    9: 9,\n" +
                "    a: 10,\n" +
                "    b: 11,\n" +
                "    c: 12,\n" +
                "    d: 13,\n" +
                "    e: 14,\n" +
                "    f: 15,\n" +
                "    A: 10,\n" +
                "    B: 11,\n" +
                "    C: 12,\n" +
                "    D: 13,\n" +
                "    E: 14,\n" +
                "    F: 15\n" +
                "};\n" +
                "    let bytes = new Uint8Array(Math.floor((hexString || \"\").length / 2));\n" +
                "    let i;\n" +
                "    for (i = 0; i < bytes.length; i++) {\n" +
                "        let a = MAP_HEX[hexString[i * 2]];\n" +
                "        let b = MAP_HEX[hexString[i * 2 + 1]];\n" +
                "        if (a === undefined || b === undefined) {\n" +
                "            break;\n" +
                "        }\n" +
                "        bytes[i] = (a << 4) | b;\n" +
                "    }\n" +
                "    i === bytes.length ? bytes : bytes.slice(0, i)";
        return ((IdFunctionObject) obj).call(context, scope, scope, new Object[]{code});
    }

    @JSAnnotation(returnType = ReturnType.OBJECT)
    public Object require(@Parameter("c") Object c, @Parameter("options") Object options,
                          @Parameter(value = "v", defaultInt = 0) Object version,
                          @Parameter("ruleTitleKey") Object r, @Parameter("evalKey") Object obj) throws Exception {
        String rule = getRuleTitle(getString(r));
        return require0(c, -1, options, obj, true, rule, getRequireVersion(version));
    }

    @JSAnnotation(returnType = ReturnType.VOID)
    public void deleteCache(@Parameter(value = "c", defaultValue = "-1") Object c, @Parameter("ruleTitleKey") Object r) throws Exception {
        String url = tryGetString(c);
        if ("-1".equals(url)) {
            String rule = getRuleTitle(getString(r));
            RequireUtils.deleteCacheByRule(rule);
        } else {
            RequireUtils.deleteCache(url);
        }
    }

    @JSAnnotation(returnType = ReturnType.OBJECT, alias = "rc")
    public Object requireCache(@Parameter("c") Object c, @Parameter("h") Object h, @Parameter("options") Object options,
                               @Parameter(value = "v", defaultInt = 0) Object version,
                               @Parameter("ruleTitleKey") Object r, @Parameter("evalKey") Object obj) throws Exception {
        int hour = 24;
        String rule = getRuleTitle(getString(r));
        Object oo1 = argsNativeObjectAdjust(h);
        if (oo1 instanceof Integer) {
            hour = (Integer) oo1;
        } else if (oo1 instanceof Double) {
            hour = ((Double) oo1).intValue();
        }
        return require0(c, hour, options, obj, true, rule, getRequireVersion(version));
    }

    private int getRequireVersion(Object version) {
        if (version == null || isUndefined(version)) {
            return 0;
        }
        Object oo1 = argsNativeObjectAdjust(version);
        if (oo1 instanceof Integer) {
            return (Integer) oo1;
        } else if (oo1 instanceof Double) {
            return ((Double) oo1).intValue();
        } else if (oo1 instanceof String) {
            return Integer.parseInt((String) oo1);
        } else {
            return 0;
        }
    }

    @JSAnnotation(returnType = ReturnType.STRING, alias = "fc")
    public String fetchCache(@Parameter("c") Object c, @Parameter("h") Object h, @Parameter("options") Object options,
                             @Parameter(value = "v", defaultInt = 0) Object version,
                             @Parameter("ruleTitleKey") Object r, @Parameter("evalKey") Object obj) throws Exception {
        int hour = 24;
        String rule = getRuleTitle(getString(r));
        Object oo1 = argsNativeObjectAdjust(h);
        if (oo1 instanceof Integer) {
            hour = (Integer) oo1;
        } else if (oo1 instanceof Double) {
            hour = ((Double) oo1).intValue();
        }
        return (String) require0(c, hour, options, obj, false, rule, getRequireVersion(version));
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getIP() {
        return LocalServerParser.getIP(Application.application);
    }

    public void updateRequire(String url) throws Exception {
        String fileName = StringUtil.md5(url) + ".js";
        String dir = UriUtils.getRootDir(Application.application.getApplicationContext()) + File.separator + "libs";
        String filePath = dir + File.separator + fileName;
        String code = requestForCode(url, null);
        if (StringUtil.isEmpty(code)) {
            throw new Exception("获取远程依赖失败：" + url);
        }
        FileUtil.stringToFile(code, filePath);
    }

    private Object require0(Object c, int hour, Object options, Object obj, boolean eval, String rule, int version) throws Exception {
        String url = tryGetString(c);
        if (StringUtil.isNotEmpty(url) && (url.startsWith("file://") || url.startsWith("hiker://"))) {
            Object articleListRule = null;
            if (url.startsWith("hiker://page/")) {
                articleListRule = LitePal.where("title = ?", rule).limit(1).findFirst(ArticleListRule.class);
            }
            String cd = (String) fetchByHiker(url, articleListRule, false);
            if (eval) {
                if (url.startsWith("hiker://page/")) {
                    ArticleListPageRule pageRule = JSON.parseObject(cd, ArticleListPageRule.class);
                    if (pageRule != null && StringUtil.isNotEmpty(pageRule.getRule())) {
                        if (pageRule.getRule().startsWith("js:")) {
                            cd = pageRule.getRule().substring(3);
                        } else {
                            cd = pageRule.getRule();
                        }
                    }
                }
                Context context = Context.getCurrentContext();
                Scriptable scope = ((IdFunctionObject) obj).getParentScope();
                return ((IdFunctionObject) obj).call(context, scope, scope, new Object[]{cd});
            } else {
                return cd;
            }
        }
        if (StringUtil.isEmpty(url) || !url.startsWith("http")) {
            throw new Exception("require地址必须为http地址：" + url);
        }
        Context context = Context.getCurrentContext();
        Scriptable scope = ((IdFunctionObject) obj).getParentScope();
        String md5 = StringUtil.md5(url);
        String fileName = md5 + ".js";
        String descName = md5 + ".json";
        String dir = UriUtils.getRootDir(Application.application.getApplicationContext()) + File.separator + "libs";
        String filePath = dir + File.separator + fileName;
        String descPath = dir + File.separator + descName;
        File file = new File(filePath);
        String code;
        int existVersion = RequireUtils.getRequireVersion(descPath, version);
        if (file.exists()) {
            req:
            if (hour >= 0 || existVersion < version) {
                long now = System.currentTimeMillis();
                if (existVersion < version || hour == 0 || now - file.lastModified() > 3600 * 1000 * hour) {
                    //版本强制更新或者超过缓存失效的时间了，重新下载覆盖
                    code = requestForCode(url, options);
                    //校验获取到的是js代码
                    if (eval && !isJsCode(code)) {
                        log("url: " + url + "获取的内容被判定为非JS代码", null);
                        break req;
                    }
                    //fetchCache：道长仓库炸了
                    if (!eval && url.contains("hiker.nokia.press") &&
                            (StringUtil.isEmpty(code) || code.length() < 3 || code.contains("非法猥亵"))) {
                        break req;
                    }
                    //fetchCache：gitee炸了
                    if (!eval && url.contains("gitee.com/") &&
                            (StringUtil.isEmpty(code) || code.length() < 3)) {
                        break req;
                    }

                    if (StringUtil.isEmpty(code)) {
                        throw new Exception("获取远程依赖失败：" + url);
                    }
                    FileUtil.stringToFile(code, filePath);
                    RequireUtils.updateDescription(descPath, url, version);
                    RequireUtils.generateRequireMap(rule, url, filePath);
                    if (eval) {
                        return ((IdFunctionObject) obj).call(context, scope, scope, new Object[]{code});
                    } else {
                        return code;
                    }
                }
            }
            code = FileUtil.fileToString(filePath);
        } else {
            code = requestForCode(url, options);
            if (StringUtil.isEmpty(code)) {
                throw new Exception("获取远程依赖失败：" + url);
            }
            FileUtil.stringToFile(code, filePath);
            RequireUtils.updateDescription(descPath, url, version);
        }
        RequireUtils.generateRequireMap(rule, url, filePath);
        if (eval) {
            return ((IdFunctionObject) obj).call(context, scope, scope, new Object[]{code});
        } else {
            return code;
        }
    }

    private String requestForCode(String url, Object options) {
        try {
            options = generateHeadersOptions(options);
            String response = request(url, options, null);
            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject != null) {
                String error = jsonObject.getString("error");
                if (StringUtil.isNotEmpty(error) && !"null".equals(error)) {
                    return null;
                }
                String statusCode = String.valueOf(jsonObject.getIntValue("statusCode"));
                if (!statusCode.startsWith("2")) {
                    //非2xx状态码，则认为请求失败
                    return null;
                }
                return jsonObject.getString("body");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String md5(@Parameter("c") Object c) throws Exception {
        return StringUtil.md5(tryGetString(c));
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getPrivateJS(@Parameter("c") Object c) throws Exception {
        return AesUtil.encrypt(AES_DEFAULT_KEY, tryGetString(c));
    }

    @JSAnnotation(returnType = ReturnType.OBJECT)
    public Object xpath(@Parameter("html") String html, @Parameter("exp") String exp) throws Exception {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
        XPath xPath = XPathFactory.newInstance().newXPath();
        return xPath.evaluate(exp, doc);
    }

    @JSAnnotation(returnType = ReturnType.JSON, alias = "xpa")
    public String xpathArray(@Parameter("html") String html, @Parameter("exp") String exp) throws Exception {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Object result = xPath.evaluate(exp, doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        List<String> list = new ArrayList<>();
        try {
            for (int i = 0; i < nodes.getLength(); i++) {
                list.add(nodes.item(i).getNodeValue());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return JSON.toJSONString(list);
    }

    @JSAnnotation(returnType = ReturnType.STRING)
    public String getHome(@Parameter(value = "url") String url, @Parameter("urlKey") Object urlKey) {
        if (StringUtil.isNotEmpty(url)) {
            return StringUtil.getHome(url);
        }
        String myUrl = (String) argsNativeObjectAdjust(urlKey);
        if (StringUtil.isNotEmpty(myUrl)) {
            return StringUtil.getHome(myUrl);
        }
        return "";
    }

    private String getReplaceJS(String js) {
        if (StringUtil.isNotEmpty(js)) {
            js = js.replace("if (b != null && b.length() > 0) {", "if (b != null && b.length > 0) {");
            return "try{\n" + js + "\n}catch(e){\nsetError(e);\n}";
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
            funcStr = getFunctionStr(funcStr, an.returnType(), an.alias(), method);
        }
        return funcStr;
    }

    private String getFunctionStr(String funcStr, ReturnType type, String alias, Method method) {
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
                    Parameter parameter = (Parameter) annotation;
                    String parameterName = parameter.value();
                    switch (parameterName) {
                        case "callbackKey":
                            paramCheck.append("param").append(i).append(" = CALLBACK_KEY;\n");
                            break;
                        case "ruleKey":
                            paramCheck.append("param").append(i).append(" = MY_RULE;\n");
                            break;
                        case "urlKey":
                            paramCheck.append("param").append(i).append(" = ").append("param").append(i).append(" || MY_URL;\n");
                            break;
                        case "ruleStrKey":
                            paramCheck.append("param").append(i).append(" = my_rule;\n");
                            break;
                        case "ruleTitleKey":
                            paramCheck.append("param").append(i).append(" = MY_TICKET;\n");
                            break;
                        case "typeKey":
                            paramCheck.append("param").append(i).append(" = MY_TYPE;\n");
                            break;
                        case "evalKey":
                            paramCheck.append("param").append(i).append(" = eval;\n");
                            break;
                    }
                    String defaultValue = parameter.defaultValue();
                    if (StringUtil.isNotEmpty(defaultValue)) {
                        defaultValue = Utils.escapeJavaScriptString(defaultValue);
                        paramCheck.append("param").append(i).append(" = ").append("param").append(i).append(" || \"").append(defaultValue).append("\";\n");
                    }
                    int defaultInt = parameter.defaultInt();
                    if (defaultInt >= 0) {
                        paramCheck.append("param").append(i).append(" = ").append("param").append(i).append(" || ").append(defaultInt).append(";\n");
                    }
                }
            }
        }

        Class returnType = method.getReturnType();
        String returnStr = returnType.getSimpleName().equals("void") ? "" : "return";//是否有返回值

        String methodStr = String.format(" var method_%s = ScriptAPI.getMethod(\"%s\"%s);\n", functionName, functionName, paramsTypeString);
        String functionStr = "";
        if (type == ReturnType.STRING) {
            //返回字符串
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return retStr == null ? retStr : retStr + '';\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == ReturnType.JSON) {
            //返回对象
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return JSON.parse(retStr);\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == ReturnType.OBJECT) {
            //返回JS原生对象
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    return method_%s.invoke(javaContext%s);\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == ReturnType.BOOL) {
            //返回布尔类型
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return retStr + '' == 'true';\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else if (type == ReturnType.Num) {
            //返回整型
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    var retStr = method_%s.invoke(javaContext%s);\n" +
                            "    return parseInt(retStr);\n" +
                            " }\n", functionName, paramsNameString, functionName, paramsNameInvokeString);
        } else {
            //非返回对象
            functionStr = String.format(
                    " function %s(%s){\n" +
                            paramCheck.toString() +
                            "    %s method_%s.invoke(javaContext%s);\n" +
                            " }\n", functionName, paramsNameString, returnStr, functionName, paramsNameInvokeString);
        }
        String js = funcStr + methodStr + functionStr;
        methodList.add(functionName);
        if (StringUtil.isNotEmpty(alias)) {
            js = js + String.format("\nvar %s = %s;", alias, functionName);
            methodList.add(alias);
        }
        return js;
    }


    private String getString(Object input) {
        Object res = argsNativeObjectAdjust(input);
        if (res instanceof String) {
            return (String) res;
        }
        return null;
    }

    private String tryGetString(Object input) throws Exception {
        Object res = argsNativeObjectAdjust(input);
        if (res instanceof String) {
            return (String) res;
        } else {
            throw new Exception("参数类型只能是字符串");
        }
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
        return argsNativeObjectAdjust(input, false);
    }

    private Object argsNativeObjectAdjust(Object input, boolean forParam) {
        if (isUndefined(input)) {
            return input;
        }
        if (input instanceof NativeObject) {
            JSONObject bodyJson = new JSONObject();
            NativeObject nativeBody = (NativeObject) input;
            for (Object key : nativeBody.keySet()) {
                Object value = nativeBody.get(key);
                if (!forParam || !"param".equals(key)) {
                    value = argsNativeObjectAdjust(value, forParam);
                }
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
                value = argsNativeObjectAdjust(value, forParam);
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

        void onUpdate(String action, String data);

        void showErr(String msg);
    }

    /**
     * 注解
     */
    @Target(value = ElementType.METHOD)
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface JSAnnotation {
        ReturnType returnType() default ReturnType.VOID;//是否返回对象，默认为false 不返回，1：字符串

        String alias() default "";//别名
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Parameter {
        String value() default "";

        String defaultValue() default "";

        int defaultInt() default -1;
    }

    public enum ReturnType {
        VOID, STRING, JSON, BOOL, Num, OBJECT
    }

    private boolean isJsCode(String code) {
        if (StringUtil.isEmpty(code)) {
            return false;
        }
        code = code.trim();
        String[] notJsCode1 = new String[]{"<!DOCTYPE", "<html", "<?xml"};
        for (String s : notJsCode1) {
            if (code.startsWith(s)) {
                return false;
            }
        }
        String[] notJsCode2 = new String[]{"</html>", "</rss>"};
        for (String s : notJsCode2) {
            if (code.endsWith(s)) {
                return false;
            }
        }
        String[] jsKey = new String[]{"var ", "let ", "function", "eval(", "call(", "eval (", "call (", " => ", ")=>"};
        for (String s : jsKey) {
            if (code.contains(s)) {
                return true;
            }
        }
        if (methodList != null) {
            for (String s : methodList) {
                if (code.contains(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
