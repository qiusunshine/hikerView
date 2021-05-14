package com.example.hikerview.utils;

import android.content.Context;
import android.util.Log;

import com.example.hikerview.constants.CollectionTypeConstant;
import com.example.hikerview.model.PlayerPosHis;
import com.example.hikerview.model.ViewCollection;
import com.example.hikerview.model.ViewHistory;
import com.example.hikerview.service.parser.HttpParser;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.webdlan.LocalServerParser;

import org.litepal.LitePal;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2018/12/6
 * 时间：At 21:27
 */
public class HeavyTaskUtil {
    private static final String TAG = "HeavyTaskUtil";
    private static final String[] playPosFixWhiteList = new String[]{"m3u8.htv009.com", "127.0.0.1", ":11111/"};
    //这里的代码是拿的AsyncTask的源码，作用是创建合理可用的线程池容量
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 3)) + 2;
    private static LinkedBlockingDeque<Runnable> taskQueue = new LinkedBlockingDeque<>(8192);
    private static ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, 6,
            10L, TimeUnit.SECONDS, taskQueue);

    public static void executeNewTask(Runnable command) {
//        Log.d(TAG, "executeNewTask: CPU_COUNT=" + CPU_COUNT + ", CORE_POOL_SIZE=" + CORE_POOL_SIZE);
        executorService.execute(command);
    }

    public static void executeBigTask(Runnable command) {
        executorService.execute(command);
    }

    public static ExecutorService getBigTaskExecutorService() {
        return executorService;
    }

    public static LinkedBlockingDeque<Runnable> getBigTaskQueue() {
        return taskQueue;
    }

    public static void saveNowPlayerPos(Context mContext, String lastUrl, long pos) {
        saveNowPlayerPos(mContext, LocalServerParser.getUrlForPos(mContext, lastUrl), (int) pos, true);
    }

    public static void saveNowPlayerPos(Context mContext, String lastUrl, long pos, boolean canBelow) {
        saveNowPlayerPos(mContext, LocalServerParser.getUrlForPos(mContext, lastUrl), (int) pos, canBelow);
    }

    public static void saveNowPlayerPos(Context mContext, String lastUrl, int pos, boolean canBelow) {
        if (lastUrl.startsWith("content")) {
            return;
        }
        lastUrl = HttpParser.getRealUrlFilterHeaders(lastUrl);
        lastUrl = LocalServerParser.getUrlForPos(mContext, lastUrl);
        lastUrl = getUrlForPosMemory(lastUrl);
        try {
            PlayerPosHis playerPosHis = LitePal.where("playUrl = ?", lastUrl).findFirst(PlayerPosHis.class);
            if (playerPosHis != null) {
                if (pos > playerPosHis.getPos() || canBelow) {
                    playerPosHis.setPos(pos);
                    playerPosHis.save();
                }
            } else {
                //超过500条，从头删除第一条
                int count = 0;
                try {
                    count = LitePal.count(PlayerPosHis.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (count > 500) {
                    PlayerPosHis l = null;
                    try {
                        l = LitePal.order("id").findFirst(PlayerPosHis.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (l != null) {
                        Timber.d("saveNowPlayerPos: delete=>%s", l.getPlayUrl());
                        l.delete();
                    }
                }
                //入库
                playerPosHis = new PlayerPosHis();
                playerPosHis.setPlayUrl(lastUrl);
                playerPosHis.setPos(pos);
                playerPosHis.save();
            }
        } catch (Exception e) {
            Log.e(TAG, "saveNowPlayerPos: " + e.getMessage(), e);
        }
    }

    public static int getPlayerPos(Context context, String playUrl) {
        try {
            if (playUrl.startsWith("content")) {
                return 0;
            }
            playUrl = HttpParser.getRealUrlFilterHeaders(playUrl);
            playUrl = LocalServerParser.getUrlForPos(context, playUrl);
            playUrl = getUrlForPosMemory(playUrl);
            PlayerPosHis playerPosHis = LitePal.where("playUrl = ?", playUrl).findFirst(PlayerPosHis.class);
            if (playerPosHis != null) {
                return playerPosHis.getPos();
            }
        } catch (Exception e) {
            Log.e(TAG, "getPlayerPos: " + e.getMessage(), e);
        }
        return 0;
    }

    private static String getUrlForPosMemory(String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        if (url.contains("memoryPosition=full")) {
            return url;
        }
        for (String s : playPosFixWhiteList) {
            if (url.contains(s)) {
                return url;
            }
        }
        String[] simpleUrls = url.split("\\?");
        if (simpleUrls.length < 2) {
            return url;
        }
        String simpleUrl = simpleUrls[0];
        String[] s = simpleUrl.split("://");
        if (s.length > 1) {
            //形如www.test.com/index.m3u8
            String[] s2 = s[1].split("/");
            if (s2.length <= 2) {
                //链接形如http://www.test.com/index.m3u8格式，过于简单，一般只根据?前的内容无法直接定位具体的资源
                return url;
            }
            String last = s2[s2.length - 1].split("\\.")[0];
            if (last.length() < 5 || "index".equalsIgnoreCase(last) || "vplay".equalsIgnoreCase(last)) {
                //链接形如http://www.test.com/src/index.m3u8格式，过于简单，一般只根据?前的内容无法直接定位具体的资源
                return url;
            }
        }
        return simpleUrl;
    }


    /**
     * 历史记录
     *
     * @param type        type
     * @param ruleBaseUrl ruleBaseUrl
     * @param chapterUrl  链接
     * @param movieTitle  电影名字
     */
    public static void saveHistory(Context context, final String type, final String ruleBaseUrl, final String chapterUrl, final String movieTitle) {
        if (!chapterUrl.startsWith("http")) {
            return;
        }
        executeNewTask(() -> {
            try {
                saveHisSync(type, ruleBaseUrl, chapterUrl, movieTitle, null, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 加入历史记录，二级列表
     *
     * @param chapterUrl
     * @param movieTitle
     * @param params
     */
    public static void saveHistory(final String chapterUrl, final String movieTitle, final String params, final String group, final String picUrl) {
//        if (params.length() > 1500) {
//            Log.d(TAG, "saveHistory: DETAIL_LIST_VIEW, params.length is too long");
//            return;
//        }
        executeNewTask(() -> {
            try {
                saveHisSync(CollectionTypeConstant.DETAIL_LIST_VIEW, StringUtil.getHomeUrl(chapterUrl), chapterUrl, movieTitle, params, group, picUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateHistoryLastClick(String title, String url, String lastClick) {
        Log.d(TAG, "updateHistoryLastClick: " + lastClick);
        executeNewTask(() -> {
            List<ViewHistory> last = null;
            try {
                last = LitePal.where("url = ? and title = ? and type = ?", url, title, CollectionTypeConstant.DETAIL_LIST_VIEW).limit(1).find(ViewHistory.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtil.isEmpty(last)) {
                last.get(0).setLastClick(lastClick);
                last.get(0).setTime(new Date());
                last.get(0).save();
            }
        });
    }

    public static void updateHistoryVideoUrl(String url, String videoUrl) {
        executeNewTask(() -> {
            List<ViewHistory> last = null;
            try {
                last = LitePal.where("url = ? and type = ?", url, CollectionTypeConstant.WEB_VIEW).limit(1).find(ViewHistory.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtil.isEmpty(last)) {
                last.get(0).setVideoUrl(videoUrl);
                last.get(0).setTime(new Date());
                last.get(0).save();
            }

            List<ViewCollection> collections = null;
            try {
                collections = LitePal.where("CUrl = ?", url).limit(1).find(ViewCollection.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtil.isEmpty(collections)) {
                collections.get(0).setVideoUrl(videoUrl);
                collections.get(0).setTime(new Date());
                collections.get(0).save();
            }
        });
    }

    private synchronized static void saveHisSync(String type, String ruleBaseUrl, String chapterUrl, String movieTitle, String params, String group, String picUrl) {
        List<ViewHistory> last = null;
        try {
            if (CollectionTypeConstant.DETAIL_LIST_VIEW.equals(type)) {
                last = LitePal.where("url = ? and title = ? and type = ?", chapterUrl, movieTitle, CollectionTypeConstant.DETAIL_LIST_VIEW).limit(1).find(ViewHistory.class);
            } else {
                last = LitePal.where("url = ? and type = ?", chapterUrl, CollectionTypeConstant.WEB_VIEW).limit(1).find(ViewHistory.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtil.isEmpty(last)) {
            last.get(0).setTime(new Date());
            last.get(0).setTitle(movieTitle);
            last.get(0).setParams(params);
            last.get(0).setGroup(group);
            if (StringUtil.isNotEmpty(picUrl)) {
                last.get(0).setPicUrl(picUrl);
            }
            last.get(0).save();
            return;
        }
        int count = 0;
        try {
            count = LitePal.count(ViewHistory.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (count >= 1000) {
            ViewHistory l = null;
            try {
                l = LitePal.findFirst(ViewHistory.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (l != null) {
                l.delete();
            }
        }
        ViewHistory history = new ViewHistory();
        history.setTime(new Date());
        history.setTitle(movieTitle);
        history.setUrl(chapterUrl);
        history.setRuleBaseUrl(ruleBaseUrl);
        history.setType(type);
        history.setParams(params);
        history.setGroup(group);
        history.setPicUrl(picUrl);
        history.save();
    }
}
