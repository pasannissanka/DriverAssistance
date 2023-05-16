//
// Created by pasannissanka on 5/13/23.
//

#include "include/yolov5.h"

yolov5 *yolov5::yolov5_detector = nullptr;

yolov5::yolov5(AAssetManager *mgr, const char *param, const char *bin, bool useGPU) {
    yolov4::hasGPU = ncnn::get_gpu_count() > 0;
    yolov4::toUseGPU = yolov4::hasGPU && useGPU;

    Net = new ncnn::Net();
    Net->opt.use_vulkan_compute = yolov4::toUseGPU;  // GPU

    Net->load_param(mgr, param);
    Net->load_model(mgr, bin);

}

yolov5::~yolov5() {

}

float yolov5::sigmoid(float x) {
    return static_cast<float>(1.f / (1.f + exp(-x)));
}


void yolov5::generate_proposals(const ncnn::Mat& anchors, int stride, const ncnn::Mat& in_pad, const ncnn::Mat& feat_blob, float prob_threshold, std::vector<BoxInfo>& objects)
{
    const int num_grid = feat_blob.h;

    int num_grid_x;
    int num_grid_y;
    if (in_pad.w > in_pad.h)
    {
        num_grid_x = in_pad.w / stride;
        num_grid_y = num_grid / num_grid_x;
    }
    else
    {
        num_grid_y = in_pad.h / stride;
        num_grid_x = num_grid / num_grid_y;
    }

    const int num_class = feat_blob.w - 5;

    const int num_anchors = anchors.w / 2;

    for (int q = 0; q < num_anchors; q++)
    {
        const float anchor_w = anchors[q * 2];
        const float anchor_h = anchors[q * 2 + 1];

        const ncnn::Mat feat = feat_blob.channel(q);

        for (int i = 0; i < num_grid_y; i++)
        {
            for (int j = 0; j < num_grid_x; j++)
            {
                const float* featptr = feat.row(i * num_grid_x + j);
                float box_confidence =  yolov5::sigmoid(featptr[4]);
                if (box_confidence >= prob_threshold)
                {
                    // find class index with max class score
                    int class_index = 0;
                    float class_score = -FLT_MAX;
                    for (int k = 0; k < num_class; k++)
                    {
                        float score = featptr[5 + k];
                        if (score > class_score)
                        {
                            class_index = k;
                            class_score = score;
                        }
                    }
                    float confidence = box_confidence * sigmoid(class_score);
                    if (confidence >= prob_threshold)
                    {
                        // yolov5/models/yolo.py Detect forward
                        // y = x[i].sigmoid()
                        // y[..., 0:2] = (y[..., 0:2] * 2. - 0.5 + self.grid[i].to(x[i].device)) * self.stride[i]  # xy
                        // y[..., 2:4] = (y[..., 2:4] * 2) ** 2 * self.anchor_grid[i]  # wh

                        float dx = sigmoid(featptr[0]);
                        float dy = sigmoid(featptr[1]);
                        float dw = sigmoid(featptr[2]);
                        float dh = sigmoid(featptr[3]);

                        float pb_cx = (dx * 2.f - 0.5f + j) * stride;
                        float pb_cy = (dy * 2.f - 0.5f + i) * stride;

                        float pb_w = pow(dw * 2.f, 2) * anchor_w;
                        float pb_h = pow(dh * 2.f, 2) * anchor_h;

                        float x0 = pb_cx - pb_w * 0.5f;
                        float y0 = pb_cy - pb_h * 0.5f;
                        float x1 = pb_cx + pb_w * 0.5f;
                        float y1 = pb_cy + pb_h * 0.5f;

                        BoxInfo obj;

                        obj.box.x = x0;
                        obj.box.y = y0;
                        obj.box.width = x1 - x0;
                        obj.box.height = y1 - y0;
                        obj.label = class_index;
                        obj.score = confidence;

                        objects.push_back(obj);
                    }
                }
            }
        }
    }
}

#pragma clang diagnostic push
#pragma ide diagnostic ignored "openmp-use-default-none"
void yolov5::qsort_descent_inplace(std::vector<BoxInfo>& faceobjects, int left, int right)
{
    int i = left;
    int j = right;
    float p = faceobjects[(left + right) / 2].score;

    while (i <= j)
    {
        while (faceobjects[i].score > p)
            i++;

        while (faceobjects[j].score < p)
            j--;

        if (i <= j)
        {
            // swap
            std::swap(faceobjects[i], faceobjects[j]);

            i++;
            j--;
        }
    }

#pragma omp parallel sections
    {
#pragma omp section
        {
            if (left < j) qsort_descent_inplace(faceobjects, left, j);
        }
#pragma omp section
        {
            if (i < right) qsort_descent_inplace(faceobjects, i, right);
        }
    }
}
#pragma clang diagnostic pop

