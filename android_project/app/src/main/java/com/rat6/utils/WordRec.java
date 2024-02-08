package com.rat6.utils;

import android.content.Context;

import com.rat6.utils.Utils;
import com.rat6.utils.StorageHelper;

import org.bytedeco.opencv.opencv_core.*;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

import static com.rat6.utils.Constants.WIDTH;
import static com.rat6.utils.Constants.kazlettersnet;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


public class WordRec {
    private int expansion, xLeft, xRight, yTop, yBottom;
    private Size inputSize;
    MultiLayerNetwork network;
    NativeImageLoader imageLoader;
    String[] labels;
    String[] cyrillic;

    public WordRec(int WIDTH, String netName, Context context){
        inputSize = new Size(WIDTH, WIDTH);
        network = StorageHelper.loadMultiLayerNetwork(netName, context);
        labels = StorageHelper.getKazAlphabetLatin();
        cyrillic = StorageHelper.getKazAlphabetCyrillic();
        imageLoader = new NativeImageLoader(WIDTH, WIDTH, 1);
    }

    public WordRec(Context context){
        inputSize = new Size(WIDTH, WIDTH);
        network = StorageHelper.loadMultiLayerNetwork(kazlettersnet, context);
        labels = StorageHelper.getKazAlphabetLatin();
        cyrillic = StorageHelper.getKazAlphabetCyrillic();
        imageLoader = new NativeImageLoader(WIDTH, WIDTH, 1);
    }

