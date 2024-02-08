package com.rat6.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_RGB2BGR;

public class StorageHelper {
    final static String TAG = "StorageHelper";

    public static MultiLayerNetwork loadMultiLayerNetwork(String a, Context context){
        try {
            return MultiLayerNetwork.load(getPath(a, context), false);
        }catch (IOException e){e.printStackTrace();}
        return null;
    }

    private static File getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();

        try (BufferedInputStream inputStream = new BufferedInputStream(assetManager.open(file))){
            File outFile = new File(context.getFilesDir(), file);
            if(outFile.length()>0) return outFile;

            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.

            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            Log.d("MultiLayerNetworkDL4J", "file uploaded" + outFile.getAbsolutePath());
            return outFile;
        } catch (IOException ex) {
            Log.d("MultiLayerNetworkDL4J", "Failed to upload a file");
        }
        return new File("");
    }

    public static CascadeClassifier loadClassifierCascade(Context context, int resId) {
        FileOutputStream fos = null;
        InputStream inputStream;

        inputStream = context.getResources().openRawResource(resId);
        File xmlDir = context.getDir("xml", Context.MODE_PRIVATE);
        File cascadeFile = new File(xmlDir, "temp.xml");
        try {
            fos = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Can\'t load the cascade file");
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        CascadeClassifier detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        if (detector.isNull()) {
            Log.e(TAG, "Failed to load cascade classifier");
            detector = null;
        } else {
            Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
        }
        // delete the temporary directory
        cascadeFile.delete();

        return detector;
    }

    public static File loadXmlFromRes2File(Context context, int resId, String filename) {
        FileOutputStream fos = null;
        InputStream inputStream;

        inputStream = context.getResources().openRawResource(resId);
        File trainDir = context.getDir("xml", Context.MODE_PRIVATE);
        File trainFile = new File(trainDir, filename);
        try {
            fos = new FileOutputStream(trainFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            Log.d(TAG, "Can\'t load the train file");
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return trainFile;
    }

    public static File saveMat2File(Mat mat, String filePath, String fileName) {
        File path = new File(filePath);
        if (!path.exists()) {
            path.mkdir();
        }
        File file = new File(path, fileName);
        Mat mat2Save = new Mat();
        cvtColor(mat, mat2Save, COLOR_RGB2BGR);
        boolean result = imwrite(file.toString(), mat2Save);
        mat2Save.release();
        if (result)
            return file;
        else
            return null;
    }

    public static String[] getKazAlphabetLatin(){
        return new String[]{"j", "z", "i", "iy", "k", "k,", "l", "m", "n", "n1", "a", "o", "o-", "p", "r", "s", "t", "u", "u1-", "u1", "f",
                "a1", "h", "h1" ,"c" ,"ch" ,"sh", "sh'", "]", "y", "y1", "]'", "b" ,"eh", "yu" ,"ya", "v", "g", "g1", "d", "e", "eo"};
    }

    public static String[] getKazAlphabetCyrillic(){
        return new String[]{"ж", "з", "и", "й", "к", "қ", "л", "м", "н", "ң", "а", "о", "ө", "п", "р", "с", "т", "у", "ұ", "ү", "ф",
                "ә", "х", "һ" ,"ц" ,"ч" ,"ш", "щ", "ъ", "ы", "і", "ь", "б" ,"э", "ю" ,"я", "в", "г", "ғ", "д", "е", "ё"};
    }

    public static String[] loadDictionary(AssetManager assetManager){
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(Constants.kz_dict)))){
            String[] dict = new String[Constants.SUM_WORDS_IN_DICT];
            for(int i=0; i<Constants.SUM_WORDS_IN_DICT; i++){
                dict[i] = reader.readLine();
            }
            return dict;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    public static void loadMayBeLetters(Map<String, Set<String>> sim_letters){
        sim_letters.put("а", new HashSet<>(Arrays.asList("о")));
        sim_letters.put("ә", new HashSet<>(Arrays.asList()));
        sim_letters.put("б", new HashSet<>(Arrays.asList()));
        sim_letters.put("в", new HashSet<>(Arrays.asList("з")));
        sim_letters.put("г", new HashSet<>(Arrays.asList()));
        sim_letters.put("ғ", new HashSet<>(Arrays.asList()));
        sim_letters.put("д", new HashSet<>(Arrays.asList("у")));
        sim_letters.put("е", new HashSet<>(Arrays.asList()));
        sim_letters.put("ё", new HashSet<>(Arrays.asList()));
        sim_letters.put("ж", new HashSet<>(Arrays.asList()));
        sim_letters.put("з", new HashSet<>(Arrays.asList("в")));
        sim_letters.put("и", new HashSet<>(Arrays.asList()));
        sim_letters.put("й", new HashSet<>(Arrays.asList()));
        sim_letters.put("к", new HashSet<>(Arrays.asList("қ")));
        sim_letters.put("қ", new HashSet<>(Arrays.asList("к", "ң")));
        sim_letters.put("л", new HashSet<>(Arrays.asList()));
        sim_letters.put("м", new HashSet<>(Arrays.asList()));
        sim_letters.put("н", new HashSet<>(Arrays.asList( "ы", "п")));
        sim_letters.put("ң", new HashSet<>(Arrays.asList("қ", "н")));
        sim_letters.put("о", new HashSet<>(Arrays.asList()));
        sim_letters.put("ө", new HashSet<>(Arrays.asList()));
        sim_letters.put("с", new HashSet<>(Arrays.asList()));
        sim_letters.put("т", new HashSet<>(Arrays.asList("з")));
        sim_letters.put("у", new HashSet<>(Arrays.asList("ү", "ү", "д")));
        sim_letters.put("ұ", new HashSet<>(Arrays.asList("ү", "у")));
        sim_letters.put("ү", new HashSet<>(Arrays.asList("ұ", "у")));
        sim_letters.put("ф", new HashSet<>(Arrays.asList()));
        sim_letters.put("х", new HashSet<>(Arrays.asList()));
        sim_letters.put("һ", new HashSet<>(Arrays.asList()));
        sim_letters.put("ц", new HashSet<>(Arrays.asList()));
        sim_letters.put("ч", new HashSet<>(Arrays.asList()));
        sim_letters.put("ш", new HashSet<>(Arrays.asList()));
        sim_letters.put("щ", new HashSet<>(Arrays.asList()));
        sim_letters.put("ъ", new HashSet<>(Arrays.asList()));
        sim_letters.put("ы", new HashSet<>(Arrays.asList("н", "ь", "ъ")));
        sim_letters.put("і", new HashSet<>(Arrays.asList()));
        sim_letters.put("ь", new HashSet<>(Arrays.asList("н", "ы")));
        sim_letters.put("э", new HashSet<>(Arrays.asList()));
        sim_letters.put("ю", new HashSet<>(Arrays.asList("н")));
        sim_letters.put("я", new HashSet<>(Arrays.asList()));
    }
}
