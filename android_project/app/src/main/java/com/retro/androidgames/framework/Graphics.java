package com.retro.androidgames.framework;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;

public interface Graphics {
    public static enum PixmapFormat {
        ARGB8888, ARGB4444, RGB565
    }
    public Pixmap newPixmap(String fileName, PixmapFormat format);
    public Bitmap newBitmap(String fileName);
    public void clear(int color);
    public void drawPixel(int x, int y, int color);
    public void drawLine(int x, int y, int x2, int y2, int color);
    public void drawRect(int x, int y, int width, int height, int color);
    public void drawRect(int x, int y, int width, int height, Paint paint);
    public void drawPixmap(Pixmap pixmap, int x, int y, int srcX, int srcY, int srcWidth, int srcHeight);
    public void drawPixmap(Pixmap pixmap, int x, int y);
    public int getWidth();
    public int getHeight();
    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint);
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint);
    public void drawText(String text, int x, int y, int fontSize);
}

