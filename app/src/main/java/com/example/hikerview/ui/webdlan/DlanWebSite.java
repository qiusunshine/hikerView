package com.example.hikerview.ui.webdlan;

import com.example.hikerview.constants.RemotePlayConfig;
import com.yanzhenjie.andserver.annotation.Website;
import com.yanzhenjie.andserver.framework.website.AssetsWebsite;

/**
 * 作者：By hdy
 * 日期：On 2019/3/19
 * 时间：At 0:34
 */
@Website
public class DlanWebSite extends AssetsWebsite {

    public DlanWebSite() {
        super(RemotePlayConfig.playerPath, "player.html");
    }
}
