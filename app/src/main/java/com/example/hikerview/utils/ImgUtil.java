package com.example.hikerview.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.annimon.stream.function.Consumer;
import com.bumptech.glide.Glide;
import com.example.hikerview.ui.browser.util.CollectionUtil;
import com.example.hikerview.ui.view.PopImageLoaderNoView;
import com.lxj.xpopup.enums.ImageType;
import com.lxj.xpopup.interfaces.XPopupImageLoader;
import com.lxj.xpopup.util.XPopupUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * 作者：By hdy
 * 日期：On 2018/6/21
 * 时间：At 18:54
 */

public class ImgUtil {
    private static final String TAG = ImgUtil.class.getSimpleName();

    /**
     * 对图片进行毛玻璃化
     *
     * @param sentBitmap       位图
     * @param radius           虚化程度
     * @param canReuseInBitmap 是否重用
     * @return 位图
     */
    public static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {

        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm in your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

//        print("虚化后 ",bitmap);
        return (bitmap);
    }

    /**
     * 对图片进行毛玻璃化
     *
     * @param originBitmap 位图
     * @param scaleRatio   缩放比率
     * @param blurRadius   毛玻璃化比率，虚化程度
     * @return 位图
     */
    public static Bitmap doBlur(Bitmap originBitmap, int scaleRatio, int blurRadius) {
//        print("原图：：",originBitmap);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originBitmap,
                originBitmap.getWidth() / scaleRatio,
                originBitmap.getHeight() / scaleRatio,
                false);
        Bitmap blurBitmap = doBlur(scaledBitmap, blurRadius, false);
        scaledBitmap.recycle();
        return blurBitmap;
    }

