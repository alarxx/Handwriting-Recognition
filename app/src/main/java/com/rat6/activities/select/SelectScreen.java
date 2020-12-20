package com.rat6.activities.select;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.rat6.utils.Utils;
import com.rat6.utils.WordRec;
import com.retro.androidgames.framework.StartNewClass;
import com.rat6.utils.Overlap;
import com.retro.androidgames.framework.Game;
import com.retro.androidgames.framework.Graphics;
import com.retro.androidgames.framework.Input.TouchEvent;
import com.retro.androidgames.framework.Screen;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class SelectScreen extends Screen {

    private android.graphics.Rect cam_src, cam_dst, nextButton;

    private Set<Integer> wordsRectsIDs;
    private StartNewClass newClass;

    Paint transGreen = new Paint();
    private Bitmap bitmap, nextBitmap;
    private List<Rect> wordsRects;

    private WordRec wordRecog;
    private Mat matOrig;

    private AndroidFrameConverter converterToBitmap;
    private OpenCVFrameConverter.ToMat converterToMat;

    int xStart, yStart;

    public SelectScreen(Game game, StartNewClass newClass, Mat matOrig, WordRec wordRecog) {
        super(game);
        this.newClass = newClass;
        this.wordRecog = wordRecog;
        this.matOrig = matOrig;

        converterToBitmap = new AndroidFrameConverter();
        converterToMat = new OpenCVFrameConverter.ToMat();

        wordsRects = new ArrayList<Rect>();
        wordRecog.getDetectedWords(matOrig, wordsRects);

        Mat copy = new Mat(matOrig).clone();
        for(Rect r: wordsRects) rectangle(copy, r, Utils.GREEN, 2, LINE_AA, 0);
        Frame frame = converterToMat.convert(copy);
        bitmap = converterToBitmap.convert(frame);
        copy.release();

        wordsRectsIDs = new HashSet<Integer>();

        cam_src = new android.graphics.Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        cam_dst =  new android.graphics.Rect(0, 0, game.getGraphics().getWidth(), bitmap.getHeight()*game.getGraphics().getWidth()/bitmap.getWidth());

        Graphics g = game.getGraphics();

        nextBitmap = g.newBitmap("buttons.png");

        xStart = g.getWidth() - (g.getWidth()/4);
        yStart = g.getHeight() - (g.getHeight()/7);
        nextButton = new android.graphics.Rect(xStart, yStart, g.getWidth(), g.getHeight());


        transGreen.setARGB(100, 0, 255, 0);
    }

    @Override
    public void update(float deltaTime) {
        List<TouchEvent> touchEvents = game.getInput().getTouchEvents();
        for(int i=0; i<touchEvents.size(); i++){
            TouchEvent event = touchEvents.get(i);
            checkOverlap(event);
        }
    }

    @Override
    public void present(float deltaTime) {
        Graphics g = game.getGraphics();
        g.drawBitmap(bitmap, cam_src, cam_dst, null);
        for (int i : wordsRectsIDs) {
            Rect r = wordsRects.get(i);
            g.drawRect(r.x(), r.y(), r.width(), r.height(), transGreen);
        }
        g.drawBitmap(nextBitmap, null, nextButton, null);
    }

    List<String> words;
    private void checkOverlap(TouchEvent event){
        if(event.type == TouchEvent.TOUCH_DRAGGED) {
            for (int i = 0; i < wordsRects.size(); i++) {
                if (Overlap.pointInRect(wordsRects.get(i), event)) {
                    wordsRectsIDs.add(i);
                    return;
                }
            }
        }
        else if(event.type == TouchEvent.TOUCH_UP) {
            if (Overlap.pointInRect(nextButton, event)) {

                select_Rects();
                words = new ArrayList<String>();
                recognize();

                game.setScreen(new RecognizedScreen(game, newClass, bitmap, words));
            }
        }
    }

    public void select_Rects(){
        List<Rect> selectedWordRect = new ArrayList<Rect>();
        for(int i=0; i<wordsRects.size(); i++)
            if(wordsRectsIDs.contains(i))
                selectedWordRect.add(wordsRects.get(i));
        wordsRects.clear();
        wordsRects.addAll(selectedWordRect);
    }

    private void recognize(){
        wordRecog.sortRectList(wordsRects);
        words = wordRecog.getWords(matOrig, wordsRects);
        for(Rect r: wordsRects)
            rectangle(matOrig, r, Utils.GREEN, 2, LINE_AA, 0);
        Frame frame = converterToMat.convert(matOrig);
        bitmap = converterToBitmap.convert(frame);
        matOrig.release();
    }



    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void dispose() { }
}
