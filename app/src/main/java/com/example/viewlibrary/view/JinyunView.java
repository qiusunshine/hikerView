package com.example.viewlibrary.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.viewlibrary.other.CirclePoint;
import com.example.viewlibrary.other.Triangle;
import com.example.viewlibrary.util.DrawUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class JinyunView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final double SMOOTHNESS = 0.4;
    //三角形移动速度
    private double moveSpeed = 0.4;

    //刷新时间
    private static int refreshTime = 50;

    //添加两次三角形的间隔
    private static int addTriangleInterval = 50;

    //每次添加的数量限制
    private static int addTriangleOnece = 1;

    //总三角形数量
    private int allTriangleCount = 300;
    //所有的三角形
    private static List<Triangle> triangleList = new ArrayList<>();


    //圆的坐标点
    private ArrayList<CirclePoint> circlePointList = new ArrayList<>();
    //两点之间画贝塞尔曲线的一个值
    private double bezierDistance;

    private double towPointMargin;

    private int mCircleR;
    private int mCircleLineMargin;


    //音谱数量
    private static final int LUMP_COUNT = 180;
    //取样间隔
    private static final int WAVE_SAMPLING_INTERVAL = 1;
    private static final int LUMP_SPACE = 1;
    private static double ratio = 1;


    //画音频线
    private byte[] mBytes;


    private float[] mPoints;
    private Paint mPaint;


    private SurfaceHolder mSurfaceHolder;

    private boolean mIsDrawing, onPause = false;
    private int mPaintColor = Color.parseColor("#cabfa3");

    private Bitmap bitmapBg;

    public void setBitmapBg(Bitmap bitmapBg) {
        this.bitmapBg = bitmapBg;
        //mPaintColor = ImageUtil.getColor(bitmapBg, 4).getRgb();
    }

    public JinyunView(Context context) {
        super(context);
        initView();
    }

    public JinyunView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public JinyunView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
        setFocusableInTouchMode(true);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mPaintColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mIsDrawing = true;
        mCircleR = getWidth() / 2 - (getWidth() / 6);
        bezierDistance = (1 - Math.cos(Math.toRadians(1))) * mCircleR;
        towPointMargin = 2 * Math.sin(Math.toRadians(1)) * mCircleR;
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        getCirclePoint(mCircleR, getWidth() / 2, getHeight() / 2);
        //setZOrderOnTop(true);
//        new Thread(this).start();
    }


    //获取圆周点坐标
    private void getCirclePoint(double circleR, double circleX, double circleY) {
        circlePointList.clear();
        for (int i = -180; i < 180; i = i + 2) {
            circlePointList.add(new CirclePoint(i, circleR, circleX, circleY));
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mIsDrawing = false;
    }

    @Override
    public void run() {
        while (mIsDrawing) {
            if (!onPause) {
                try {
                    drawSomething();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onPause() {
        onPause = true;
    }

    public void onResume() {
        onPause = false;
    }

    private void drawSomething() {
        Canvas canvas = null;
        long t = System.currentTimeMillis();
        try {
            canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if (bitmapBg != null) {
                canvas.drawBitmap(bitmapBg, 0, 0, new Paint());
            }
            //drawAudioLine(canvas);
//            drawCircleLine(canvas);
            manageTriangle((int) ((System.currentTimeMillis() - t) * moveSpeed), canvas);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
            SystemClock.sleep(Math.max(refreshTime - (System.currentTimeMillis() - t), 0));
        }
    }

    public static int pointSize = 180;

    Path wavePath = new Path();
    Path wavePath2 = new Path();


    //画线
    private void drawCircleLine(Canvas canvas) {

        wavePath.reset();
        wavePath2.reset();
        List<CirclePoint> circlePoints = new ArrayList<>();
        for (int i = 0; i < circlePointList.size() - 1; i++) {
            CirclePoint circlePoint = circlePointList.get(i);
            circlePoint.move(mBytes[i]);
            if (mBytes[i] == 0 && mBytes[i + 1] == 0) {
                DrawUtil.drawCicleLineFromTowPoints(canvas, circlePoint, circlePointList.get(i + 1), mPaint);
            } else {
                circlePoints.add(circlePoint);
                if (mBytes[i + 1] == 0) {
                    circlePoints.add(circlePointList.get(i + 1));
                    DrawUtil.drawCurvesFromPoints(canvas, circlePoints, 0.4, mPaint);
                    circlePoints.clear();
                }
            }
        }
        DrawUtil.drawCicleLineFromTowPoints(canvas, circlePointList.get(circlePointList.size() - 1), circlePointList.get(0), mPaint);
    }


    private static Long startTime = System.currentTimeMillis();

    /**
     * 三角形控制
     *
     * @param distence
     */
    private void manageTriangle(int distence, Canvas canvas) {
        Iterator iter = triangleList.iterator();
        while (iter.hasNext()) {
            Triangle triangle = (Triangle) iter.next();
            if (triangle.isOut(getWidth(), getHeight())) {
                iter.remove();
            } else {
                triangle.move(distence);
            }
            drawTriangle(canvas, triangle, mPaintColor);
        }

        if (System.currentTimeMillis() - startTime > addTriangleInterval && triangleList.size() < allTriangleCount) {
            for (int i = 0; i < addTriangleOnece; i++) {
                triangleList.add(Triangle.getRandomTriangle(getWidth() / 2, getHeight() / 2));
            }
            startTime = System.currentTimeMillis();
        }
    }


    /**
     * 画三角形
     *
     * @param canvas
     * @param triangle
     * @param color
     */
    public void drawTriangle(Canvas canvas, Triangle triangle, int color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(getAlpha(triangle));
        paint.setStrokeWidth(5);

        Path path = new Path();
        path.moveTo(triangle.topPoint1.x, triangle.topPoint1.y);
        path.lineTo(triangle.topPoint2.x, triangle.topPoint2.y);
        path.lineTo(triangle.topPoint3.x, triangle.topPoint3.y);
        path.close();
        canvas.drawPath(path, paint);
    }

    public int getAlpha(Triangle triangle) {
        double distence1 = Math.sqrt(Math.pow((triangle.topPoint1.x - getWidth() / 2), 2) + Math.pow((triangle.topPoint1.y - getHeight() / 2), 2));
        double distence2 = Math.sqrt(Math.pow((triangle.topPoint2.x - getWidth() / 2), 2) + Math.pow((triangle.topPoint2.y - getHeight() / 2), 2));
        double distence3 = Math.sqrt(Math.pow((triangle.topPoint3.x - getWidth() / 2), 2) + Math.pow((triangle.topPoint3.y - getHeight() / 2), 2));

        double distence = Math.max(Math.max(distence1, distence2), distence3);

        if (distence < getWidth() * (1.5 / 5)) {
            return 255;
        } else {
            double alpha = ((-1275 / (2 * (double) getWidth())) * distence + 1275 / 2) - 280;
            if (alpha < 0) {
                alpha = 0;
            }
            return (int) alpha;
        }
    }

    public void setmPaintColor(int mPaintColor) {
        this.mPaintColor = mPaintColor;
        mPaint.setColor(mPaintColor);
    }

    public void setmBytes(byte[] mBytes) {
        this.mBytes = mBytes;

    }

}
