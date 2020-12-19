package com.rat6.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ButtonStart extends Activity implements View.OnClickListener {
    Button b;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = new Button(this);
        b.setOnClickListener(this);
        b.setText("Start");
        setContentView(b);
    }

    @Override
    public void onClick(View v) {
        try {
            startActivity(new Intent(this, Class.forName("com.rat6.activities.camera.WordRecActivityUp")));
            finish();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
