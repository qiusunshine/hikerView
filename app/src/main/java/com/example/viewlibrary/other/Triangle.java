package com.example.viewlibrary.other;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Triangle {

    public static int triangleSize = 45;
    public Point topPoint1, topPoint2, topPoint3;
    public int moveAngle;

    public static int moveAnglePre;

    public Triangle(Point topPoint1, Point topPoint2, Point topPoint3) {
        this.topPoint1 = topPoint1;
        this.topPoint2 = topPoint2;
        this.topPoint3 = topPoint3;
        moveAngle = getMoveAngel();
    }

    private int getMoveAngel() {
        int moveAngle = 180 - new Random(System.currentTimeMillis()*System.currentTimeMillis()).nextInt(360);
        if (moveAngle == 0 || moveAngle == 90 || moveAngle == 180 || moveAngle == -90 || moveAngle == -180) {
            return getMoveAngel();
        }

        if(Math.abs(moveAnglePre - moveAngle) < 30){
            return getMoveAngel();
        }

        moveAnglePre = moveAngle;
        return moveAngle;
    }


    public void move(int distance) {
        int moveX, moveY;
        moveY = (int) (Math.sin(Math.toRadians(moveAngle)) * distance);
        moveX = (int) (Math.cos(Math.toRadians(moveAngle)) * distance);
        move(this, moveX, moveY);
    }


    public boolean isOut(int width, int height) {
        if (topPoint1.x <= 0 || topPoint1.y <= 0 || topPoint1.x >= width || topPoint1.y >= height) {
            return true;
        }
        return false;
    }


    public static Random random = new Random(System.currentTimeMillis());
    public static List<Point> pointList = new ArrayList<>();


    public static Triangle getRandomTriangle(int startX, int startY) {
        pointList.clear();
        for (int i = -triangleSize; i <= triangleSize; i = i + 2) {
            for (int k = -triangleSize; k < triangleSize; k = k + 2) {
                pointList.add(new Point(i, k));
            }
        }
        Triangle triangle = null;
        Point topPoint1, topPoint2, topPoint3;
        topPoint1 = pointList.get(random.nextInt(triangleSize * triangleSize));
        topPoint2 = pointList.get(random.nextInt(triangleSize * triangleSize));
        topPoint3 = pointList.get(random.nextInt(triangleSize * triangleSize));
        triangle = new Triangle(topPoint1, topPoint2, topPoint3);
        move(triangle, startX, startY);
        if (isTriangle(triangle)) {
            return triangle;
        } else {
            return getRandomTriangle(startX, startY);
        }
    }


    public static void move(Triangle triangle, int x, int y) {
        triangle.topPoint1.x += x;
        triangle.topPoint2.x += x;
        triangle.topPoint3.x += x;
        triangle.topPoint1.y += y;
        triangle.topPoint2.y += y;
        triangle.topPoint3.y += y;

    }


    public static boolean isTriangle(Triangle triangle) {
        double a = Math.sqrt(Math.pow((triangle.topPoint1.x - triangle.topPoint2.x),2) + Math.pow((triangle.topPoint1.y - triangle.topPoint2.y),2));
        double b = Math.sqrt(Math.pow((triangle.topPoint1.x - triangle.topPoint3.x),2) + Math.pow((triangle.topPoint1.y - triangle.topPoint3.y),2));
        double c = Math.sqrt(Math.pow((triangle.topPoint2.x - triangle.topPoint3.x),2) + Math.pow((triangle.topPoint2.y - triangle.topPoint3.y),2));
        if (a + b <= c + triangleSize / 10 || a + c <= b + triangleSize / 10 || b + c <= a + triangleSize / 10) {
            return false;
        }
        if (a <= triangleSize / 10 || b <= triangleSize / 10 || c <= triangleSize / 10) {
            return false;
        }

        return true;
    }


}
