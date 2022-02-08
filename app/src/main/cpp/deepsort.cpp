//
// Created by pasan on 2022-02-07.
//

#include "deepsort.h"

deepsort *deepsort::deepsortDetector = nullptr;

deepsort::deepsort() {
    options_deepsort_ = OptionsDeepSORT();
    detections_.clear();
}

deepsort::~deepsort() {
}

int deepsort::init() {
    tracker_.reset(new tracker(options_deepsort_.max_cosine_distance,
                               options_deepsort_.nn_budget,
                               options_deepsort_.max_iou_distance,
                               options_deepsort_.max_age,
                               options_deepsort_.n_init));
    return 0;
}

int deepsort::load_detections(std::vector<BoxInfo>& obj_info) {
    for(unsigned int i = 0; i < obj_info.size(); i++)
    {
        DETECTION_ROW box;
        box.tlwh = DETECTBOX(obj_info[i].x1, obj_info[i].y1, obj_info[i].x2 - obj_info[i].x1, obj_info[i].y2 - obj_info[i].y1);
        for(int j = 0; j < 128; j++)
        {
            box.feature[j] = obj_info[i].ids_[j];
        }
        detections_.push_back(box);
    }
    return 0;
}

int deepsort::update() {
    tracker_->predict();
    tracker_->update(detections_);
    detections_.clear();
    return 0;
}

int deepsort::get_results(std::vector<BoxInfo>& obj_info, std::vector<int>& obj_id) {
    for(Track& track : tracker_->tracks) {
        if(!track.is_confirmed() || track.time_since_update > 1) continue;
        obj_id.push_back(track.track_id);
        // TO DO: track.to_tlwh() -> obj_info
        BoxInfo temp;
        temp.x1 = track.to_tlwh()[0];
        temp.x2 = track.to_tlwh()[2] + temp.x1;
        temp.y1 = track.to_tlwh()[1];
        temp.y2 = track.to_tlwh()[3] + temp.y1;
        obj_info.push_back(temp);
    }
    return 0;
}