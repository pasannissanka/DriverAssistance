//
// Created by pasannissanka on 5/13/23.
//

#include "include/yolov5.h"



yolov5 *yolov5::yolov5_detector = nullptr;
bool yolov5::yolov5_hasGPU = false;

yolov5::yolov5(AAssetManager *mgr, const char *param, const char *bin, bool useGPU) {
    Net = new ncnn::Net();
    // opt 需要在加载前设置
    yolov5_hasGPU = ncnn::get_gpu_count() > 0;
    Net->opt.use_vulkan_compute = true;  // gpu
    Net->opt.use_fp16_arithmetic = true;  // fp16运算加速

    __android_log_print(ANDROID_LOG_DEBUG, "YOLOV5", "NET LOADED");
    {
        int ret = Net->load_param(mgr, param);
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, "YOLOV5", "load parmas failed");
        }
    }
    {
        int ret = Net->load_model(mgr, bin);
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, "YOLOV5", "load model failed");
        }
    }
}

yolov5::~yolov5() {
    delete Net;
}



inline float fast_exp(float x) {
    union {
        uint32_t i;
        float f;
    } v{};
    v.i = (1 << 23) * (1.4426950409 * x + 126.93490512f);
    return v.f;
}

inline float sigmoid(float x) {
    return 1.0f / (1.0f + fast_exp(-x));
}


std::vector<BoxInfo>
yolov5::detect(JNIEnv *env, jobject image, float threshold, float nms_threshold) {
    AndroidBitmapInfo img_size;
    AndroidBitmap_getInfo(env, image, &img_size);
//    ncnn::Mat in_net = ncnn::Mat::from_android_bitmap_resize(env,image,ncnn::Mat::PIXEL_BGR2RGB,input_size/2,input_size/2);
    ncnn::Mat in_net = ncnn::Mat::from_android_bitmap_resize(env, image, ncnn::Mat::PIXEL_RGBA2RGB, input_size / 2,
                                                             input_size / 2);
    float norm[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    float mean[3] = {0, 0, 0};
    in_net.substract_mean_normalize(mean, norm);

    auto ex = Net->create_extractor();
    ex.set_light_mode(true);
    ex.set_num_threads(4);
    yolov5_hasGPU = ncnn::get_gpu_count() > 0;
    ex.set_vulkan_compute(yolov5_hasGPU);

    ex.input("in0", in_net);
    ncnn::Mat out;
    ex.extract("out0", out);

    __android_log_print(ANDROID_LOG_DEBUG, "DETECTION", "%s", reinterpret_cast<const char *>(out.h));

    std::vector<BoxInfo> result;

    // the out blob would be a 2-dim tensor with w=85 h=25200
    //
    //        |cx|cy|bw|bh|box score(1)| per-class scores(80) |
    //        +--+--+--+--+------------+----------------------+
    //        |53|50|70|80|    0.11    |0.1 0.0 0.0 0.5 ......|
    //   all /|  |  |  |  |      .     |           .          |
    //  boxes |46|40|38|44|    0.95    |0.0 0.9 0.0 0.0 ......|
    // (25200)|  |  |  |  |      .     |           .          |
    //       \|  |  |  |  |      .     |           .          |
    //        +--+--+--+--+------------+----------------------+
    //

    // enumerate all boxes
    std::vector<BoxInfo> proposals;
    for (int i = 0; i < out.h; i++)
    {
        const float* ptr = out.row(i);

        const int num_class = 80;

        const float cx = ptr[0];
        const float cy = ptr[1];
        const float bw = ptr[2];
        const float bh = ptr[3];
        const float box_score = ptr[4];
        const float* class_scores = ptr + 5;

        // find class index with the biggest class score among all classes
        int class_index = 0;
        float class_score = -FLT_MAX;
        for (int j = 0; j < num_class; j++)
        {
            if (class_scores[j] > class_score)
            {
                class_score = class_scores[j];
                class_index = j;
            }
        }

        // combined score = box score * class score
        float confidence = box_score * class_score;

        // filter candidate boxes with combined score >= prob_threshold
        if (confidence < threshold)
            continue;

        // transform candidate box (center-x,center-y,w,h) to (x0,y0,x1,y1)
        float x0 = cx - bw * 0.5f;
        float y0 = cy - bh * 0.5f;
        float x1 = cx + bw * 0.5f;
        float y1 = cy + bh * 0.5f;

        // collect candidates
        BoxInfo obj;
        obj.box.x = x0;
        obj.box.y = y0;
        obj.box.width = x1 - x0;
        obj.box.height = y1 - y0;
        obj.label = class_index;
        obj.score = confidence;

        proposals.push_back(obj);
    }

//    // sort all candidates by score from highest to lowest
////    qsort_descent_inplace(proposals);
//
//    // apply non max suppression
//    std::vector<int> picked;
////    nms_sorted_bboxes(proposals, picked, nms_threshold);
//
//    // collect final result after nms
//    const int count = picked.size();
//    result.resize(count);
//    for (int i = 0; i < count; i++)
//    {
//        result[i] = proposals[picked[i]];
//
//        // adjust offset to original unpadded
////        float x0 = (result[i].box.x - (wpad / 2)) / scale;
////        float y0 = (objects[i].rect.y - (hpad / 2)) / scale;
////        float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale;
////        float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale;
//
//        // clip
//        x0 = std::max(std::min(x0, (float)(img_w - 1)), 0.f);
//        y0 = std::max(std::min(y0, (float)(img_h - 1)), 0.f);
//        x1 = std::max(std::min(x1, (float)(img_w - 1)), 0.f);
//        y1 = std::max(std::min(y1, (float)(img_h - 1)), 0.f);
//
//        result[i].box.x = x0;
//        result[i].box.y = y0;
//        result[i].box.width = x1 - x0;
//        result[i].box.height = y1 - y0;
//    }

    return proposals;
}

