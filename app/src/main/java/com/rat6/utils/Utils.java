package com.rat6.utils;

import android.graphics.Bitmap;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.*;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.*;

import org.bytedeco.opencv.opencv_java;
import org.nd4j.linalg.factory.Nd4j;

import java.util.List;


public class Utils {

    public static AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();
    public static OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

    public static final Scalar RED = new Scalar(255, 0, 0, 255);
    public static final Scalar BLUE = new Scalar(0, 0, 255, 255);
    public static final Scalar GREEN = new Scalar(0, 255, 0, 255);
    public static final Scalar WHITE = new Scalar(255, 255, 255, 255);
    public static final Scalar BLACK = new Scalar(0, 0, 0, 255);

    public static void LoadLibraries(){
        new Nd4j();
        new opencv_java();
    }

    public static void matToBitmap(Mat mat, Bitmap bitmap){
        Frame frame = converterToMat.convert(mat);
        bitmap = converterToBitmap.convert(frame);
    }

    public static String join(String separator, List<String> input) {
        if (input == null || input.size() <= 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.size(); i++) {
            sb.append(input.get(i));
            //if not the last item
            if (i != input.size() - 1) {
                sb.append(separator);
            }
        }
        String word = sb.toString();
        return word;
    }
}
