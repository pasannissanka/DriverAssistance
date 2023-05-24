package com.pasannissanka;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

public class Detector {

    private static final String YOLO_V4_TINY_1_MODEL = "custom-yolov4-tiny-detector_opt";
    private static final String YOLO_V4_TINY_2_MODEL = "custom-yolov4-tiny-detector_opt";
    private static final String YOLO_V5_N_1_MODEL = "yolov5n_customdata_scratch-sim-opt";
    private static final String YOLO_V5_N_2_MODEL = "best-sim-opt";


    public enum MODEL {
        YOLO_V4_TINY_PRETRAINED,
        YOLO_V4_TINY,
        YOLO_V5_N,
        YOLO_V5_N_PRETRAINED
    }

    private MODEL selectedModel;
    private String modelName;
    private final String LOG = "DETECTOR";

    public Detector() {
        this.selectedModel = MODEL.YOLO_V4_TINY;
    }

    public void init(AssetManager assetManager) {
        String TAG = LOG.concat("_INIT");
        String model_bin;
        String model_param;
        switch (selectedModel) {
            case YOLO_V5_N:
                modelName = YOLO_V5_N_1_MODEL;
                model_bin = YOLO_V5_N_1_MODEL + ".bin";
                model_param = YOLO_V5_N_1_MODEL + ".param";
                YOLOv5.init(assetManager, model_param, model_bin);
                Log.i(TAG, "YOLO V5 N model 1 INIT, param: " + model_param + ", bin: " + model_bin);
                break;
            case YOLO_V5_N_PRETRAINED:
                modelName = YOLO_V5_N_2_MODEL;
                model_bin = YOLO_V5_N_2_MODEL + ".bin";
                model_param = YOLO_V5_N_2_MODEL + ".param";
                YOLOv5.init(assetManager, model_param, model_bin);
                Log.i(TAG, "YOLO V5 N model 2 INIT, param: " + model_param + ", bin: " + model_bin);
                break;
            case YOLO_V4_TINY:
                modelName = YOLO_V4_TINY_1_MODEL;
                model_bin = YOLO_V4_TINY_1_MODEL + ".bin";
                model_param = YOLO_V4_TINY_1_MODEL + ".param";
                YOLOv4.init(assetManager, model_param, model_bin);
                Log.i(TAG, "YOLO V4 Tiny model 1 INIT, param: " + model_param + ", bin: " + model_bin);
                break;
            case YOLO_V4_TINY_PRETRAINED:
                modelName = YOLO_V4_TINY_2_MODEL;
                model_bin = YOLO_V4_TINY_2_MODEL + ".bin";
                model_param = YOLO_V4_TINY_2_MODEL + ".param";
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
            case YOLO_V4_TINY:
            case YOLO_V4_TINY_PRETRAINED:
                return YOLOv4.detect(bitmap, threshold, nms_threshold, kMinHits);
            case YOLO_V5_N:
            case YOLO_V5_N_PRETRAINED:
                return YOLOv5.detect(bitmap, threshold, nms_threshold, kMinHits);
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
