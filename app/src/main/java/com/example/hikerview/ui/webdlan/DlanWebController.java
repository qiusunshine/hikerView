package com.example.hikerview.ui.webdlan;

import com.alibaba.fastjson.JSON;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.example.hikerview.constants.ArticleColTypeEnum;
import com.example.hikerview.constants.JSONPreFilter;
import com.example.hikerview.constants.PreferenceConstant;
import com.example.hikerview.constants.UAEnum;
import com.example.hikerview.event.OnArticleListRuleChangedEvent;
import com.example.hikerview.event.video.PlayChapterEvent;
import com.example.hikerview.model.BigTextDO;
import com.example.hikerview.ui.Application;
import com.example.hikerview.ui.browser.model.JSManager;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.home.model.ArticleListPageRule;
import com.example.hikerview.ui.home.model.ArticleListRule;
import com.example.hikerview.ui.home.model.ArticleListRuleJO;
import com.example.hikerview.ui.js.model.JsRule;
import com.example.hikerview.ui.video.VideoPlayerActivity;
import com.example.hikerview.ui.webdlan.model.JsDTO;
import com.example.hikerview.utils.FilterUtil;
import com.example.hikerview.utils.PreferenceMgr;
import com.example.hikerview.utils.StringUtil;
import com.yanzhenjie.andserver.annotation.Controller;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.ResponseBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.RequestBody;
import com.yanzhenjie.andserver.util.StatusCode;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 作者：By hdy
 * 日期：On 2019/3/19
 * 时间：At 0:33
 */
@Controller
public class DlanWebController {

