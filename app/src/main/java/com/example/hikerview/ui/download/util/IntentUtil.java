package com.example.hikerview.ui.download.util;

import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * Created by xm on 17/8/21.
 */
public class IntentUtil {
    /** 使用系统API，根据url获得对应的MIME类型 */
    public static String getMimeTypeFromUrl(String url) {
        String type = null;
        //使用系统API，获取URL路径中文件的后缀名（扩展名）
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            //使用系统API，获取MimeTypeMap的单例实例，然后调用其内部方法获取文件后缀名（扩展名）所对应的MIME类型
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (type == null){
                if ("m3u8".equals(extension)){
                    //m3u8只是Unicode版本的m3u而已,目前(Android O)MimeTypeMap里没有m3u8对应的mimetype
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u");
                }
            }
        }
        Log.i("bqt", "系统定义的MIME类型为：" + type);
        return type;
    }
}
