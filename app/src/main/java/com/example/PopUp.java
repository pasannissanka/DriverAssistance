package com.example;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.TextView;

import java.util.Objects;

public class PopUp extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        setContentView(R.layout.activity_pop_up);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int w = dm.widthPixels;
        int h = dm.heightPixels;

        // Set layout to 50% of width and 80% height of parent activity
        getWindow().setLayout((int)(w * .5), (int)(h * .8));

        Intent intent = getIntent();
        float limit = intent.getFloatExtra("speed", 50.0f);

        TextView tvSpeedLimit = findViewById(R.id.popup_tv_speed_limit);
        tvSpeedLimit.setText(String.valueOf((int) limit));
    }
}