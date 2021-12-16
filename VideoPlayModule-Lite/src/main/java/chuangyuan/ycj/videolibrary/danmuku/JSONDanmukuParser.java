package chuangyuan.ycj.videolibrary.danmuku;

/**
 * 作者：By 15968
 * 日期：On 2021/11/15
 * 时间：At 10:57
 */

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.android.JSONSource;

import static master.flame.danmaku.danmaku.model.IDanmakus.ST_BY_TIME;

public class JSONDanmukuParser extends BaseDanmakuParser {

    protected float mDispScaleX;
    protected float mDispScaleY;

    @Override
    public IDanmakus parse() {
        if (mDataSource != null) {
            JSONSource source = (JSONSource) mDataSource; //jsonSource
            JSONArray jsonArray = source.data();
            IDanmakus result = new Danmakus(ST_BY_TIME, false, mContext.getBaseComparator());
            for (int i = 0; i < jsonArray.length(); i++) {    //在这里将数据取出来设置进去
                BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL, mContext);
                danmaku.priority = 0;
                danmaku.textColor = Color.WHITE;
                danmaku.setTimer(mTimer);
                danmaku.index = i;
                try {
                    JSONObject object = jsonArray.getJSONObject(i);
                    danmaku.text = object.optString("text", "");
                    long time = (long) object.optDouble("time", 1) * 1000; // 出现时间
                    danmaku.setTime(time);
                    danmaku.flags = mContext.mGlobalFlagValues;
                    danmaku.textSize = 25 * (mDispDensity - 0.6f);
                    result.addItem(danmaku);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public BaseDanmakuParser setDisplayer(IDisplayer disp) {
        super.setDisplayer(disp);
        mDispScaleX = mDispWidth / DanmakuFactory.BILI_PLAYER_WIDTH;
        mDispScaleY = mDispHeight / DanmakuFactory.BILI_PLAYER_HEIGHT;
        return this;
    }
}