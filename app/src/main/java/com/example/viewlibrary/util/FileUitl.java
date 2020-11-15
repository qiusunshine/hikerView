package com.example.viewlibrary.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 作者：Tyhj on 2018/10/21 19:13
 * 邮箱：tyhj5@qq.com
 * github：github.com/tyhjh
 * description：
 */

public class FileUitl {


    public static File bitmapToPath(Bitmap bitmap, String filepath) {
        File file = new File(filepath);
        //3.保存Bitmap
        try {
            //文件

            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }

            FileOutputStream fos = null;
            fos = new FileOutputStream(file);
            if (null != fos) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    public static void drawSmallPoint(Bitmap bitmap, int x, int y, int color) {
        drawPoint(bitmap, x, y, 1, color);
    }


    public static void drawPoint(Bitmap bitmap, int x, int y, int size, int color) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint2 = new Paint();
        paint2.setColor(color);
        paint2.setStyle(Paint.Style.FILL);
        canvas.drawRect(x - size, y - size, x + size, y + size, paint2);
    }


    public static void drawRect(Bitmap bitmap, Rect rect, int color) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint2 = new Paint();
        paint2.setColor(color);
        paint2.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect, paint2);
    }


    /**
     * @param bitmap
     * @param fromY  开始的比例
     * @param y      长度所占比例
     * @return
     */
    public static Bitmap cropBitmapY(Bitmap bitmap, double fromY, double y) {
        return bitmap.createBitmap(bitmap, 0, (int) (bitmap.getHeight() * fromY), bitmap.getWidth(), (int) (bitmap.getHeight() * y));
    }


}