    public List<Rect> getDetectedWords(Mat imgOriginal){
        List<Rect> boundRect = new ArrayList<Rect>();

        Mat img = imgOriginal.clone();
        if(img.type()!=0)
            cvtColor(img, img, COLOR_RGB2GRAY);

        //Sobel(img, img, CV_8U, 1, 0, 3, 1, 0, BORDER_DEFAULT);
        //threshold(img, img, 100, 200, THRESH_OTSU);
        Canny(img, img, 80, 200);

        Mat element = getStructuringElement(MORPH_RECT, new Size(15,7));
        morphologyEx(img, img, MORPH_CLOSE, element);
        element.release();

        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        findContours(img, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        for( int i = 0; i < contours.size(); i++ ){
            Mat mMOP2f1 = new Mat();
            contours.get(i).convertTo(mMOP2f1, CV_32FC2);
            approxPolyDP(mMOP2f1, mMOP2f1, 2, true);
            mMOP2f1.convertTo(contours.get(i), CV_32S);
            Rect mbWord = boundingRect(contours.get(i));
            if (mbWord.width()>img.cols()/33 && mbWord.width()<img.cols()/2 && mbWord.height()<img.rows()/2 && mbWord.width()>mbWord.height())
                boundRect.add(mbWord);
            else mbWord.close();
            mMOP2f1.release();
        }

        contours.close();img.release();

        sortRectList(boundRect);
        return boundRect;
    }
    public void getDetectedWords(Mat imgOriginal, List<Rect> boundRect){
        if(!boundRect.isEmpty())
            boundRect.clear();
        Mat img = imgOriginal.clone();
        if(img.type()!=0)
            cvtColor(img, img, COLOR_RGB2GRAY);

        //Sobel(img, img, CV_8U, 1, 0, 3, 1, 0, BORDER_DEFAULT);
        //threshold(img, img, 100, 200, THRESH_OTSU);
        Canny(img, img, 80, 200);

        Mat element = getStructuringElement(MORPH_RECT, new Size(15,7));
        morphologyEx(img, img, MORPH_CLOSE, element);
        element.release();

        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        findContours(img, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        for( int i = 0; i < contours.size(); i++ ){
            Mat mMOP2f1 = new Mat();
            contours.get(i).convertTo(mMOP2f1, CV_32FC2);
            approxPolyDP(mMOP2f1, mMOP2f1, 2, true);
            mMOP2f1.convertTo(contours.get(i), CV_32S);
            Rect mbWord = boundingRect(contours.get(i));
            if (mbWord.width()>img.cols()/33 && mbWord.width()<img.cols()/2 && mbWord.height()<img.rows()/2 && mbWord.width()>mbWord.height())
                boundRect.add(mbWord);
            else mbWord.close();
            mMOP2f1.release();
        }

        contours.deallocate();
        img.release();

        sortRectList(boundRect);
    }


    public List<String> getWords(Mat imgOriginal, List<Rect> wordsRects){
        List<String> words = new ArrayList<String>();
        List<Rect> drawLetters = new ArrayList<Rect>();

        for(int word = 0; word < wordsRects.size(); word++) {
            Rect wordRect = wordsRects.get(word);

            expansion = 10;
            xRight = wordRect.x()+wordRect.width()-1+expansion > imgOriginal.cols()? (wordRect.x()+wordRect.width()): (wordRect.x()+wordRect.width()-1+expansion);
            xLeft = wordRect.x() < expansion? wordRect.x(): wordRect.x()-expansion;
            yTop = wordRect.y() < expansion? (wordRect.y()): (wordRect.y()-expansion);
            yBottom = wordRect.y()+wordRect.height()-1+expansion > imgOriginal.rows()? (wordRect.y()+wordRect.height()-1): (wordRect.y()+wordRect.height()-1+expansion);
            Rect expandedWordRect = new Rect(new Point(xLeft, yTop), new Point(xRight, yBottom));

            List<Rect> letterRects = getDetectedLetters(imgOriginal, expandedWordRect);

            List<String> wordList = new ArrayList<String>();
            for(Rect r: letterRects) {
                drawLetters.add(r);
                //rectangle(imgOriginal, r, new Scalar(0, 0, 255, 255));
                try {
                    Mat letterSubmat = new Mat(imgOriginal, r).clone();
                    if (letterSubmat.type() != 0)
                        cvtColor(letterSubmat, letterSubmat, COLOR_BGRA2GRAY);
                    resize(letterSubmat, letterSubmat, inputSize);
                    Canny(letterSubmat, letterSubmat, 100, 200);
                    letterSubmat.convertTo(letterSubmat, CV_64FC1, 1d / 255d, 0);
                    INDArray array = imageLoader.asMatrix(letterSubmat);
                    INDArray predicted = network.output(array, false);
                    int maxId = Nd4j.getBlasWrapper().iamax(predicted);
                    putText(imgOriginal, labels[maxId], new Point(r.x(), r.y() + r.height()), FONT_HERSHEY_TRIPLEX, 0.9, Utils.BLUE);
                    wordList.add(cyrillic[maxId]);
                    //array.close();predicted.close(); letterSubmat.release();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            words.add(Utils.join("", wordList));
            //drawLetters.addAll(letterRects);
        }

        for(Rect r: drawLetters) rectangle(imgOriginal, r, Utils.RED);

        return words;
    }
    private List<Rect> getDetectedLetters(Mat imgOriginal, Rect wordRect){
        List<Rect> lettersRects = new ArrayList<Rect>();

        Mat wordImg = new Mat(imgOriginal, wordRect).clone();//GRAY
        if(wordImg.type()!=0) cvtColor(wordImg, wordImg, COLOR_RGB2GRAY);

        Mat kernel = getStructuringElement(MORPH_RECT, new Size(3,3));
        Canny(wordImg, wordImg, 80, 200);
        morphologyEx(wordImg, wordImg, MORPH_CLOSE, kernel);
        kernel.release();

        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        findContours(wordImg, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        for(int i=0; i<contours.size(); i++){
            Rect letterRect = boundingRect(contours.get(i));
            if(letterRect.area()>0 && letterRect.width() > wordImg.cols()/33 && letterRect.width() < wordImg.cols()-(int)(wordImg.cols()/33)) {
                expansion = 5;
                xLeft = wordRect.x()+letterRect.x() < expansion? wordRect.x()+letterRect.x(): wordRect.x()+letterRect.x()-expansion;
                xRight = wordRect.x()+(letterRect.x()+letterRect.width()-1)+expansion > imgOriginal.cols()? (wordRect.x()+(letterRect.x()+letterRect.width()-1)): (wordRect.x()+(letterRect.x()+letterRect.width()-1)+expansion);
                yTop = wordRect.y()+letterRect.y() < expansion? (wordRect.y()+letterRect.y()): (wordRect.y()+letterRect.y()-expansion);
                yBottom = wordRect.y()+(letterRect.y()+letterRect.height()-1)+expansion > imgOriginal.rows()? (wordRect.y()+(letterRect.y()+letterRect.height()-1)): (wordRect.y()+(letterRect.y()+letterRect.height()-1)+expansion);
                Rect letterExpandedRect = new Rect(new Point(xLeft, yTop), new Point(xRight, yBottom));
                lettersRects.add(letterExpandedRect);
            }
        }

        wordImg.release();
        sortRectList(lettersRects);
        return lettersRects;
    }

    public Comparator<Rect> compRect = new Comparator<Rect>() {
        @Override
        public int compare(Rect o1, Rect o2) {
            return (o1.x()<o2.x()? -1: (o1.x()==o2.x()? 0: 1));
        }
    };
    public List<Rect> sortRectList(List<Rect> rects){
        Collections.sort(rects, compRect);
        return rects;
    }

}