void yolov5::qsort_descent_inplace(std::vector<BoxInfo>& faceobjects)
{
    if (faceobjects.empty())
        return;

    qsort_descent_inplace(faceobjects, 0, faceobjects.size() - 1);
}

inline float yolov5::intersection_area(const BoxInfo & a, const BoxInfo & b)
{
    cv::Rect_<float> inter = a.box & b.box;
    return inter.area();
}

void yolov5::nms_sorted_bboxes(const std::vector<BoxInfo>& faceobjects, std::vector<int>& picked, float nms_threshold, bool agnostic)
{
    picked.clear();

    const int n = faceobjects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++)
    {
        areas[i] = faceobjects[i].box.area();
    }

    for (int i = 0; i < n; i++)
    {
        const BoxInfo & a = faceobjects[i];

        int keep = 1;
        for (int j = 0; j < (int)picked.size(); j++)
        {
            const BoxInfo & b = faceobjects[picked[j]];

            if (!agnostic && a.label != b.label)
                continue;

            // intersection over union
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            // float IoU = inter_area / union_area
            if (inter_area / union_area > nms_threshold)
                keep = 0;
        }

        if (keep)
            picked.push_back(i);
    }
}


std::vector<BoxInfo>
yolov5::detect(JNIEnv *env, jobject image, float threshold, float nms_threshold) {
    AndroidBitmapInfo img_size;
    AndroidBitmap_getInfo(env, image, &img_size);

    int img_w = img_size.width;
    int img_h = img_size.height;

    // letterbox pad to multiple of MAX_STRIDE
    int w = img_w;
    int h = img_h;
    float scale = 1.f;
    if (w > h)
    {
        scale = (float)target_size / w;
        w = target_size;
        h = h * scale;
    }
    else
    {
        scale = (float)target_size / h;
        h = target_size;
        w = w * scale;
    }

    ncnn::Mat in_net = ncnn::Mat::from_android_bitmap_resize(env, image, ncnn::Mat::PIXEL_RGBA2RGB,
                                                             target_size,
                                                             target_size);
    // pad to target_size rectangle
    // yolov5/utils/datasets.py letterbox
    int wpad = (w + MAX_STRIDE - 1) / MAX_STRIDE * MAX_STRIDE - w;
    int hpad = (h + MAX_STRIDE - 1) / MAX_STRIDE * MAX_STRIDE - h;

    ncnn::Mat in_pad;
    ncnn::copy_make_border(in_net, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 114.f);

    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = Net->create_extractor();
    ex.input("images", in_pad);

    std::vector<BoxInfo> proposals;

    // anchor setting from yolov5/models/yolov5s.yaml

    // stride 8
    {
        ncnn::Mat out;
        ex.extract("output", out);

        ncnn::Mat anchors(6);
        anchors[0] = 10.f;
        anchors[1] = 13.f;
        anchors[2] = 16.f;
        anchors[3] = 30.f;
        anchors[4] = 33.f;
        anchors[5] = 23.f;

        std::vector<BoxInfo> objects8;
        generate_proposals(anchors, 8, in_pad, out, threshold, objects8);

        proposals.insert(proposals.end(), objects8.begin(), objects8.end());
    }
    // stride 16
    {
        ncnn::Mat out;

#if YOLOV5_V62
        ex.extract("353", out);
#elif YOLOV5_V60
        ex.extract("376", out);
#else
        ex.extract("781", out);
#endif

        ncnn::Mat anchors(6);
        anchors[0] = 30.f;
        anchors[1] = 61.f;
        anchors[2] = 62.f;
        anchors[3] = 45.f;
        anchors[4] = 59.f;
        anchors[5] = 119.f;

        std::vector<BoxInfo> objects16;
        generate_proposals(anchors, 16, in_pad, out, threshold, objects16);

        proposals.insert(proposals.end(), objects16.begin(), objects16.end());
    }
    // stride 32
    {
        ncnn::Mat out;
#if YOLOV5_V62
        ex.extract("367", out);
#elif YOLOV5_V60
        ex.extract("401", out);
#else
        ex.extract("801", out);
#endif
        ncnn::Mat anchors(6);
        anchors[0] = 116.f;
        anchors[1] = 90.f;
        anchors[2] = 156.f;
        anchors[3] = 198.f;
        anchors[4] = 373.f;
        anchors[5] = 326.f;

        std::vector<BoxInfo> objects32;
        generate_proposals(anchors, 32, in_pad, out, threshold, objects32);

        proposals.insert(proposals.end(), objects32.begin(), objects32.end());
    }
    // sort all proposals by score from highest to lowest
    qsort_descent_inplace(proposals);

    // apply nms with nms_threshold
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, nms_threshold);

    return proposals;
}

