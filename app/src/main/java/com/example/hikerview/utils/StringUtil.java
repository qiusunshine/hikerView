package com.example.hikerview.utils;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：By hdy
 * 日期：On 2018/11/12
 * 时间：At 12:03
 */
public class StringUtil {

    public final static String[] LOWER_CASES = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    public final static String[] UPPER_CASES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public final static String[] NUMS_LIST = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public final static String[] SYMBOLS_ARRAY = {"!", "~", "^", "_", "*"};

    public static String genRandomPwd(int pwd_len) {
        return genRandomPwd(pwd_len, false);
    }

    /**
     * 生成随机密码
     *
     * @param pwd_len 密码长度
     * @param simple  简单模式
     * @return 密码的字符串
     */
    public static String genRandomPwd(int pwd_len, boolean simple) {
        if (pwd_len < 6 || pwd_len > 20) {
            return "";
        }
        int lower, upper, num = 0, symbol = 0;
        lower = pwd_len / 2;

        if (simple) {
            upper = pwd_len - lower;
        } else {
            upper = (pwd_len - lower) / 2;
            num = (pwd_len - lower) / 2;
            symbol = pwd_len - lower - upper - num;
        }

        StringBuilder pwd = new StringBuilder();
        Random random = new Random();
        int position = 0;
        while ((lower + upper + num + symbol) > 0) {
            if (lower > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, LOWER_CASES[random.nextInt(LOWER_CASES.length)]);
                lower--;
            }
            if (upper > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, UPPER_CASES[random.nextInt(UPPER_CASES.length)]);
                upper--;
            }
            if (num > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, NUMS_LIST[random.nextInt(NUMS_LIST.length)]);
                num--;
            }
            if (symbol > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, SYMBOLS_ARRAY[random.nextInt(SYMBOLS_ARRAY.length)]);
                symbol--;
            }

            System.out.println(pwd.toString());
        }
        return pwd.toString();
    }

    public static String arrayToString(String[] list, int fromIndex, String cha) {
        return arrayToString(list, fromIndex, list == null ? 0 : list.length, cha);
    }

    public static String arrayToString(String[] list, int fromIndex, int endIndex, String cha) {
        StringBuilder builder = new StringBuilder();
        if (list == null || list.length <= fromIndex) {
            return "";
        } else if (list.length <= 1) {
            return list[0];
        } else {
            builder.append(list[fromIndex]);
        }
        for (int i = 1 + fromIndex; i < list.length && i < endIndex; i++) {
            builder.append(cha).append(list[i]);
        }
        return builder.toString();
    }


    public static String listToString(List<String> list, String cha) {
        StringBuilder builder = new StringBuilder();
        if (list == null || list.size() <= 0) {
            return "";
        } else if (list.size() <= 1) {
            return list.get(0);
        } else {
            builder.append(list.get(0));
        }
        for (int i = 1; i < list.size(); i++) {
            builder.append(cha).append(list.get(i));
        }
        return builder.toString();
    }

    public static String listToString(List<String> list, int fromIndex, String cha) {
        StringBuilder builder = new StringBuilder();
        if (list == null || list.size() <= fromIndex) {
            return "";
        } else if (list.size() <= 1) {
            return list.get(0);
        } else {
            builder.append(list.get(fromIndex));
        }
        for (int i = fromIndex + 1; i < list.size(); i++) {
            builder.append(cha).append(list.get(i));
        }
        return builder.toString();
    }

    public static String listToString(List<String> list) {
        return listToString(list, "&&");
    }

    public static String replaceBlank(String str) {
        try {
            String dest = "";
            if (str != null) {
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(str);
                dest = m.replaceAll("");
            }
            return dest;
        } catch (Exception e) {
            return str;
        }
    }

    public static String replaceLineBlank(String str) {
        try {
            return str.replaceAll("\n", "");
        } catch (Exception e) {
            return str;
        }
    }

    public static String trimBlanks(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        int len = str.length();
        int st = 0;

        while ((st < len) && (str.charAt(st) == '\n' || str.charAt(st) == '\r' || str.charAt(st) == '\f' || str.charAt(st) == '\t')) {
            st++;
        }
        while ((st < len) && (str.charAt(len - 1) == '\n' || str.charAt(len - 1) == '\r' || str.charAt(len - 1) == '\f' || str.charAt(len - 1) == '\t')) {
            len--;
        }
        return ((st > 0) || (len < str.length())) ? str.substring(st, len) : str;
    }

    public static boolean equalsDomUrl(String url1, String url2) {
        if (url1 == null) {
            return url2 == null;
        }
        if (url2 == null) {
            return false;
        }
        String pUrl = url1;
        if (pUrl.endsWith("/")) {
            pUrl = pUrl.substring(0, pUrl.length() - 1);
        }
        String sUrl = url2;
        if (sUrl.endsWith("/")) {
            sUrl = sUrl.substring(0, sUrl.length() - 1);
        }
        return pUrl.equals(sUrl);
    }

    public static String getHomeUrl(String url) {
        if (isEmpty(url)) {
            return url;
        } else {
            String dom = getDom(url);
            if (url.startsWith("https")) {
                return "https://" + dom + "/";
            } else {
                return "http://" + dom + "/";
            }
        }
    }


    public static boolean isHexStr(String str) {
        boolean flag = false;
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (!str.startsWith("#")) {
            str = "#" + str;
        }
        if (str.length() != 7 && str.length() != 9) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            char cc = str.charAt(i);
            if (cc == '0' || cc == '1' || cc == '2' || cc == '3' || cc == '4' || cc == '5' || cc == '6' || cc == '7' || cc == '8' || cc == '9' || cc == 'A' || cc == 'B' || cc == 'C' ||
                    cc == 'D' || cc == 'E' || cc == 'F' || cc == 'a' || cc == 'b' || cc == 'c' || cc == 'd' || cc == 'e' || cc == 'f') {
                flag = true;
            }
        }
        return flag;
    }

    // 判断一个字符是否是中文
    private static boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;// 根据字节码判断
    }

    // 判断一个字符串是否含有中文
    private static boolean containsChinese(String str) {
        if (str == null)
            return false;
        for (char c : str.toCharArray()) {
            if (isChinese(c))
                return true;
        }
        return false;
    }

    public static String decodeConflictStr(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("？？", "?").replace("＆＆", "&").replace("；；", ";");
    }

    public static boolean isUrl(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (isWebUrl(str)) {
            return true;
        }
        return !containsChinese(str) && str.contains(".") && !str.contains(" ");
    }

    public static String getDom(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        try {
            url = url.replaceFirst("http://", "").replaceFirst("https://", "");
            String[] urls = url.split("/");
            if (urls.length > 0) {
                return urls[0];
            }
        } catch (Exception e) {
            return null;
        }
        return url;
    }

    public static String removeDom(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        try {
            url = url.replaceFirst("http://", "").replaceFirst("https://", "");
            String[] urls = url.split("/");
            if (urls.length > 1) {
                return arrayToString(urls, 1, "/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String getSimpleDom(String url) {
        String dom = getDom(url);
        if (StringUtil.isEmpty(url) || StringUtil.isEmpty(dom) || url.equals(dom)) {
            return url;
        }
        String[] s = dom.split("\\.");
        if (s.length < 3) {
            return dom;
        }
        return dom.substring(dom.indexOf(".", s.length - 2) + 1);
    }

    /**
     * 多关键字查询表红,避免后面的关键字成为特殊的HTML语言代码
     *
     * @param str    检索结果
     * @param inputs 关键字集合
     * @param resStr 表红后的结果
     */
    public static StringBuilder spannableString(String str, List<String> inputs, StringBuilder resStr) {
        int index = str.length();//用来做为标识,判断关键字的下标
        String next = "";//保存str中最先找到的关键字
        for (int i = inputs.size() - 1; i >= 0; i--) {
            String theNext = inputs.get(i);
            int theIndex = str.indexOf(theNext);
            if (theIndex == -1) {//过滤掉无效关键字
                inputs.remove(i);
            } else if (theIndex < index) {
                index = theIndex;//替换下标
                next = theNext;
            }
        }
        //如果条件成立,表示串中已经没有可以被替换的关键字,否则递归处理
        if (index == str.length()) {
            resStr.append(str);
        } else {
            resStr.append(str.substring(0, index));
            resStr.append("<font color='#FF0000'>").append(str.substring(index, index + next.length())).append("</font>");
            String str1 = str.substring(index + next.length(), str.length());
            spannableString(str1, inputs, resStr);//剩余的字符串继续替换
        }
        return resStr;
    }

    /**
     * 转义正则特殊字符 （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return keyword
     */
    public static String escapeExprSpecialWord(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (keyword.contains(key)) {
                    keyword = keyword.replace(key, "\\" + key);
                }
            }
        }
        return keyword;
    }

    /**
     * 删除正则特殊字符 （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return keyword
     */

    public static String removeSpecialWord(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (keyword.contains(key)) {
                    keyword = keyword.replace(key, "");
                }
            }
        }
        return keyword;
    }

    public static String getBaseUrl(String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        String baseUrls = url.replace("http://", "").replace("https://", "");
        String baseUrl2 = baseUrls.split("/")[0];
        String baseUrl;
        if (url.startsWith("https")) {
            baseUrl = "https://" + baseUrl2;
        } else {
            baseUrl = "http://" + baseUrl2;
        }
        return baseUrl;
    }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(@Nullable CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isUTF8(String str) {
        try {
            str.getBytes("utf-8");
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        }

    }


    public static String convertBlankToTagP(String content) {
        try {
            if (StringUtil.isEmpty(content)) {
                return content;
            } else if (!content.contains("\n")) {
                return content;
            } else {
                return content.replace("\n", "<br>");
            }
        } catch (Exception e) {
            return content;
        }
    }


    public static String simplyGroup(String title) {
        if (isEmpty(title)) {
            return title;
        }
        String rTitle = title.replace("①", "")
                .replace("②", "")
                .replace("③", "")
                .replace("④", "")
                .replace("⑤", "")
                .replace("⑥", "")
                .replace("⑦", "")
                .replace("⑧", "")
                .replace("⑨", "")
                .replace("⑩", "");
        String[] rTitleSplit = rTitle.split("@@");
        if (rTitleSplit.length > 0) {
            rTitle = rTitleSplit[0];
        }
        return rTitle;
    }

    /**
     * 判读是否是emoji
     *
     * @param codePoint
     * @return
     */
    public static boolean getIsEmoji(char codePoint) {
        if ((codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA)
                || (codePoint == 0xD)
                || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
                || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)))
            return false;
        return true;
    }


    public static boolean getIsSp(char codePoint) {
        if (Character.getType(codePoint) > Character.LETTER_NUMBER) {
            return true;
        }
        return false;
    }

    /**
     * 判断搜索框内容是否包含特殊字符
     *
     * @param str
     * @return
     */
    public static boolean hasSpWord(String str) {
        String limitEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@①#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern pattern = Pattern.compile(limitEx);
        Matcher m = pattern.matcher(str);
        return m.find();
    }

    /**
     * 判断是否只含英文数字
     *
     * @param str
     * @return
     */
    public static boolean isLetterDigit(String str) {
        String regex = "^[a-z0-9A-Z\\-_]+$";
        return str.matches(regex);
    }

    public static boolean isWebUrl(String str) {
        if (isEmpty(str)) {
            return false;
        }
        String url = str.toLowerCase();
        return url.startsWith("http") || url.startsWith("file://") || url.startsWith("ftp");
    }


    public static String autoFixUrl(String bUrl, String url) {
        if (isEmpty(bUrl) || isEmpty(url)) {
            return url;
        }
        bUrl = bUrl.split(";")[0];
        String baseUrl = getBaseUrl(bUrl);
        String lowUrl = url.toLowerCase();
        if (lowUrl.startsWith("http") || lowUrl.startsWith("hiker") || lowUrl.startsWith("pics") || lowUrl.startsWith("code")) {
            return url;
        } else if (url.startsWith("//")) {
            return "http:" + url;
        } else if (url.startsWith("magnet") || url.startsWith("thunder") || url.startsWith("ftp") || url.startsWith("ed2k")) {
            return url;
        } else if (url.startsWith("/")) {
            if (baseUrl.endsWith("/")) {
                return baseUrl.substring(0, baseUrl.length() - 1) + url;
            } else {
                return baseUrl + url;
            }
        } else if (url.startsWith("./")) {
            String[] protocolUrl = bUrl.split("://");
            if (protocolUrl.length < 1) {
                return url;
            }
            String[] c = protocolUrl[1].split("/");
            if (c.length <= 1) {
                if (baseUrl.endsWith("/")) {
                    return baseUrl.substring(0, baseUrl.length() - 1) + url.replace("./", "");
                } else {
                    return baseUrl + url.replace("./", "");
                }
            }
            String sub = protocolUrl[1].replace(c[c.length - 1], "");
            return protocolUrl[0] + "://" + sub + url.replace("./", "");
        } else if (url.startsWith("?")) {
            return bUrl + url;
        } else {
            return url;
        }
    }

    public static String[] splitUrlByQuestionMark(String url) {
        if (isEmpty(url)) {
            return new String[]{url};
        } else {
            String[] urls = url.split("\\?");
            if (urls.length <= 1) {
                return urls;
            } else {
                String[] res = new String[2];
                res[0] = urls[0];
                res[1] = arrayToString(urls, 1, "?");
                return res;
            }
        }
    }
}
