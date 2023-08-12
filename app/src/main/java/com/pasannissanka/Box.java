package com.pasannissanka;

import android.graphics.Color;
import android.graphics.RectF;

public class Box {
    public float x0, y0, x1, y1;
    private final int label;
    private final float score;
    private final int id;

    private static final String[] labels_v_1 = {
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
            "speed_limit",
    };

    // Labels to Detect
    private static final String[] labels_v_2 = {
            "T_juc_ahead",
            "bend_ahead",
            "bus_only_lane",
            "bus_stop",
            "chevron_markers",
            "children_crossing_ahead",
            "directional_express_way",
            "directional_normal",
            "expressway",
            "give_way",
            "height_limit",
            "hospital",
            "level_crossing",
            "level_crossing_gates_ahead",
            "light_signal_ahead",
            "merge_ahead",
            "no_entry",
            "no_horning",
            "no_parking",
            "no_turn",
            "no_u_turn",
            "one_way",
            "parking",
            "pass",
            "pedestrian_crossing",
            "pedestrian_crossing_ahead",
            "road_closed",
            "road_narrows_ahead",
            "road_works_ahead",
            "roundabout",
            "roundabout_ahead",
            "side_road",
            "speed_limit",
            "stop",
            "turn",
    };


    public Box(float x0, float y0, float x1, float y1, int label, float score, int id) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.label = label;
        this.score = score;
        this.id = id;
    }

    public RectF getRect() {
        return new RectF(x0, y0, x1, y1);
    }

    public String getLabel() {
        Detector.MODEL selectedModel = Detector.getInstance().getSelectedModel();
        if (selectedModel== Detector.MODEL.YOLO_V4_TINY_1) {
            return labels_v_1[label];
        } else {
            return labels_v_2[label];
        }
    }

    public int getLabelId() {
        return label;
    }

    public float getScore() {
        return score;
    }

    public int getColor() {
        return Color.argb(255, 50, 205, 50);
    }

    public int getId() {
        return id;
    }
}
