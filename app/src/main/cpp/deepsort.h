//
// Created by pasan on 2022-02-07.
//

#ifndef DRIVERASSISTANCE_DEEPSORT_H
#define DRIVERASSISTANCE_DEEPSORT_H

#include <memory>
#include <vector>

#include "yolov4.h"
#include "tracker.h"
#include "model.h"

struct OptionsDeepSORT {
    OptionsDeepSORT() {
        nn_budget = 100;//len(feature);
        max_cosine_distance = 0.6;
        max_iou_distance = 0.7;
        max_age = 70;
        n_init = 3;
    }
    int nn_budget;
    float max_cosine_distance;
    float max_iou_distance;
    int max_age;
    int n_init;
};


class deepsort {
public:
    deepsort();
    ~deepsort();

    int init();
    int load_detections(std::vector<BoxInfo>& obj_info);
    int update();
    int get_results(std::vector<BoxInfo>& obj_info, std::vector<int>& obj_id);
private:
    OptionsDeepSORT options_deepsort_;
    std::unique_ptr<tracker> tracker_;
    DETECTIONS detections_;

public:
    static deepsort *deepsortDetector;
};


#endif //DRIVERASSISTANCE_DEEPSORT_H
