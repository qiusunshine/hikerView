package com.example.hikerview.service.parser;

import android.text.TextUtils;

import com.example.hikerview.model.MovieRule;
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
        } else if (url.startsWith("/")) {
            if (StringUtil.isEmpty(movieRule.getBaseUrl())) {
                return url;
            } else if (movieRule.getBaseUrl().endsWith("/")) {
                return movieRule.getBaseUrl().substring(0, movieRule.getBaseUrl().length() - 1) + url;
            } else {
                return movieRule.getBaseUrl() + url;
            }
        } else if (url.startsWith("./")) {
            String searchUrl = movieRule.getSearchUrl().split(";")[0];
            String[] c = searchUrl.split("/");
            if (c.length <= 1) {
                return url;
            }
            String sub = searchUrl.replace(c[c.length - 1], "");
            return sub + url.replace("./", "");
        } else if (url.startsWith("?")) {
            return lastUrl + url;
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
            if (StringUtil.isEmpty(movieRule.getBaseUrl())) {
                return url;
            } else if (movieRule.getBaseUrl().endsWith("/")) {
                return movieRule.getBaseUrl() + url;
            } else {
                return lastUrl + "/" + url;
            }
        }
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
}
