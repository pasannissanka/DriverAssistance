package com.example;

public class Detection {
    private final int label;
    private final int id;
    private String speed;

    Detection(int label, int id) {
        this.id = id;
        this.label = label;
        this.speed = "";
    }

    public int getLabel() {
        return label;
    }

    public int getId() {
        return id;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }
}
