//
// Created by pasannissanka on 5/13/23.
//

#ifndef YOLOV5_H
#define YOLOV5_H

#include "../ncnnvulkan/include/ncnn/net.h"
#include "../opencv/include/opencv2/video/tracking.hpp"
#include "yolov4.h"
#include <android/log.h>


#define YOLOV5_V62 1 //YOLOv5 v6.2 export  onnx model method https://github.com/shaoshengsong/yolov5_62_export_ncnn
#if YOLOV5_V60 || YOLOV5_V62
#define MAX_STRIDE 64
#else
#define MAX_STRIDE 32
class YoloV5Focus : public ncnn::Layer
{
public:
    YoloV5Focus()
    {
        one_blob_only = true;
    }

    virtual int forward(const ncnn::Mat& bottom_blob, ncnn::Mat& top_blob, const ncnn::Option& opt) const
    {
        int w = bottom_blob.w;
        int h = bottom_blob.h;
        int channels = bottom_blob.c;

        int outw = w / 2;
        int outh = h / 2;
        int outc = channels * 4;

        top_blob.create(outw, outh, outc, 4u, 1, opt.blob_allocator);
        if (top_blob.empty())
            return -100;

#pragma omp parallel for num_threads(opt.num_threads)
        for (int p = 0; p < outc; p++)
        {
            const float* ptr = bottom_blob.channel(p % channels).row((p / channels) % 2) + ((p / channels) / 2);
            float* outptr = top_blob.channel(p);

            for (int i = 0; i < outh; i++)
            {
                for (int j = 0; j < outw; j++)
                {
                    *outptr = *ptr;

                    outptr += 1;
                    ptr += 2;
                }

                ptr += w;
            }
        }

        return 0;
    }
};

DEFINE_LAYER_CREATOR(YoloV5Focus)

#endif //YOLOV5_V60    YOLOV5_V62



class yolov5 {

public:
    yolov5(AAssetManager *mgr, const char *param, const char *bin, bool useGPU);

    ~yolov5();

    std::vector<BoxInfo> detect(JNIEnv *env, jobject image, float threshold, float nms_threshold);

    std::vector<std::string> labels{
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
