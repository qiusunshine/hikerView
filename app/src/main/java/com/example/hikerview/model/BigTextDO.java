package com.example.hikerview.model;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

/**
 * 作者：By 15968
 * 日期：On 2020/1/3
 * 时间：At 21:37
 */
public class BigTextDO extends LitePalSupport {

    public static final String JS_LIST_ORDER_KEY = "js_list_order";
    public static final String ARTICLE_LIST_ORDER_KEY = "article_list_order";
    public static final String BOOKMARK_ORDER_KEY = "bookmark_order";
    public static final String VIDEO_RULES_KEY = "video_rules";
    public static final String xiuTanDialogBlackListPath = "xiuTanDialogBlackList.json";
    public static final String JS_ENABLE_MAP_KEY = "jsEnableMap";
    public static final String SEARCH_LIST_ORDER_KEY = "search_rules_order";
    public static final String PUBLISH_CODE_KEY = "publish_code";
    public static final String PUBLISH_ACCOUNT_KEY = "publish_account_code";
    private static final String HOME_JS_KEY = "home_js";
    private static final String HOME_SUB_KEY = "home_sub";

    private String key;
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static String getHomeJs() {
        BigTextDO bigTextDO = LitePal.where("key = ?", HOME_JS_KEY).findFirst(BigTextDO.class);
        if (bigTextDO == null) {
            return null;
        }
        return bigTextDO.getValue();
    }

    public static void updateHomeJs(String value) {
        BigTextDO bigTextDO = LitePal.where("key = ?", HOME_JS_KEY).findFirst(BigTextDO.class);
        if (bigTextDO == null) {
            bigTextDO = new BigTextDO();
            bigTextDO.setKey(HOME_JS_KEY);
        }
        bigTextDO.setValue(value);
        bigTextDO.save();
    }


    public static String getHomeSub() {
        BigTextDO bigTextDO = LitePal.where("key = ?", HOME_SUB_KEY).findFirst(BigTextDO.class);
        if (bigTextDO == null) {
            return null;
        }
        return bigTextDO.getValue();
    }

    public static void updateHomeSub(String value) {
        BigTextDO bigTextDO = LitePal.where("key = ?", HOME_SUB_KEY).findFirst(BigTextDO.class);
        if (bigTextDO == null) {
            bigTextDO = new BigTextDO();
            bigTextDO.setKey(HOME_SUB_KEY);
        }
        bigTextDO.setValue(value);
        bigTextDO.save();
    }
}