    @GetMapping(path = "/getRuleContent")
    @ResponseBody
    String getRuleContent(@RequestParam(name = "title") String title) {
        List<ArticleListRule> articleListRules = null;
        try {
            articleListRules = LitePal.where("title = ?", title).limit(1).find(ArticleListRule.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtil.isEmpty(articleListRules)) {
            return JSON.toJSONString(articleListRules.get(0));
        } else {
            return "";
        }
    }

    @GetMapping(path = "/getAllRuleTitles")
    @ResponseBody
    String getAllRuleTitles() {
        List<ArticleListRule> articleListRules = new ArrayList<>();
        try {
            articleListRules = LitePal.findAll(ArticleListRule.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtil.isEmpty(articleListRules)) {
            return JSON.toJSONString(Stream.of(articleListRules).map(ArticleListRule::getTitle).collect(Collectors.toList()));
        } else {
            return "[]";
        }
    }

    @GetMapping(path = "/getAllJsTitles")
    @ResponseBody
    String getAllJsTitles() {
        List<JsRule> jsRules = JSManager.instance(Application.getContext()).listAllJsFileNames();
        if (!CollectionUtil.isEmpty(jsRules)) {
            return JSON.toJSONString(Stream.of(jsRules).map(JsRule::getName).collect(Collectors.toList()));
        } else {
            return "[]";
        }
    }

    @GetMapping(path = "/getColTypes")
    @ResponseBody
    String getColTypes() {
        return JSON.toJSONString(ArticleColTypeEnum.getCodeArray());
    }

    @GetMapping(path = "/getJsContent")
    @ResponseBody
    String getJsContent(@RequestParam(name = "name") String name) {
        String js = JSManager.instance(Application.getContext()).getJsByFileName(name);
        if (StringUtil.isEmpty(js)) {
            return "";
        } else {
            return js;
        }
    }

    @PostMapping(path = "/saveJs")
    @ResponseBody
    String saveJs(RequestBody body) {
        try {
            String data = body.string();
            JsDTO jsDTO = JSON.parseObject(data, JsDTO.class);
            if (jsDTO == null || StringUtil.isEmpty(jsDTO.getName())) {
                return "{\"isSuccess\":false, \"errorMsg\":\"解析数据失败，插件名称不能为空\"}";
            }
            JSManager.instance(Application.getContext()).updateJs(jsDTO.getName(), jsDTO.getContent());
            return "{\"isSuccess\":true}";
        } catch (IOException e) {
            return "{\"isSuccess\":false, \"errorMsg\":\"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping(path = "/playUrl")
    @ResponseBody
    String getUrl(@RequestParam(name = "enhance", required = false) boolean enhance) {
        String url = LocalServerParser.getRealUrlForRemotedPlay(Application.getContext(), RemoteServerManager.instance().getPlayUrl());
        if (!enhance) {
            return url;
        } else {
            return JSON.toJSONString(RemoteServerManager.instance().getUrlDTO());
        }
    }

    @GetMapping(path = "/getPlayList")
    @ResponseBody
    String getPlayList() {
        List<String> chapters = VideoPlayerActivity.getChapters();
        return JSON.toJSONString(chapters);
    }

    @GetMapping(path = "/playMe")
    @ResponseBody
    String playMe(@RequestParam(name = "title") String title, @RequestParam(name = "index") String index) {
        try {
            PlayChapterEvent event = new PlayChapterEvent(Integer.parseInt(index), title);
            if (!EventBus.getDefault().hasSubscriberForEvent(event.getClass())) {
                return Boolean.FALSE.toString();
            } else {
                EventBus.getDefault().post(event);
                return Boolean.TRUE.toString();
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return Boolean.FALSE.toString();
        }
    }

    @GetMapping(path = "/playNext")
    @ResponseBody
    String playNext() {
        try {
            PlayChapterEvent event = new PlayChapterEvent(-1, null);
            if (!EventBus.getDefault().hasSubscriberForEvent(event.getClass())) {
                return Boolean.FALSE.toString();
            } else {
                EventBus.getDefault().post(event);
                return Boolean.TRUE.toString();
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return Boolean.FALSE.toString();
        }
    }

    @GetMapping(path = "/checkRuleExist")
    @ResponseBody
    String checkRuleExist(@RequestParam(name = "title") String title) {
        List<ArticleListRule> articleListRules = null;
        try {
            articleListRules = LitePal.where("title = ?", title).limit(1).find(ArticleListRule.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtil.isEmpty(articleListRules)) {
            return Boolean.TRUE.toString();
        } else {
            return Boolean.FALSE.toString();
        }
    }

    @PostMapping(path = "/saveRule")
    @ResponseBody
    String saveRule(RequestBody body) {
        try {
            String data = body.string();
            ArticleListRuleJO ruleJO = JSON.parseObject(data, ArticleListRuleJO.class);
            if (ruleJO == null || StringUtil.isEmpty(ruleJO.getTitle())) {
                return "{\"isSuccess\":false, \"errorMsg\":\"解析数据失败，标题不能为空\"}";
            }
            ArticleListRule articleListRule = new ArticleListRule().fromJO(ruleJO);
            if (StringUtil.isEmpty(articleListRule.getUa())) {
                articleListRule.setUa(UAEnum.MOBILE.getCode());
            }
            if (articleListRule.getArea_url() != null && articleListRule.getArea_name() != null) {
                if (FilterUtil.hasFilterWord(articleListRule.getArea_name())) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"含有违禁词\"}";
                }
                if (articleListRule.getArea_url().split("&").length != articleListRule.getArea_name().split("&").length) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"地区名称和地区替换词长度不一致\"}";
                }
            }
            if (articleListRule.getYear_url() != null && articleListRule.getYear_name() != null) {
                if (FilterUtil.hasFilterWord(articleListRule.getYear_name())) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"含有违禁词\"}";
                }
                if (articleListRule.getYear_url().split("&").length != articleListRule.getYear_name().split("&").length) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"年份名称和年份替换词长度不一致\"}";
                }
            }
            if (articleListRule.getClass_url() != null && articleListRule.getClass_name() != null) {
                if (FilterUtil.hasFilterWord(articleListRule.getClass_name())) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"含有违禁词\"}";
                }
                if (articleListRule.getClass_name().split("&").length != articleListRule.getClass_url().split("&").length) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"分类名称和分类替换词长度不一致\"}";
                }
            }
            if (articleListRule.getSort_url() != null && articleListRule.getSort_name() != null) {
                if (FilterUtil.hasFilterWord(articleListRule.getSort_name())) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"含有违禁词\"}";
                }
                if (articleListRule.getSort_name().split("&").length != articleListRule.getSort_url().split("&").length) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"排序名称和排序替换词长度不一致\"}";
                }
            }
            List<ArticleListRule> articleListRules = null;
            try {
                articleListRules = LitePal.where("title = ?", articleListRule.getTitle()).limit(1).find(ArticleListRule.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtil.isEmpty(articleListRules)) {
                //4部曲，4，更新到库
                ArticleListRule rule = articleListRules.get(0);
                String dom = StringUtil.getDom(StringUtil.isEmpty(articleListRule.getUrl()) ? articleListRule.getSearch_url() : articleListRule.getUrl());
                String domNow = StringUtil.getDom(StringUtil.isEmpty(rule.getUrl()) ? rule.getSearch_url() : rule.getUrl());
                if (dom != null && domNow != null && !StringUtils.equals(dom, domNow)) {
                    return "{\"isSuccess\":false, \"errorMsg\":\"存在同名不同域名的规则，不能在Web端直接更新\"}";
                }
                rule.setUrl(articleListRule.getUrl());
                rule.setAuthor(articleListRule.getAuthor());
                rule.setVersion(articleListRule.getVersion());
                rule.setTitleColor(articleListRule.getTitleColor());
                rule.setCol_type(articleListRule.getCol_type());
                rule.setClass_name(articleListRule.getClass_name());
                rule.setClass_url(articleListRule.getClass_url());
                rule.setYear_name(articleListRule.getYear_name());
                rule.setYear_url(articleListRule.getYear_url());
                rule.setArea_name(articleListRule.getArea_name());
                rule.setArea_url(articleListRule.getArea_url());
                rule.setSort_name(articleListRule.getSort_name());
                rule.setSort_url(articleListRule.getSort_url());
                rule.setFind_rule(articleListRule.getFind_rule());
                rule.setSearch_url(articleListRule.getSearch_url());
                rule.setSearchFind(articleListRule.getSearchFind());
                rule.setGroup(articleListRule.getGroup());
                rule.setDetail_col_type(articleListRule.getDetail_col_type());
                rule.setDetail_find_rule(articleListRule.getDetail_find_rule());
                rule.setSdetail_col_type(articleListRule.getSdetail_col_type());
                rule.setSdetail_find_rule(articleListRule.getSdetail_find_rule());
                rule.setUa(articleListRule.getUa());
                rule.setPreRule(articleListRule.getPreRule());
                rule.setLast_chapter_rule(articleListRule.getLast_chapter_rule());

                try {
                    List<ArticleListPageRule> pageRules = articleListRule.getPageList();
                    for (ArticleListPageRule pageRule : pageRules) {
                        pageRule.setPath(StringUtils.replace(pageRule.getPath(), "hiker://page/", ""));
                    }
                    rule.setPages(JSON.toJSONString(pageRules));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rule.save();
            } else {
                articleListRule.save();
            }
            EventBus.getDefault().post(new OnArticleListRuleChangedEvent());
        } catch (IOException e) {
            return "{\"isSuccess\":false, \"errorMsg\":\"" + e.getMessage() + "\"}";
        }
        return "{\"isSuccess\":true}";
    }

    @GetMapping(path = "/ruleEdit")
    String ruleEdit() {
        return "forward:/index.html";
    }

    @GetMapping("/redirectPlayUrl")
    public void redirectPlayUrl(HttpRequest request, HttpResponse response) {
        String content = LocalServerParser.getRealUrlForRemotedPlay(Application.getContext(), RemoteServerManager.instance().getPlayUrl());
        com.yanzhenjie.andserver.http.ResponseBody body = new StringBody(content);
        response.setBody(body);
        response.setStatus(StatusCode.SC_FOUND);
        response.setHeader("Location", content);
    }

    @GetMapping(path = "/")
    String home() {
        return "forward:/player.html";
    }

    @GetMapping(path = "/list")
    String list() {
        return "forward:/list.html";
    }

    @GetMapping(path = "/homeRules")
    @ResponseBody
    String getHomeRules() {
        SyncRulesDTO rulesDTO = new SyncRulesDTO();
        List<ArticleListRule> rules = LitePal.findAll(ArticleListRule.class);
        String needSyncGroup = PreferenceMgr.getString(Application.getContext(), PreferenceConstant.KEY_needSyncGroup, "");
        if (StringUtil.isNotEmpty(needSyncGroup) && CollectionUtil.isNotEmpty(rules)) {
            Set<String> groupSet = toGroupSet(needSyncGroup);
            rules = Stream.of(rules)
                    .filter(rule -> StringUtil.isNotEmpty(rule.getGroup()) && groupSet.contains(StringUtil.simplyGroup(rule.getGroup())))
                    .collect(Collectors.toList());
        }
        String excludeSyncGroup = PreferenceMgr.getString(Application.getContext(), PreferenceConstant.KEY_excludeSyncGroup, "");
        if (StringUtil.isNotEmpty(excludeSyncGroup)) {
            Set<String> groupSet = toGroupSet(excludeSyncGroup);
            //排除分组在groupSet里面的规则
            rules = Stream.of(rules)
                    .filter(rule -> StringUtil.isEmpty(rule.getGroup()) || !groupSet.contains(StringUtil.simplyGroup(rule.getGroup())))
                    .collect(Collectors.toList());
        }
        rulesDTO.setHomeRules(JSON.toJSONString(rules, JSONPreFilter.getSimpleFilter()));
        BigTextDO bigTextDO = LitePal.where("key = ?", BigTextDO.ARTICLE_LIST_ORDER_KEY).findFirst(BigTextDO.class);
        if (bigTextDO != null) {
            String value = bigTextDO.getValue();
//            Log.d(TAG, "loadOrderMap: " + value);
            if (StringUtil.isNotEmpty(value)) {
                rulesDTO.setOrderMapStr(value);
            }
        }
        return JSON.toJSONString(rulesDTO);
    }

    @GetMapping(path = "/test")
    @ResponseBody
    String test() {
        return PreferenceMgr.getString(Application.getContext(), PreferenceConstant.KEY_deviceName, "ok");
    }

    private Set<String> toGroupSet(String groupText) {
        String[] groups = groupText.split("&&");
        Set<String> groupSet = new HashSet<>();
        for (String group : groups) {
            if (StringUtil.isNotEmpty(group)) {
                groupSet.add(StringUtil.simplyGroup(group));
            }
        }
        return groupSet;
    }
}
