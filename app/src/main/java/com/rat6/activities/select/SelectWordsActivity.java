package com.rat6.activities.select;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.rat6.activities.camera.WordRecActivityUp;
import com.rat6.utils.WordRec;
import com.retro.androidgames.framework.StartNewClass;
import com.rat6.Extra;
import com.retro.androidgames.framework.Audio;
import com.retro.androidgames.framework.FileIO;
import com.retro.androidgames.framework.Game;
import com.retro.androidgames.framework.Graphics;
import com.retro.androidgames.framework.Input;
import com.retro.androidgames.framework.Screen;
import com.retro.androidgames.framework.impl.AndroidAudio;
import com.retro.androidgames.framework.impl.AndroidFileIO;
import com.retro.androidgames.framework.impl.AndroidGraphics;
import com.retro.androidgames.framework.impl.AndroidInput;
import com.retro.androidgames.framework.impl.AndroidView;

import org.bytedeco.opencv.opencv_core.Mat;

import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class SelectWordsActivity extends Activity implements Game, StartNewClass {

    private AndroidView renderView;
    private Graphics graphics;
    private Input input;
    private Audio audio;
    private FileIO fileIO;
    private Screen screen;

    private Mat matOrig;
    private WordRec wordRecog;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        matOrig = new Mat(Extra.mat).clone();
        Extra.mat.release();

        wordRecog = Extra.wordRecog;

        int frameBufferWidth = matOrig.cols();//bitmap.getWidth();
        int frameBufferHeight = getWindowManager().getDefaultDisplay().getHeight() * matOrig.cols()/*bitmap.getWidth()*/ / getWindowManager().getDefaultDisplay().getWidth();

        Bitmap frameBuffer = Bitmap.createBitmap(frameBufferWidth, frameBufferHeight, Bitmap.Config.RGB_565);

        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();

        float scaleX = (float) frameBufferWidth / screenWidth;
        float scaleY = (float) frameBufferHeight / screenHeight;

        renderView = new AndroidView(this, frameBuffer, screenWidth, screenHeight);
        graphics = new AndroidGraphics(getAssets(), frameBuffer);
        fileIO = new AndroidFileIO(getAssets());
        audio = new AndroidAudio(this);
        input = new AndroidInput(this, renderView, scaleX, scaleY);
        screen = getStartScreen();
        setContentView(renderView);
    }

    @Override
    public void onPause() {
        super.onPause();
        screen.pause();
        if (isFinishing())
            screen.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        screen.resume();
    }



    @Override
    public Input getInput() {
        return input;
    }

    @Override
    public FileIO getFileIO() {
        return fileIO;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public Audio getAudio() {
        return audio;
    }

    @Override
    public void setScreen(Screen screen) {
        if (screen == null)
            throw new IllegalArgumentException("Screen must not be null");
        this.screen.pause();
        this.screen.dispose();
        screen.resume();
        screen.update(0);
        this.screen = screen;
    }



    public Screen getCurrentScreen() {
        return screen;
    }

    @Override
    public Screen getStartScreen() {
        return new SelectScreen(this, this, matOrig, wordRecog);
    }

    @Override
    public void startNewClass()  {
        Intent intent = new Intent(this, WordRecActivityUp.class);
        startActivity(intent);
        finish();
    }
}
