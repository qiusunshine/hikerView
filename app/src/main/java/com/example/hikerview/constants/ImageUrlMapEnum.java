package com.example.hikerview.constants;

import com.example.hikerview.R;

/**
 * 作者：By 15968
 * 日期：On 2021/1/11
 * 时间：At 22:42
 */

public enum ImageUrlMapEnum {
    collection(R.drawable.home_collection, "hiker://images/collection"),
    bookmark(R.drawable.home_bookmark, "hiker://images/bookmark"),
    history(R.drawable.home_history, "hiker://images/history"),
    video(R.drawable.home_video, "hiker://images/video"),
    home(R.drawable.home_home, "hiker://images/home"),
    account(R.drawable.account_home, "hiker://images/account"),
    placeholder(R.drawable.nothing, "hiker://images/placeholder"),
    bbs(R.drawable.bbs, "hiker://images/bbs"),
    card_bg(R.drawable.card_bg, "hiker://images/card_bg"),
    icon1(R.drawable.icon1, "hiker://images/icon1"),
    icon2(R.drawable.icon2, "hiker://images/icon2"),
    icon3(R.drawable.icon3, "hiker://images/icon3"),
    icon4(R.drawable.icon4, "hiker://images/icon4"),
    search(R.drawable.ic_main_nav_search, "hiker://images/search"),
    logo(R.drawable.logo, "hiker://images/logo"),
    setting_green(R.drawable.setting_green, "hiker://images/设置"),
    bookmark_green(R.drawable.bookmark_green, "hiker://images/书签"),
    douban(R.drawable.douban, "hiker://images/豆瓣"),
    switch_source(R.drawable.switch_source, "hiker://images/开关"),
    his_green(R.drawable.his_green, "hiker://images/历史"),
    add_green(R.drawable.add_green, "hiker://images/添加"),
    edit_black(R.drawable.edit_black, "hiker://images/修改"),
    delete(R.drawable.delete, "hiker://images/删除"),
    mv(R.drawable.mv, "hiker://images/移动"),
    backup_cloud(R.drawable.backup_cloud, "hiker://images/云备份");
    private final int id;
    private final String url;

    ImageUrlMapEnum(int id, String url) {
        this.id = id;
        this.url = url;
    }


    public String getUrl() {
        return url;
    }

    public int getId() {
        return id;
    }

    public static int getIdByUrl(String url) {
        for (ImageUrlMapEnum value : values()) {
            if (value.getUrl().equals(url)) {
                return value.getId();
            }
        }
        return -1;
    }
}
