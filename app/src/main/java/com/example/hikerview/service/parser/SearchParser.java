package com.example.hikerview.service.parser;

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
                            getTitle(listBean, url, movieInfo, elementt, ss);
                        } catch (Exception e) {
                            listBean.setTitle("");
                        }
                    } else {
                        getTitle(listBean, url, movieInfo, elementt, ss);
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
        }
        if (isOk) {
            callBackSuccess(callback, listBeanList);
        }

    }

    private static void getTitle(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        String[] ss1 = allRules[1].split("&&");
        Element element2;
        if (ss1.length == 1) {
            element2 = elementt;
        } else {
            element2 = CommonParser.getTrueElement(ss1[0], elementt);
        }
        for (int i = 1; i < ss1.length - 1; i++) {
            element2 = CommonParser.getTrueElement(ss1[i], element2);
        }
        listBean.setTitle(CommonParser.getText(element2, ss1[ss1.length - 1]));
    }

    private static void getUrl(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        String[] ss2 = allRules[2].split("&&");
        Element element3;
        if (ss2.length == 1) {
            element3 = elementt;
        } else {
            element3 = CommonParser.getTrueElement(ss2[0], elementt);
        }
        for (int i = 1; i < ss2.length - 1; i++) {
            element3 = CommonParser.getTrueElement(ss2[i], elementt);
        }
        listBean.setUrl(CommonParser.getUrl(element3, ss2[ss2.length - 1], movieInfo, url));
    }

    private static void getPicUrl(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        if (allRules.length > 5 && StringUtil.isNotEmpty(allRules[5]) && !"*".equals(allRules[5])) {
            String[] ss5 = allRules[5].split("&&");
            Element element5;
            if (ss5.length == 1) {
                element5 = elementt;
            } else {
                element5 = CommonParser.getTrueElement(ss5[0], elementt);
            }
            for (int i = 1; i < ss5.length - 1; i++) {
                element5 = CommonParser.getTrueElement(ss5[i], element5);
            }
            listBean.setImg(CommonParser.getUrl(element5, ss5[ss5.length - 1], movieInfo, movieInfo.getChapterUrl()));
        }
    }

    private static void getDesc(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        if (allRules.length > 3 && StringUtil.isNotEmpty(allRules[3]) && !"*".equals(allRules[3])) {
            String[] ss3 = allRules[3].split("&&");
            Element element4;
            if (ss3.length == 1) {
                element4 = elementt;
            } else {
                element4 = CommonParser.getTrueElement(ss3[0], elementt);
            }
            for (int i = 1; i < ss3.length - 1; i++) {
                element4 = CommonParser.getTrueElement(ss3[i], elementt);
            }
            listBean.setDescMore(CommonParser.getText(element4, ss3[ss3.length - 1]));
        }
    }

    private static void getContent(SearchResult listBean, String url, MovieRule movieInfo, Element elementt, String[] allRules) {
        if (allRules.length > 4 && StringUtil.isNotEmpty(allRules[4]) && !"*".equals(allRules[4])) {
            String[] ss4 = allRules[4].split("&&");
            Element element4;
            if (ss4.length == 1) {
                element4 = elementt;
            } else {
                element4 = CommonParser.getTrueElement(ss4[0], elementt);
            }
            for (int i = 1; i < ss4.length - 1; i++) {
                element4 = CommonParser.getTrueElement(ss4[i], elementt);
            }
            listBean.setContent(CommonParser.getText(element4, ss4[ss4.length - 1]));
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
