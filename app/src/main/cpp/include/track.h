#pragma once

#include "../opencv/include/opencv2/core.hpp"
#include "kalman_filter.h"

class Track
{
public:
    // Constructor
    Track();

    // Destructor
    ~Track() = default;

    void Init(const cv::Rect &bbox, int lbl, float s);
    void Predict();
    void Update(const cv::Rect &bbox);
    cv::Rect GetStateAsBbox() const;
    float GetNIS() const;

    int coast_cycles_ = 0, hit_streak_ = 0;
    int label = -1;
    float score = 0;


private:
    Eigen::VectorXd ConvertBboxToObservation(const cv::Rect &bbox) const;
    cv::Rect ConvertStateToBbox(const Eigen::VectorXd &state) const;

    KalmanFilter kf_;
};
