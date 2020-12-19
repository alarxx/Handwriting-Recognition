package com.retro.androidgames.framework.gamedev;

import android.util.Log;

public class FPS {
    private static long lastTime = System.nanoTime();
    private static int fps = 0;
    public static void fps(){
        fps+=1;
        if(System.nanoTime()-lastTime>1000000000){
            Log.d("FPS", ""+fps);
            fps=0;
            lastTime=System.nanoTime();
        }
    }
}
