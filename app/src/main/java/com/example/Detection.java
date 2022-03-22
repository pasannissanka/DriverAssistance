package com.example;

public class Detection {
    private final int label;
    private String labelName;
    private final int id;
    private String speed;

    Detection(int label, int id) {
        this.id = id;
        this.label = label;
        this.speed = "";
    }

    Detection(String labelName,int id) {
        this.id=id;
        this.labelName=labelName;
        this.label=0;
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

    public String getLabelName(){
        return labelName;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }
}
