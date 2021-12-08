package com.example.hikerview.service.parser;

import android.os.Looper;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.ui.base.BaseCallback;
import com.example.hikerview.ui.browser.model.SearchEngine;
import com.example.hikerview.ui.home.enums.HomeActionEnum;
import com.example.hikerview.ui.home.model.ArticleList;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.SearchResult;

import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2018/12/9
 * 时间：At 18:46
 */
public class JsEngineBridge {
    private static final String TAG = "JsEngineBridge";

    /**
     * 用JS解析字符串
     *
     * @param input     字符串
     * @param js        js规则
     * @param movieRule movieRule
     * @param callBack  返回
     */
    public static void parseCallBack(final String input, final String js,
                                     final MovieRule movieRule, final JSEngine.OnFindCallBack<String> callBack) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            JSEngine.getInstance().parseStr(input, js, movieRule, callBack);
        } else {
            new Thread(() -> JSEngine.getInstance().parseStr(input, js, movieRule, callBack)).start();
        }
    }

    public static void parseHomeCallBack(String url, final String colType, final ArticleListRule articleListRule, final String rule, final String s, final boolean newLoad, final BaseCallback<ArticleList> callback) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            JSEngine.getInstance().parseHome(url, s, articleListRule, rule, new JSEngine.OnFindCallBack<List<ArticleList>>() {
                @Override
                public void onSuccess(List<ArticleList> data) {
//                    Log.d(TAG, "onSuccess: col_type="+colType+"===>"+ JSON.toJSONString(data));
                    if (!TextUtils.isEmpty(colType)) {
                        for (int i = 0; i < data.size(); i++) {
                            if ("*".equals(data.get(i).getPic())) {
                                data.get(i).setType(ArticleColTypeEnum.TEXT_1.getCode());
                            } else if(TextUtils.isEmpty(data.get(i).getType())){
                                data.get(i).setType(colType);
                            }
                        }
                    } else {
                        for (int i = 0; i < data.size(); i++) {
                            if ("*".equals(data.get(i).getPic())) {
                                data.get(i).setType(ArticleColTypeEnum.TEXT_1.getCode());
                            }else if(TextUtils.isEmpty(data.get(i).getType())){
                                data.get(i).setType(ArticleColTypeEnum.MOVIE_3.getCode());
                            }
                        }
                    }
                    if (newLoad) {
                        callback.bindArrayToView(HomeActionEnum.ARTICLE_LIST_NEW, data);
                    } else {
                        callback.bindArrayToView(HomeActionEnum.ARTICLE_LIST, data);
                    }
                }

                @Override
                public void onUpdate(String action, String data) {
                    ArticleList articleList = new ArticleList();
                    articleList.setTitle(data);
                    callback.bindObjectToView(action, articleList);
                }

                @Override
                public void showErr(String msg) {
                    callback.error("JS解析失败", msg, "404", null);
                    JSEngine.getInstance().log(msg, JSON.toJSON(articleListRule));
                }
            });
        } else {
            new Thread(() -> JSEngine.getInstance().parseHome(url, s, articleListRule, rule, new JSEngine.OnFindCallBack<List<ArticleList>>() {
                @Override
                public void onSuccess(List<ArticleList> data) {
//                    Log.d(TAG, "onSuccess: col_type="+colType+"===>"+ JSON.toJSONString(data));
                    if (!TextUtils.isEmpty(colType)) {
                        for (int i = 0; i < data.size(); i++) {
                            if ("*".equals(data.get(i).getPic())) {
                                data.get(i).setType(ArticleColTypeEnum.TEXT_1.getCode());
                            } else if(TextUtils.isEmpty(data.get(i).getType())){
                                data.get(i).setType(colType);
                            }
                        }
                    } else {
                        for (int i = 0; i < data.size(); i++) {
                            if ("*".equals(data.get(i).getPic())) {
                                data.get(i).setType(ArticleColTypeEnum.TEXT_1.getCode());
                            }else if(TextUtils.isEmpty(data.get(i).getType())){
                                data.get(i).setType(ArticleColTypeEnum.MOVIE_3.getCode());
                            }
                        }
                    }
                    if (newLoad) {
                        callback.bindArrayToView(HomeActionEnum.ARTICLE_LIST_NEW, data);
                    } else {
                        callback.bindArrayToView(HomeActionEnum.ARTICLE_LIST, data);
                    }
                }

                @Override
                public void onUpdate(String action, String data) {
                    ArticleList articleList = new ArticleList();
                    articleList.setTitle(data);
                    callback.bindObjectToView(action, articleList);
                }

                @Override
                public void showErr(String msg) {
                    callback.error("JS解析失败", msg, "404", null);
                    JSEngine.getInstance().log(msg, JSON.toJSON(articleListRule));
                }
            })).start();
        }
    }


    /**
     * 搜索
     * @param searchEngine 规则
     * @param s 源码
     * @param callback 回调
     */
    public static void parseCallBack(String url, final SearchEngine searchEngine, final String s, final SearchJsCallBack<List<SearchResult>> callback){
        if(Looper.myLooper() != Looper.getMainLooper()){
            JSEngine.getInstance().parseSearchRes(url, s, searchEngine, new JSEngine.OnFindCallBack<List<SearchResult>>() {
                @Override
                public void onSuccess(List<SearchResult> data) {
                    callback.showData(data);
                }

                @Override
                public void onUpdate(String action, String data) {
                    callback.onUpdate(action, data);
                }

                @Override
                public void showErr(String msg) {
                    callback.showErr(msg);
                    JSEngine.getInstance().log(msg, JSON.toJSON(searchEngine));
                }
            });
        }else {
            new Thread(() -> JSEngine.getInstance().parseSearchRes(url, s, searchEngine, new JSEngine.OnFindCallBack<List<SearchResult>>() {
                @Override
                public void onSuccess(List<SearchResult> data) {
                    callback.showData(data);
                }

                @Override
                public void onUpdate(String action, String data) {
                    callback.onUpdate(action, data);
                }

                @Override
                public void showErr(String msg) {
                    callback.showErr(msg);
                    JSEngine.getInstance().log(msg, JSON.toJSON(searchEngine));
                }
            })).start();
        }
    }

    public static void parseLastChapterCallback(String url, String res, ArticleListRule articleListRule, String rule, BaseParseCallback<String> callBack) {
        if(Looper.myLooper() != Looper.getMainLooper()){
            JSEngine.getInstance().parseLastChapterRule(url, res, articleListRule, rule, new JSEngine.OnFindCallBack<String>() {
                @Override
                public void onSuccess(String data) {
                    callBack.success(data);
                }

                @Override
                public void onUpdate(String action, String data) {

                }

                @Override
                public void showErr(String msg) {
                    callBack.error("获取更新失败，请前往日志查看报错！");
                    JSEngine.getInstance().log(msg, JSON.toJSON(articleListRule));
                }
            });
        } else {
            new Thread(() -> JSEngine.getInstance().parseLastChapterRule(url, res, articleListRule, rule, new JSEngine.OnFindCallBack<String>() {
                @Override
                public void onSuccess(String data) {
                    callBack.success(data);
                }

                @Override
                public void onUpdate(String action, String data) {

                }

                @Override
                public void showErr(String msg) {
                    callBack.error("获取更新失败，请前往日志查看报错！");
                    JSEngine.getInstance().log(msg, JSON.toJSON(articleListRule));
                }
            })).start();
        }

    }

}
