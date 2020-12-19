package com.rat6.activities;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListAct extends ListActivity {
    String tests[]={
            "com.rat6.activities.camera.WordRecActivityUp",
            "com.rat6.activities.Main",
            "com.rat6.activities.camera.WordRecActivityImpl"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tests ));
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id){
        super.onListItemClick(list, view, position, id);
        String testPosition = tests[position];
        try {
            Class cl = Class.forName(testPosition);
            Intent intent = new Intent(this, cl);
            startActivity(intent);
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
