package com.example.hikerview.service.parser;

import android.text.TextUtils;

import com.example.hikerview.model.MovieRule;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.utils.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：By hdy
 * 日期：On 2018/10/22
 * 时间：At 14:47
 */
public class CommonParser {
    private static final String TAG = "CommonParser";
    private static String[] normalAttrs = {"href", "src", "class", "title", "alt"};

    public static Element getTrueElement(String rule, Element element) {
        if (rule.startsWith("Text") || rule.startsWith("Attr")) {
            return element;
        }
        for (String normalAttr : normalAttrs) {
            if (normalAttr.equals(rule)) {
                return element;
            }
        }
        //剔除元素
        String[] rules = rule.split("--");
        if (rules.length > 1) {
            Element e = getTrueElement(rules[0], element);
            String s = e.outerHtml();
            for (int i = 1; i < rules.length; i++) {
                String r = getTrueElement(rules[i], e).outerHtml();
                s = s.replace(r, "");
                e = Jsoup.parse(s);
            }
            return e;
        }
        //或规则
        String[] ors = rule.split("\\|\\|");
        if (ors.length > 1) {
            for (int i = 0; i < ors.length; i++) {
                Element e = null;
                try {
                    e = getTrueElement(ors[i], element);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                if (e != null) {
                    return e;
                }
            }
        }
        String[] ss01 = rule.split(",");
        if (ss01.length > 1) {
            int index = Integer.parseInt(ss01[1]);
            Elements elements = element.select(ss01[0]);
            if (index < 0) {
                return elements.get(elements.size() + index);
            } else {
                return element.select(ss01[0]).get(index);
            }
        } else return element.select(rule).first();
    }

    public static Elements selectElements(Element element, String rule) {
        String[] ors = rule.split("\\|\\|");
        Elements res = new Elements();
        for (int i = 0; i < ors.length; i++) {
            try {
                res.addAll(selectElementsWithoutOr(element, ors[i]));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return res;
    }

    private static Elements selectElementsWithoutOr(Element element, String rule) {
        String[] rules = rule.split(",");
        if (rules.length > 1) {
            String[] indexNumbs = rules[1].split(":", -1);
            int startPos = 0;
            int endPos = 0;
            if (!TextUtils.isEmpty(indexNumbs[0])) {
                try {
                    startPos = Integer.parseInt(indexNumbs[0]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            if (!TextUtils.isEmpty(indexNumbs[1])) {
                try {
                    endPos = Integer.parseInt(indexNumbs[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            Elements elements = element.select(rules[0]);
            if (endPos > elements.size()) {
                endPos = elements.size();
            }
            if (endPos <= 0) {
                endPos = elements.size() + endPos;
            }
            Elements res = new Elements();
            for (int i = startPos; i < endPos; i++) {
                res.add(elements.get(i));
            }
            return res;
        } else {
            return element.select(rule);
        }
    }

    public static String getUrlByRule(String url, MovieRule movieInfo, Element elementt, String rule) {
        if ("*".equals(rule)) {
            return url;
        } else {
            String[] ss6 = rule.split("&&");
            Element element5;
            if (ss6.length == 1) {
                element5 = elementt;
            } else {
                element5 = CommonParser.getTrueElement(ss6[0], elementt);
            }
            for (int i = 1; i < ss6.length - 1; i++) {
                element5 = CommonParser.getTrueElement(ss6[i], element5);
            }
            return CommonParser.getUrl(element5, ss6[ss6.length - 1], movieInfo, movieInfo.getChapterUrl());
        }
    }

    public static String getTextByRule(Element elementt, String rule) {
        if (StringUtil.isEmpty(rule) || "*".equals(rule)) {
            return "";
        }
        if (rule.contains(".js:")) {
            return getAndTextByRule(elementt, rule, "＋");
        } else {
            if (rule.contains("＋")) {
                return getAndTextByRule(elementt, rule, "＋");
            }
            return getAndTextByRule(elementt, rule, "\\+");
        }
    }

    private static String getAndTextByRule(Element elementt, String rule, String fs) {
        String[] rules = rule.split(fs);
        List<String> contents = new ArrayList<>();
        for (String s : rules) {
            s = s.trim();
            if ((s.startsWith("'") && s.endsWith("'"))
                    || (s.startsWith("\"") && s.endsWith("\""))) {
                contents.add(s.substring(1, s.length() - 1).replace("\\n", "\n"));
            } else {
                contents.add(CommonParser.getContentWithoutAnd(elementt, s));
            }
        }
        return StringUtil.listToString(contents, "");
    }

    private static String getContentWithoutAnd(Element elementt, String rule) {
        String[] ss3 = rule.split("&&");
        Element element2;
        if (ss3.length == 1) {
            element2 = elementt;
        } else {
            element2 = CommonParser.getTrueElement(ss3[0], elementt);
        }
        for (int i = 1; i < ss3.length - 1; i++) {
            element2 = CommonParser.getTrueElement(ss3[i], element2);
        }
        return CommonParser.getText(element2, ss3[ss3.length - 1]);
    }

    public static String getText(Element element, String lastRule) {
        if ("*".equals(lastRule)) {
            return "null";
        }
        String[] ors = lastRule.split("\\|\\|");
        if (ors.length > 1) {
            for (int i = 0; i < ors.length; i++) {
                String e = null;
                try {
                    e = getTextWithoutOr(element, ors[i]);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                if (!TextUtils.isEmpty(e)) {
                    return e;
                }
            }
        }
        return getTextWithoutOr(element, lastRule);
    }

    private static String getTextWithoutOr(Element element, String lastRule) {
        String js = "";
        String[] ss = lastRule.split("\\.js:");
        if (ss.length > 1) {
            lastRule = ss[0];
            js = StringUtil.arrayToString(ss, 1, ss.length, ".js:");
        }
        String[] rules = lastRule.split("!");
        String text;
        if (rules.length > 1) {
            if ("Text".equals(rules[0])) {
                text = element.text();
            } else if ("Html".equals(rules[0])) {
                text = element.html();
            } else if (rules[0].contains("Attr")) {
                text = element.attr(rules[0].replace("Attr", ""));
            } else {
                text = element.attr(rules[0]);
            }
            if (!"Html".equals(lastRule)) {
                text = text.replaceAll("\n", " ");
            }
            for (int i = 1; i < rules.length; i++) {
                text = text.replace(rules[i], "");
            }
        } else {
            if ("Text".equals(lastRule)) {
                text = element.text();
            } else if ("Html".equals(lastRule)) {
                text = element.html();
            } else if (lastRule.contains("Attr")) {
                text = element.attr(lastRule.replace("Attr", ""));
            } else {
                text = element.attr(lastRule);
            }
            if (!"Html".equals(lastRule)) {
                text = text.replaceAll("\n", " ");
            }
        }
        if (StringUtil.isNotEmpty(js)) {
            try {
                text = JSEngine.getInstance().evalJS(js, text);
            } catch (Exception ignored) {
            }
        }
        return text;
    }

    public static String getUrl(Element element3, String lastRule, MovieRule movieRule, String lastUrl) {
        if ("*".equals(lastRule)) {
            return "null";
        }
        String[] ors = lastRule.split("\\|\\|");
        if (ors.length > 1) {
            for (int i = 0; i < ors.length; i++) {
                String e = null;
                try {
                    e = getUrlWithoutOr(element3, ors[i], movieRule, lastUrl);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                if (!TextUtils.isEmpty(e)) {
                    return e;
                }
            }
        }
        //        Log.d(TAG, "getUrl getUrlWithoutOr: " + url);
        return getUrlWithoutOr(element3, lastRule, movieRule, lastUrl);
    }

    private static String getUrlWithoutOr(Element element3, String lastRule, MovieRule movieRule, String lastUrl) {
        String js = "";
        String[] ss = lastRule.split("\\.js:");
        if (ss.length > 1) {
            lastRule = ss[0];
            js = StringUtil.arrayToString(ss, 1, ss.length, ".js:");
//            Log.d(TAG, "getUrlWithoutOr: " + js);
        }
        if(element3 == null){
            return "";
        }
        String url;
//        String[] rules = lastRule.split("@js:");
        if (lastRule.startsWith("Text")) {
            url = element3.text();
        } else if ("Html".equals(lastRule)) {
            url = element3.html();
        } else if (lastRule.startsWith("AttrNo")) {
            url = element3.attr(lastRule.replaceFirst("AttrNo", ""));
            return movieRule.getBaseUrl() + url;
        } else if (lastRule.startsWith("AttrYes")) {
            url = element3.attr(lastRule.replaceFirst("AttrYes", ""));
        } else if (lastRule.startsWith("Attr")) {
            url = element3.attr(lastRule.replaceFirst("Attr", ""));
        } else {
            url = element3.attr(lastRule);
//            url = element3.select(lastRule).first().toString();
        }
        if (TextUtils.isEmpty(js)) {
            if (!"Html".equals(lastRule)) {
                url = StringUtil.trimBlanks(url);
            }
        } else {
            try {
//                Log.d(TAG, "getUrlWithoutOr: " + url);
                url = JSEngine.getInstance().evalJS(js, url);
//                Log.d(TAG, "getUrlWithoutOr2: " + url);
                if (url.toLowerCase().trim().startsWith("http")
                        || url.toLowerCase().trim().startsWith("x5://")) {
//                    Log.d(TAG, "getUrlWithoutOr2 startsWith: ");
                    return url.trim();
                }
            } catch (Exception e) {
                if (!"Html".equals(lastRule)) {
                    url = StringUtil.trimBlanks(url);
                }
                e.printStackTrace();
            }
        }
        if (StringUtil.isEmpty(url)) {
            return "";
        }
        if ("Html".equals(lastRule)) {
            return url;
        }
        if (url.startsWith("http")) {
            return url;
        } else if (url.startsWith("//")) {
            return "http:" + url;
        } else if (url.startsWith("magnet") || url.startsWith("thunder") || url.startsWith("ftp") || url.startsWith("ed2k")) {
            return url;
        } else if (url.startsWith("/") || url.startsWith("./") || url.startsWith("../") || url.startsWith("?")) {
            return joinUrl(lastUrl, url);
        } else {
            String[] urls = url.split("\\$");
            if (urls.length > 1 && urls[1].startsWith("http")) {
                return urls[1];
            }
            if (url.contains("url(")) {
                String[] urls2 = url.split("url\\(");
                if (urls2.length > 1 && urls2[1].startsWith("http")) {
                    return urls2[1].split("\\)")[0];
                }
            }
            return joinUrl(lastUrl, url);
        }
    }

    private static String joinUrl(String parent, String child) {
        if (StringUtil.isEmpty(parent)) {
            return child;
        }
        URL url;
        String q = parent;
        try {
            url = new URL(new URL(parent), child);
            q = url.toExternalForm();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
//        if (q.contains("#")) {
//            q = q.replaceAll("^(.+?)#.*?$", "$1");
//        }
        return q;
    }

    public static String parseDomForUrl(String html, String rule, String movieUrl) {
        Document doc = Jsoup.parse(html);
        String[] ss4 = rule.split("&&");
        Element element3;
        if (ss4.length == 1) {
            element3 = doc;
        } else {
            element3 = CommonParser.getTrueElement(ss4[0], doc);
        }
        for (int i = 1; i < ss4.length - 1; i++) {
            element3 = CommonParser.getTrueElement(ss4[i], element3);
        }
        MovieRule movieRule = new MovieRule();
        movieUrl = HttpParser.getFirstPageUrl(movieUrl);
        movieRule.setBaseUrl(StringUtil.getBaseUrl(movieUrl));
        return CommonParser.getUrl(element3, ss4[ss4.length - 1], movieRule, movieUrl);
    }

    public static List<String> parseDomForList(String html, String rule) {
        Document doc = Jsoup.parse(html);
        String[] ss2 = rule.split("&&");
        //循环获取
        Elements elements = new Elements();
        Element element;
        element = CommonParser.getTrueElement(ss2[0], doc);
        for (int i = 1; i < ss2.length - 1; i++) {
            element = CommonParser.getTrueElement(ss2[i], element);
        }
        elements.addAll(CommonParser.selectElements(element, ss2[ss2.length - 1]));
        List<String> eleHtml = new ArrayList<>();
        for (Element element1 : elements) {
            eleHtml.add(element1.outerHtml());
        }
        return eleHtml;
    }


    public static List<String> parseDomForHtmlList(String html, String listRule, String htmlRule) {
        Document doc = Jsoup.parse(html);
        String[] ss2 = listRule.split("&&");
        //循环获取
        Elements elements = new Elements();
        Element element;
        element = CommonParser.getTrueElement(ss2[0], doc);
        for (int i = 1; i < ss2.length - 1; i++) {
            element = CommonParser.getTrueElement(ss2[i], element);
        }
        elements.addAll(CommonParser.selectElements(element, ss2[ss2.length - 1]));
        String[] ss4 = htmlRule.split("&&");
        List<String> eleHtml = new ArrayList<>();
        for (Element element1 : elements) {
            MovieRule movieRule = new MovieRule();
            movieRule.setBaseUrl("");
            Element element3;
            if (ss4.length == 1) {
                element3 = element1;
            } else {
                element3 = getTrueElement(ss4[0], element1);
            }
            for (int i = 1; i < ss4.length - 1; i++) {
                element3 = getTrueElement(ss4[i], element3);
            }
            eleHtml.add(getUrl(element3, ss4[ss4.length - 1], movieRule, ""));
        }
        return eleHtml;
    }

    public static String parsePageClassUrl(String urlWithUa, int page, ArticleListRule articleListRule) {
        if (StringUtil.isEmpty(urlWithUa)) {
            return urlWithUa;
        }
        if (articleListRule == null) {
            articleListRule = new ArticleListRule();
        }
        if (articleListRule.getClass_url() == null) {
            articleListRule.setClass_url("");
        }
        if (articleListRule.getYear_url() == null) {
            articleListRule.setYear_url("");
        }
        if (articleListRule.getArea_url() == null) {
            articleListRule.setArea_url("");
        }
        if (articleListRule.getSort_url() == null) {
            articleListRule.setSort_url("");
        }
        String[] allUrl = urlWithUa.split(";");
        String url;
        String[] urls = allUrl[0].split("\\[firstPage=");
        if (page == 1 && urls.length > 1) {
            url = urls[1].split("]")[0];
        } else if (urls[0].contains("fypage@")) {
            //fypage@-1@*2@/fyclass
            String[] strings = urls[0].split("fypage@");
            String[] pages = strings[1].split("@");
            for (int i = 0; i < pages.length - 1; i++) {
                if (pages[i].startsWith("-")) {
                    page = page - Integer.parseInt(pages[i].replace("-", ""));
                } else if (pages[i].startsWith("+")) {
                    page = page + Integer.parseInt(pages[i].replace("+", ""));
                } else if (pages[i].startsWith("*")) {
                    page = page * Integer.parseInt(pages[i].replace("*", ""));
                } else if (pages[i].startsWith("/")) {
                    page = page / Integer.parseInt(pages[i].replace("/", ""));
                }
            }
            //前缀 + page + 后缀
            url = strings[0] + page + pages[pages.length - 1];
        } else {
            url = urls[0].replace("fypage", page + "");
        }
        if (url.contains("fyAll")) {
            if ("class".equals(articleListRule.getFirstHeader())) {
                url = url.replace("fyAll", articleListRule.getClass_url());
            } else if ("area".equals(articleListRule.getFirstHeader())) {
                url = url.replace("fyAll", articleListRule.getArea_url());
            } else if ("year".equals(articleListRule.getFirstHeader())) {
                url = url.replace("fyAll", articleListRule.getYear_url());
            } else if ("sort".equals(articleListRule.getFirstHeader())) {
                url = url.replace("fyAll", articleListRule.getSort_url());
            } else {
                url = url.replace("fyAll", articleListRule.getClass_url());
            }
        } else {
            url = url.replace("fyclass", articleListRule.getClass_url())
                    .replace("fyyear", articleListRule.getYear_url())
                    .replace("fyarea", articleListRule.getArea_url())
                    .replace("fysort", articleListRule.getSort_url());
        }
        StringBuilder builder = new StringBuilder(url);
        if (allUrl.length > 1) {
            for (int i = 1; i < allUrl.length; i++) {
                builder.append(";").append(allUrl[i]);
            }
        }
        return builder.toString();
    }
}
