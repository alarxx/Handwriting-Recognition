package com.rat6.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import static android.view.Gravity.FILL;
import static android.view.Gravity.FILL_HORIZONTAL;

public class ButtonStart extends Activity implements View.OnClickListener {
    Button b;
    LinearLayout layout;
    TextView textView;
    boolean isTapped = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        textView = new TextView(this);
        textView.setText("Please write letters separately");
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        b = new Button(this);
        b.setOnClickListener(this);
        b.setText("Press to Start");
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 3f));
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        layout.addView(textView);
        layout.addView(b);

        setContentView(layout);
    }

    @Override
    public void onClick(View v) {
        if(!isTapped) {
            try {
                isTapped = true;
                startActivity(new Intent(this, Class.forName("com.rat6.activities.camera.WordRecActivityUp")));
                finish();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
