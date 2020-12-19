package com.rat6.activities.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.rat6.activities.select.SelectWordsActivity;
import com.rat6.opencv.CvCameraView;
import com.rat6.utils.Constants;
import com.rat6.utils.Overlap;
import com.rat6.Extra;
import com.rat6.utils.Utils;
import com.rat6.utils.WordRec;
import com.retro.androidgames.framework.Graphics;
import com.retro.androidgames.framework.Input.TouchEvent;

import org.bytedeco.opencv.opencv_core.*;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * new opencv_core.Rect(leftX, topY, width, height);
 * new android.graphics.Rect(leftX, topY, rightX, BottomY);
 */

public class WordRecActivityUp extends Activity implements CvCameraView.CvCameraViewListener {
    //Надо бы сделать чтобы ширина и высота выбирались автоматически и оптимально

    private CvCameraView cameraView;

    android.graphics.Rect take_a_picture_Button;

    Bitmap ertas;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.LoadLibraries();
        initCameraView();
        new Extra();
        Extra.wordRecog = new WordRec(this);

        setContentView(cameraView);
    }

    private void initCameraView(){
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        cameraView = new CvCameraView(this, CvCameraView.CAMERA_BACK, isLandscape);
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraSize(Constants.CAMERA_W, Constants.CAMERA_H);
    }

    @Override public void onCameraViewStarted(int camera_width, int camera_height){
        int hFrame = camera_height * cameraView.getCANVAS_WIDTH() / camera_width;
        take_a_picture_Button = new android.graphics.Rect(0, hFrame, cameraView.getCANVAS_WIDTH(), cameraView.getCANVAS_HEIGHT());
        ertas = cameraView.getGraphics().newBitmap("ertas.png");
    }



    @Override
    public Mat onCameraFrame(Mat imgOriginal) { //RGBA
        imgOriginal.copyTo(Extra.mat);
        List<Rect> wordsRects = new ArrayList<Rect>();
        Extra.wordRecog.getDetectedWords(imgOriginal, wordsRects);

        //List<String> words = wordRec.getWords(imgOriginal, wordsRects);//Включает распознавание слов в реальном времени

        for(Rect r: wordsRects)
            rectangle(imgOriginal, r, Utils.GREEN, 2, LINE_AA, 0);

        return imgOriginal;
    }

    @Override
    public void update(float deltaTime){
        List<TouchEvent> touchEvents = cameraView.getInput().getTouchEvents();

        for(int i=0; i<touchEvents.size(); i++){
            TouchEvent event = touchEvents.get(i);
            if(event.type == TouchEvent.TOUCH_UP) {
                if(Overlap.pointInRect(take_a_picture_Button, event)) {
                    startActivity(new Intent(this, SelectWordsActivity.class));
                    finish();
                }
            }
        }
    }

    @Override
    public void present(float deltaTime){
        Graphics g = cameraView.getGraphics();
        g.drawBitmap(ertas, 10, 10, null);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Utils.LoadLibraries();
        initCameraView();
        new Extra();
        Extra.wordRecog = new WordRec(this);
        setContentView(cameraView);
    }

    @Override public void onCameraViewStopped(){}
    @Override public void onResume(){super.onResume();}
    @Override public void onPause(){super.onPause();}
}
