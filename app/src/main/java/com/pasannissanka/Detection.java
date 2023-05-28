package com.pasannissanka;

public class Detection {
    private final int label;
    private String speed;

    Detection(int label) {
        this.label = label;
        this.speed = "";
    }

    public int getLabel() {
        return label;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }
}
