package com.example;

import android.graphics.Color;
import android.graphics.RectF;

public class Box {
    public float x0, y0, x1, y1;
    private final int label;
    private final float score;

    // Labels to Detect
    private static final String[] labels = {
            "bus prority lane",
            "children crossing",
            "hospital",
            "level crossing with gate",
            "no honking",
            "no left turn",
            "no right turn",
            "no u turn",
            "other",
            "pedestrian crossing",
            "pedestrian crossing ahead",
            "speed limit",
    };

    public Box(float x0, float y0, float x1, float y1, int label, float score) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.label = label;
        this.score = score;
    }

    public RectF getRect() {
        return new RectF(x0, y0, x1, y1);
    }

    public String getLabel() {
        return labels[label];
    }

    public float getScore() {
        return score;
    }

    public int getColor() {
        if (label == 11) {
            return Color.argb(255, 220, 20, 60);
        }
        return Color.argb(255, 50, 205, 50);
    }
}