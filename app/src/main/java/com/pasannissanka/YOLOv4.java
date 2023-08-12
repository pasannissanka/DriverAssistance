package com.pasannissanka;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class YOLOv4 {
    static {
        System.loadLibrary("detector");
    }

    public static native void init(AssetManager manager, String param, String bin);
    public static native Box[] detect(Bitmap bitmap, double threshold, double nms_threshold, int kMinHits);
}
