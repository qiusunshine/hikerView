package com.example.hikerview.service.parser;

import android.app.Activity;
import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.annimon.stream.function.Consumer;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.model.article.extra.X5Extra;
import com.example.hikerview.utils.StringUtil;

import org.adblockplus.libadblockplus.android.Utils;

/**
 * 作者：By 15968
 * 日期：On 2021/11/18
 * 时间：At 17:01
 */

public abstract class IWebViewParser {

    protected Consumer<String> consumer;

    public abstract void destroy();

    public abstract boolean parse(Activity activity, String url, String extra, Consumer<String> consumer);


    public abstract boolean finishParseInner(Context context, String url);

    protected X5Extra generateExtra(String extra, String innerJs, String mode, String ticket) {
        X5Extra x5Extra = new X5Extra();
        if (StringUtil.isNotEmpty(extra)) {
            X5Extra x5 = JSON.parseObject(extra, X5Extra.class);
            if (x5 != null) {
                //继承item的extra属性
                if (StringUtil.isNotEmpty(x5.getUa())) {
                    x5Extra.setUa(x5.getUa());
                }
                if (CollectionUtil.isNotEmpty(x5.getBlockRules())) {
                    x5Extra.setBlockRules(x5.getBlockRules());
                }
                if (x5.isDisableX5()) {
                    x5Extra.setDisableX5(true);
                }
            }
        }
        x5Extra.setJs(generateExtraJS(innerJs, mode, ticket));
        return x5Extra;
    }

    private String generateExtraJS(String innerJs, String mode, String ticket) {
        return "function getUrl() {\n" +
                "        return eval('" + Utils.escapeJavaScriptString(innerJs) + "');" +
                "      }\n" +
                "            var cc = 0;" +
                "            function check() {\n" +
                "                if (cc > 120){" +
                "                   fy_bridge_app.finishParse('', '" + mode + "', '" + ticket + "');\n" +
                "                   return;" +
                "                }" +
                "                cc++;" +
                "                setTimeout(() => {\n" +
                "                    var url = getUrl();\n" +
                "                    if (url != null) {\n" +
                "                        fy_bridge_app.finishParse(url, '" + mode + "', '" + ticket + "');\n" +
                "                    } else {\n" +
                "                        check();\n" +
                "                    }\n" +
                "                }, 250)\n" +
                "            }\n" +
                "            check();";
    }
}
