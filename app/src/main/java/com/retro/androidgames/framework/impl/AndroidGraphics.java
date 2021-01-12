package com.retro.androidgames.framework.impl;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options ;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.retro.androidgames.framework.Graphics;
import com.retro.androidgames.framework.Pixmap;

import java.io.IOException;
import java.io.InputStream;

public class AndroidGraphics implements Graphics {
    AssetManager asset;
    Bitmap frameBuffer;
    Canvas canvas;
    Paint paint;
    Rect srcRect = new Rect(), dstRect = new Rect();

    public AndroidGraphics(AssetManager asset, Bitmap frameBuffer){
        this.asset = asset;
        this.frameBuffer = frameBuffer;
        this.canvas = new Canvas(frameBuffer);
        this.paint = new Paint();
    }

    @Override
    public Pixmap newPixmap(String fileName, PixmapFormat format){
        Config config = null;
        if(format == PixmapFormat.RGB565) config = Config.RGB_565;
        else if(format == PixmapFormat.ARGB4444) config = Config.ARGB_4444;
        else config = Config.ARGB_8888;
        Options options = new Options();
        options.inPreferredConfig = config;
        InputStream in = null;
        Bitmap bitmap = null;
        try{
            in = asset.open(fileName);
            bitmap = BitmapFactory.decodeStream(in);
            if(bitmap == null)
                throw new RuntimeException("Couldn't load bitmap from assets: ' " + fileName + " '");
        } catch (IOException e){ throw new RuntimeException("Couldn't load bitmap from assets: ' " + fileName + " '");
        }finally{
            if(in != null){
                try{
                    in.close();
                }catch (IOException e){}
            }
        }

        if(bitmap.getConfig() == Config.RGB_565){
            format = PixmapFormat.RGB565;
        }else if(bitmap.getConfig() == Config.ARGB_4444){
            format = PixmapFormat.ARGB4444;
        }else format = PixmapFormat.ARGB8888;

        return new AndroidPixmap(bitmap, format);
    }
    @Override
    public Bitmap newBitmap(String fileName){
        InputStream in = null;
        Bitmap bitmap = null;
        try{
            in = asset.open(fileName);
            bitmap = BitmapFactory.decodeStream(in);
            if(bitmap == null)
                throw new RuntimeException("Couldn't load bitmap from assets: ' " + fileName + " '");
        } catch (IOException e){ throw new RuntimeException("Couldn't load bitmap from assets: ' " + fileName + " '");
        }finally{
            if(in != null){
                try{
                    in.close();
                }catch (IOException e){}
            }
        }
        return bitmap;
    }

    @Override
    public void clear(int color) {
        canvas.drawRGB((color & 0xff0000) >> 16, (color & 0xff00) >> 8,
                (color & 0xff));
    }

    @Override
    public void drawPixel(int x, int y, int color) {
        paint.setColor(color);
        canvas.drawPoint(x, y, paint);
    }

    @Override
    public void drawLine(int x, int y, int x2, int y2, int color) {
        paint.setColor(color);
        canvas.drawLine(x, y, x2, y2, paint);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(x, y, x + width - 1, y + height - 1, paint);
    }
    @Override
    public void drawRect(int x, int y, int width, int height, Paint paint){
        canvas.drawRect(x, y, x + width - 1, y + height - 1, paint);
    }

    @Override
    public void drawPixmap(Pixmap p, int x , int y, int srcX, int srcY, int srcWidth, int srcHeight){ //Позволяет нарисовать часть картинки
        srcRect.left = srcX;
        srcRect.top = srcY;
        srcRect.right = srcX + srcWidth - 1;
        srcRect.bottom = srcY + srcHeight - 1;
        dstRect.left = x;
        dstRect.top = y;
        dstRect.right = x + srcWidth - 1;
        dstRect.bottom = y + srcHeight - 1;
        canvas.drawBitmap(((AndroidPixmap) p).bitmap, srcRect, dstRect, null);
    }
    @Override
    public void drawPixmap(Pixmap pixmap, int x, int y) {
        canvas.drawBitmap(((AndroidPixmap)pixmap).bitmap, x, y, null);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint){
        canvas.drawBitmap(bitmap, left, top, paint);
    }
    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint){
        canvas.drawBitmap(bitmap, src, dst, paint);
    }

    @Override
    public void drawText(String text, int x, int y, int fontSize){
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(fontSize);
        canvas.drawText(text, x, y, paint);
    }

    @Override
    public int getWidth() {
        return frameBuffer.getWidth();
    }
    @Override
    public int getHeight() {
        return frameBuffer.getHeight();
    }
}