//    private static void print(String tag, Bitmap originBitmap) {
//        StringBuilder sb = new StringBuilder(tag);
//        sb.append( String.format("  width=%s,",originBitmap.getWidth()));
//        sb.append( String.format(" height=%s,",originBitmap.getHeight()));
//        Log.i(TAG,sb.toString());
//    }

    /**
     * 对图片进行 毛玻璃化，虚化
     *
     * @param originBitmap 位图
     * @param width        缩放后的期望宽度
     * @param height       缩放后的期望高度
     * @param blurRadius   虚化程度
     * @return 位图
     */
    public static Bitmap doBlur(Bitmap originBitmap, int width, int height, int blurRadius) {
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(originBitmap, width, height);
        Bitmap blurBitmap = doBlur(thumbnail, blurRadius, true);
        thumbnail.recycle();
        return blurBitmap;
    }

    /**
     * 获取手机状态栏的高度
     *
     * @return 状态栏的高度
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c;
        Object obj;
        Field field;
        int x, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 保存到相册
     *
     * @param context context
     * @param picUrl  picUrl
     * @param baseUrl baseUrl
     */
    public static void savePic2Gallery(Context context, String picUrl, String baseUrl, OnSaveListener listener) {

        com.lxj.xpopup.util.XPermission.create(context, com.lxj.xpopup.util.PermissionConstants.STORAGE)
                .callback(new com.lxj.xpopup.util.XPermission.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        //save bitmap to album.
                        XPopupUtils.saveBmpToAlbum(context, new PopImageLoaderNoView(baseUrl), picUrl);
                    }

                    @Override
                    public void onDenied() {
                        Toast.makeText(context, "没有保存权限，保存功能无法使用！", Toast.LENGTH_SHORT).show();
                    }
                }).request();

    }

    /**
     * 保存到相册
     *
     * @param context context
     * @param picUrls picUrls
     * @param baseUrl baseUrl
     */
    public static void savePic2Gallery(Context context, List<String> picUrls, String baseUrl, Consumer<Integer> completeListener) {
        com.lxj.xpopup.util.XPermission.create(context, com.lxj.xpopup.util.PermissionConstants.STORAGE)
                .callback(new com.lxj.xpopup.util.XPermission.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        //save bitmap to album.
                        saveBmpToAlbum(context, new PopImageLoaderNoView(baseUrl), picUrls, completeListener);
                    }

                    @Override
                    public void onDenied() {
                        Toast.makeText(context, "没有保存权限，保存功能无法使用！", Toast.LENGTH_SHORT).show();
                    }
                }).request();

    }


    public static void saveBmpToAlbum(final Context context, final XPopupImageLoader imageLoader, List<String> uriList,
                                      Consumer<Integer> completeListener) {
        if (CollectionUtil.isEmpty(uriList)) {
            return;
        }
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            int success = 0;
            for (int i = 0; i < uriList.size(); i++) {
                String uri = uriList.get(i);
                File source = imageLoader.getImageFile(context, uri);
                if (source == null) {
                    continue;
                }
                //1. create path
                String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_PICTURES;
                File dirFile = new File(dirPath);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                try {
                    ImageType type = ImageHeaderParser.getImageType(new FileInputStream(source));
                    String ext = getFileExt(type);
                    final File target = new File(dirPath, System.currentTimeMillis() + "." + ext);
                    if (target.exists()) target.delete();
                    target.createNewFile();
                    //2. save
                    writeFileFromIS(target, new FileInputStream(source));
                    //3. notify
                    MediaScannerConnection.scanFile(context, new String[]{target.getAbsolutePath()},
                            new String[]{"image/" + ext}, (path, uri1) -> mainHandler.post(() -> {

                            }));
                    success++;
                } catch (IOException e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        Toast.makeText(context, "没有保存权限，保存功能无法使用！", Toast.LENGTH_SHORT).show();
                    });
                    break;
                }
            }
            if (completeListener != null) {
                completeListener.accept(success);
            }
        });
    }

    private static boolean writeFileFromIS(final File file, final InputStream is) {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            byte data[] = new byte[8192];
            int len;
            while ((len = is.read(data, 0, 8192)) != -1) {
                os.write(data, 0, len);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getFileExt(ImageType type) {
        switch (type) {
            case GIF:
                return "gif";
            case PNG:
            case PNG_A:
                return "png";
            case WEBP:
            case WEBP_A:
                return "webp";
            case JPEG:
                return "jpeg";
        }
        return "jpeg";
    }

    public interface OnSaveListener {
        void success();

        void failed(String msg);
    }

    private static void save(Context context, String picUrl, String picName) throws Exception {
        URL url = new URL(picUrl);
        //打开输入流
        InputStream inputStream = url.openStream();
        //对网上资源进行下载转换位图图片
        Bitmap bmp = BitmapFactory.decodeStream(inputStream);
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileName = null;
        //系统相册目录
        String galleryPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator;
        // 声明文件对象
        File file = null;
        // 声明输出流
        FileOutputStream outStream = null;
        try {
            // 如果有目标文件，直接获得文件对象，否则创建一个以filename为名称的文件
            file = new File(galleryPath, picName + ".jpg");
            // 获得文件相对路径
            fileName = file.toString();
            // 获得输出流，如果文件中有内容，追加内容
            outStream = new FileOutputStream(fileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //通知相册更新
//        MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                bmp, fileName, null);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        context.sendBroadcast(intent);
    }

    public static void downloadImgByGlide(Activity context, String urls, String filePathList) {
        HeavyTaskUtil.executeNewTask(() -> {
            String[] ors = urls.split("\\|\\|");
            for (String url : ors) {
                if (StringUtil.isNotEmpty(url)) {
                    try {
                        downloadImgByGlideSync(context, url, filePathList);
                        //只要有一个下载成功就结束
                        return;
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }
        });
    }


    private static void downloadImgByGlideSync(Activity context, String url, String filePath) throws ExecutionException, InterruptedException, IOException {
        File file = Glide.with(context).downloadOnly().load(url).submit().get();
        if (file != null && file.exists()) {
            File out = new File(filePath);
            if (out.exists()) {
                out.delete();
            } else if (!out.getParentFile().exists()) {
                out.getParentFile().mkdirs();
            }
            FileUtil.copy(file, out);
        } else {
            throw new IOException("file not found");
        }
    }
}
