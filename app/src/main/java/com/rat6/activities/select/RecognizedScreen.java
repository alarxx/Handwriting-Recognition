package com.rat6.activities.select;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.rat6.utils.Algorithm;
import com.rat6.utils.StorageHelper;
import com.rat6.utils.Utils;
import com.retro.androidgames.framework.StartNewClass;
import com.rat6.utils.Overlap;
import com.retro.androidgames.framework.Game;
import com.retro.androidgames.framework.Graphics;
import com.retro.androidgames.framework.Input;
import com.retro.androidgames.framework.Screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecognizedScreen extends Screen {

    android.graphics.Rect src, dst, nextButton;
    StartNewClass newClass;
    Bitmap bitmap, nextBitmap;
    String mbtext;
    int xStart, yStart;

    public RecognizedScreen(Game game, StartNewClass newClass, Bitmap bitmap, List<String> words) {
        super(game);
        this.newClass = newClass;
        this.bitmap = bitmap;

        origText = Utils.join(" ", words);

        Graphics g = game.getGraphics();

        src = new android.graphics.Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        dst =  new android.graphics.Rect(0, 0, g.getWidth(), bitmap.getHeight()*g.getWidth()/bitmap.getWidth());

        nextBitmap = g.newBitmap("buttons.png");

        xStart = g.getWidth() - (g.getWidth()/4);
        yStart = g.getHeight() - (g.getHeight()/7);
        nextButton = new android.graphics.Rect(xStart, yStart, g.getWidth(), g.getHeight());

        Log.d("RECOD_WORD_TEST", words.toString());

        Map<String, Set<String>> sim_letters = new HashMap<>();
        StorageHelper.loadMayBeLetters(sim_letters);

        List<String> text2 = updateTextT9(game.getFileIO().getAssets(), words, sim_letters);
        mbtext = Utils.join(" ", text2);
        sim_letters = null;

        Log.d("RECOD_WORD_TEST", text2.toString());
        Log.d("RECOD_WORD_TEST", mbtext);
    }

    String origText = "";

    private List<String> updateTextT9(AssetManager assetManager, List<String> text, Map<String, Set<String>> sim_letters){

        List<String> res = new ArrayList<>();

            String[] dict = StorageHelper.loadDictionary(game.getFileIO().getAssets());
            if(dict.length==0) return res;

            for(String word: text) {
                if(word.isEmpty()) continue;

                List<String> sim_words = getMBwords(word, dict);

                //String correct_word = getCorrectWord(word, sim_words, sim_letters);

                res.addAll(sim_words);
            }

        return res;
    }

    private List<String> getMBwords(String word, String[] dict) {
        List<String> words = new ArrayList<String>();
        int minDist = word.length();

        for(String line: dict){
            int dist = Algorithm.levenshtein(word, line);
            if(dist>minDist)
                continue;
            else if(dist<minDist){
                minDist=dist;
                words.clear();
                words.add(line);
            }
            else if(minDist == dist)
                words.add(line);
        }

        return words;
    }

    private String getCorrectWord(String word, List<String> sim_words, Map<String, Set<String>> sim_letters){
        List<String> correct_words = new ArrayList<>();
        int words_length = word.length();
        for(String s: sim_words){
                //ПЕРЕПИСАТЬ, А ТО ГОВНО КАКОЕ-ТО ТУТ!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if(s.length()==words_length/* && isSimi(word, s, sim_letters)*/) correct_words.add(s);
        }
    /*
        if(correct_words.isEmpty()){
            correct_words.add(word);
        }*/

        Log.d("MBWords", correct_words.toString());
        return correct_words.get(0);
    }

    private boolean isSimi(String word, String simi2, Map<String, Set<String>> dict_sim){
        boolean res = true;
        String[] w = word.split("");
        String[] str2 = simi2.split("");
        for(int i=0; i<word.length(); i++){
            if(!w[i].equals(str2[i])){
                if(!dict_sim.get(w[i]).contains(str2[i])){ res = false; }
            }
        }
        return res;
    }




    @Override
    public void update(float deltaTime) {
        List<Input.TouchEvent> touchEvents = game.getInput().getTouchEvents();
        for(int i=0; i<touchEvents.size(); i++){
            Input.TouchEvent event = touchEvents.get(i);
            checkOverlap(event);
        }
        Log.d("Words", mbtext);
    }
    private void checkOverlap(Input.TouchEvent event) {
        if (event.type == Input.TouchEvent.TOUCH_UP) {
            if (Overlap.pointInRect(nextButton, event)) {
                newClass.startNewClass();
            }
        }
    }

    @Override
    public void present(float deltaTime) {
        Graphics g = game.getGraphics();
        g.drawBitmap(bitmap, src, dst, null);
        g.drawBitmap(nextBitmap, null, nextButton, null);
        if(mbtext != null && origText!=null) {
            g.drawText(mbtext, 100, g.getHeight() - 300, 20);
            g.drawText(origText, 100, g.getHeight() - 370, 50);
        }
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void dispose() { }
}
