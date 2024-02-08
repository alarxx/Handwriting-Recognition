package com.rat6.utils;

import com.retro.androidgames.framework.Input;

import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;

public class Overlap {
    public static boolean pointInRect(int pX, int pY, int rLeftX, int rBottomY, int rRightX, int rTopY){
        return rLeftX <= pX && rRightX >= pX && rTopY <= pY && rBottomY >= pY;
    }
    public static boolean pointInRect(Rect r, Point point){
        return pointInRect(point.x(), point.y(), r.x(), r.br().y(), r.br().x(), r.tl().y());
    }
    public static boolean pointInRect(Rect r, Input.TouchEvent point){
        return pointInRect(point.x(), point.y(), r.x(), r.br().y(), r.br().x(), r.tl().y());
    }
    public static boolean pointInRect(android.graphics.Rect r, Point point){
        return pointInRect(point.x(), point.y(), r.left, r.bottom, r.right, r.top);
    }
    public static boolean pointInRect(android.graphics.Rect r, Input.TouchEvent point){
        return pointInRect(point.x(), point.y(), r.left, r.bottom, r.right, r.top);
    }
}
