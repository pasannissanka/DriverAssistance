//
// Created by pasannissanka on 5/13/23.
//

#ifndef YOLOV5_H
#define YOLOV5_H

#include "../ncnnvulkan/include/ncnn/net.h"
#include "../opencv/include/opencv2/video/tracking.hpp"
#include "yolov4.h"


#define YOLOV5_V62 1 //YOLOv5 v6.2 export  onnx model method https://github.com/shaoshengsong/yolov5_62_export_ncnn
#if YOLOV5_V60 || YOLOV5_V62
#define MAX_STRIDE 64
#else
#define MAX_STRIDE 32
#endif //YOLOV5_V60    YOLOV5_V62

class yolov5 {

public:
    yolov5(AAssetManager *mgr, const char *param, const char *bin, bool useGPU);

    ~yolov5();

    std::vector<BoxInfo> detect(JNIEnv *env, jobject image, float threshold, float nms_threshold);

    std::vector<std::string> labels{
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
private:
    ncnn::Net *Net;

    const int target_size = 640;

    static inline float sigmoid(float x);
    static void generate_proposals(const ncnn::Mat& anchors, int stride, const ncnn::Mat& in_pad, const ncnn::Mat& feat_blob, float prob_threshold, std::vector<BoxInfo>& objects);
    static void qsort_descent_inplace(std::vector<BoxInfo>& faceobjects, int left, int right);
    static void qsort_descent_inplace(std::vector<BoxInfo>& faceobjects);
    static inline float intersection_area(const BoxInfo & a, const BoxInfo & b);
    static void nms_sorted_bboxes(const std::vector<BoxInfo>& faceobjects, std::vector<int>& picked, float nms_threshold, bool agnostic = false);


public:
    static yolov5 *yolov5_detector;
};

#endif //YOLOV5_H
