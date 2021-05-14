package com.example.hikerview.ui.download.util;

import android.util.Log;

import java.io.IOException;
import java.net.URL;

/**
 * Created by xm on 17/8/17.
 */
public class M3U8Util {
    public static double figureM3U8Duration(String url) throws IOException {
        String m3U8Content = HttpRequestUtil.getResponseString(HttpRequestUtil.sendGetRequest(url));
        boolean isSubFileFound = false;
        double totalDuration = 0d;
        for(String lineString:m3U8Content.split("\n")){
            lineString = lineString.trim();
            if(isSubFileFound){
                if(lineString.startsWith("#")){
                    //格式错误 直接返回时长0
                    Log.d("M3U8Util", "格式错误1");
                    return 0d;
                }else{
                    String subFileUrl = new URL(new URL(url), lineString).toString();
                    return figureM3U8Duration(subFileUrl);
                }
            }
            if(lineString.startsWith("#")){
                if(lineString.startsWith("#EXT-X-STREAM-INF")){
                    isSubFileFound = true;
                    continue;
                }
                if(lineString.startsWith("#EXTINF:")){
                    int sepPosition = lineString.indexOf(",");
                    if(sepPosition<="#EXTINF:".length()){
                        sepPosition = lineString.length();
                    }
                    double duration = 0d;
                    try {
                        duration = Double.parseDouble(lineString.substring("#EXTINF:".length(), sepPosition).trim());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                        //格式错误 直接返回时长0
                        Log.d("M3U8Util", "格式错误3");
                        return 0d;
                    }
                    totalDuration += duration;
                }
            }

        }
        return totalDuration;
    }
}
