package com.example.hikerview.ui.js.editor;

import android.graphics.Paint;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/2/19.
 */

public class TextUtil {
    private static Map<String, Map<Float, Float>> fontMaps = new HashMap<>();

    public static float messureText(Paint paint, String c) {
        Map<Float, Float> map = fontMaps.get(c);
        if (map == null) {
            map = new HashMap<>();
        }
        if (map.containsKey(paint.getTextSize())) {
            return map.get(paint.getTextSize());
        } else {
            float len = paint.measureText(c);
            map.put(paint.getTextSize(), len);
            return len;
        }
    }
}
