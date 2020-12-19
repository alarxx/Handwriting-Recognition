package com.rat6;

import android.graphics.Bitmap;

import com.rat6.utils.WordRec;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Extra {
    //public static Bitmap bitmap;
    public static Mat mat;
    public static Set<Integer> wordsRectsIDs;
    public static WordRec wordRecog;

    public Extra(){
        mat = new Mat();
        wordsRectsIDs = new HashSet<Integer>();
    }
}
