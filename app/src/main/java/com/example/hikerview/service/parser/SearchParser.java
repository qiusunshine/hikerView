package com.example.hikerview.service.parser;

import com.alibaba.fastjson.JSON;
import com.example.hikerview.model.MovieRule;
import com.example.hikerview.ui.browser.model.SearchEngine;
import com.example.hikerview.ui.home.model.SearchResult;
import com.example.hikerview.ui.setting.model.SettingConfig;
import com.example.hikerview.utils.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2018/10/22
 * 时间：At 14:46
 */
public class SearchParser {

    /**
     * 解析搜索结果
     *
     * @param url          url
     * @param searchEngine 规则1
     * @param s            源码
     * @param callback     回调
     */

    public static void findList(String url, SearchEngine searchEngine, String s, SearchJsCallBack<List<SearchResult>> callback) {
        MovieRule movieInfo = searchEngine.toMovieRule();
        movieInfo.setChapterUrl(url);
        if (StringUtil.isEmpty(searchEngine.getFindRule())) {
            callBackError(callback, "搜索解析规则不能为空");
            return;
        }
        if (searchEngine.getFindRule().startsWith("js:")) {
            JsEngineBridge.parseCallBack(url, searchEngine, s, callback);
            return;
        }
        boolean isOk = true;
        List<SearchResult> listBeanList = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(s);
            String[] ss = searchEngine.getFindRule().split(";");
            String[] ss0 = ss[0].split("&&");
            Element element;
            element = CommonParser.getTrueElement(ss0[0], doc);
            Elements elements;
            /**
             * 获取列表
             */
            for (int i = 1; i < ss0.length - 1; i++) {
                element = CommonParser.getTrueElement(ss0[i], element);
            }
            elements = CommonParser.selectElements(element, ss0[ss0.length - 1]);
            /**
             * 获取名字和链接
             */
            for (Element elementt : elements) {
                try {
                    SearchResult listBean = new SearchResult();
                    //获取名字
                    if (SettingConfig.developerMode) {
                        try {
                            listBean.setTitle(CommonParser.getTextByRule(elementt, ss[1]));
                        } catch (Exception e) {
                            listBean.setTitle("");
                        }
                    } else {
                        listBean.setTitle(CommonParser.getTextByRule(elementt, ss[1]));
                    }
                    //获取链接
                    if (SettingConfig.developerMode) {
                        try {
                            getUrl(listBean, url, movieInfo, elementt, ss);
                        } catch (Exception e) {
                            listBean.setUrl("");
                        }
                    } else {
                        getUrl(listBean, url, movieInfo, elementt, ss);
                    }
                    //获取更多信息
                    if (SettingConfig.developerMode) {
                        try {
                            getDesc(listBean, url, movieInfo, elementt, ss);
                        } catch (Exception e) {
                            listBean.setDescMore("");
                        }
                    } else {
                        getDesc(listBean, url, movieInfo, elementt, ss);
                    }
                    //获取content内容

                    if (SettingConfig.developerMode) {
                        try {
                            getContent(listBean, url, movieInfo, elementt, ss);
                        } catch (Exception e) {
                            listBean.setContent("");
                        }
                    } else {
                        getContent(listBean, url, movieInfo, elementt, ss);
                    }

                    //获取图片链接
                    if (SettingConfig.developerMode) {
                        try {
                            getPicUrl(listBean, url, movieInfo, elementt, ss);
                        } catch (Exception e) {
                            listBean.setImg("");
                        }
                    } else {
                        getPicUrl(listBean, url, movieInfo, elementt, ss);
                    }
                    //来自的网站名称
                    listBean.setDesc(movieInfo.getTitle());
                    listBean.setType("video");
                    listBeanList.add(listBean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isOk = false;
            callBackError(callback, "findList：msg：" + e.toString());
            JSEngine.getInstance().log(e.getMessage(), JSON.toJSON(searchEngine));
        }
        if (isOk) {
            callBackSuccess(callback, listBeanList);
        }

    }

    private static void getUrl(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        listBean.setUrl(CommonParser.getUrlByRule(url, movieInfo, elementt, allRules[2]));
    }

    private static void getPicUrl(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        if (allRules.length > 5 && StringUtil.isNotEmpty(allRules[5]) && !"*".equals(allRules[5])) {
            listBean.setImg(CommonParser.getUrlByRule("*", movieInfo, elementt, allRules[5]));
        }
    }

    private static void getDesc(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        if (allRules.length > 3 && StringUtil.isNotEmpty(allRules[3]) && !"*".equals(allRules[3])) {
            listBean.setDescMore(CommonParser.getTextByRule(elementt, allRules[3]));
        }
    }

    private static void getContent(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        if (allRules.length > 4 && StringUtil.isNotEmpty(allRules[4]) && !"*".equals(allRules[4])) {
            listBean.setContent(CommonParser.getTextByRule(elementt, allRules[4]));
        }
    }

    /**
     * static方法会锁住所有这个类中的这个方法，方便线程同步
     *
     * @param callback 返回到view中
     * @param msg      错误信息
     */
    public static synchronized void callBackError(SearchJsCallBack<List<SearchResult>> callback, String msg) {
        callback.showErr(msg);
    }

    private static synchronized void callBackSuccess(SearchJsCallBack<List<SearchResult>> callback, List<SearchResult> listBeanList) {
        callback.showData(listBeanList);
    }
}
