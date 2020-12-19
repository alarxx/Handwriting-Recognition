package com.retro.androidgames.framework.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import com.retro.androidgames.framework.Game;

public class AndroidView extends View {
    Game game;
    Bitmap framebuffer;
    Rect dst;
    long startTime;
    float deltaTime;

    public AndroidView(Game game, Bitmap framebuffer, int width, int height) {
        super((Context) game);
        this.game = game;
        this.framebuffer = framebuffer;
        dst = new Rect(0, 0, width, height);
        startTime = System.nanoTime();
    }

    private void updateTime(){
        deltaTime = (System.nanoTime()-startTime)/ 1000000000.0f;
        startTime = System.nanoTime();
    }

    protected void onDraw(Canvas canvas){
        updateTime();
        game.getCurrentScreen().update(deltaTime);
        game.getCurrentScreen().present(deltaTime);
        canvas.drawBitmap(framebuffer, null, dst, null);
        invalidate();
    }

}
