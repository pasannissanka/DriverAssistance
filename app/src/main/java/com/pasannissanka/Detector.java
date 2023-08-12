package com.pasannissanka;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

public class Detector {

    private static volatile Detector INSTANCE = null;

    private static final String YOLO_V4_TINY_1 = "custom-yolov4-tiny-detector_opt";
    private static final String YOLO_V4_TINY_2 = "custom-traffic-signs-merged-yolov4-tiny-detector-ncnn_sim";


    public enum MODEL {
        YOLO_V4_TINY_1,
        YOLO_V4_TINY_2,
    }

    private MODEL selectedModel;
    private String modelName;
    private final String LOG = "DETECTOR";

    private Detector() {
        this.selectedModel = MODEL.YOLO_V4_TINY_1;
    }

    public static Detector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Detector();
        }
        return INSTANCE;
    }

    public void init(AssetManager assetManager) {
        String TAG = LOG.concat("_INIT");
        String model_bin;
        String model_param;
        switch (selectedModel) {
            case YOLO_V4_TINY_1:
                modelName = YOLO_V4_TINY_1;
                model_bin = YOLO_V4_TINY_1 + ".bin";
                model_param = YOLO_V4_TINY_1 + ".param";
                YOLOv4.init(assetManager, model_param, model_bin);
                Log.i(TAG, "YOLO V4 Tiny model 1 INIT, param: " + model_param + ", bin: " + model_bin);
                break;
            case YOLO_V4_TINY_2:
                modelName = YOLO_V4_TINY_2;
                model_bin = YOLO_V4_TINY_2 + ".bin";
                model_param = YOLO_V4_TINY_2 + ".param";
                YOLOv4.init(assetManager, model_param, model_bin);
                Log.i(TAG, "YOLO V4 Tiny model 2 INIT, param: " + model_param + ", bin: " + model_bin);
                break;
            default:
                Log.e(TAG, "Invalid model, " + selectedModel);
                break;
        }
    }

    public Box[] detect(Bitmap bitmap, double threshold, double nms_threshold, int kMinHits) {
        switch (selectedModel) {
            case YOLO_V4_TINY_1:
            case YOLO_V4_TINY_2:
                return YOLOv4.detect(bitmap, threshold, nms_threshold, kMinHits);
//            case YOLO_V5_N:
//            case YOLO_V5_N_PRETRAINED:
//                return YOLOv5.detect(bitmap, threshold, nms_threshold, kMinHits);
            default:
                Log.e(LOG.concat("_DETECT"), "Invalid model, " + selectedModel);
                return null;
        }
    }

    public MODEL getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(MODEL selectedModel) {
        Log.i(LOG.concat("_SELECT_MODEL"), "New model selected, OLD: " + this.selectedModel + ", NEW: " + selectedModel);
        this.selectedModel = selectedModel;
    }

    public String getModelName() {
        return modelName;
    }
}
