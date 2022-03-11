//
// Created by pasan on 2022-03-07.
//

#ifndef SORT_H
#define SORT_H

#include "yolov4.h"

//#include <iomanip> // to format image names using setw() and setfill()
#include <set>

#include "Hungarian.h"
#include "KalmanTracker.h"

#include "opencv2/video/tracking.hpp"
//#include "opencv2/highgui/highgui.hpp"

using namespace std;

#define CUNM 20

struct Box {
    float x1;
    float y1;
    float x2;
    float y2;

    Box(float x1, float y1, float x2, float y2): x1(x1), y1(y1), x2(x2), y2(y2) {}
};

class SORT {
public:
    SORT(double threshold);
    ~SORT();

    static vector<BoxInfo> predict(vector<BoxInfo> detections);

private:
    static double getIOU(cv::Rect_ <float> bb_test, cv::Rect_<float> bb_gt);
    static Box toBox(cv::Rect_<float> rect);
    static cv::Rect_<float> toCVBox(Box box);

public:
    static SORT *detector;

private:
    // update across frames
    static int max_age;
    static int min_hits;
    static double iouThreshold;
    static vector<KalmanTracker> trackers;

    static vector<Rect_<float>> predictedBoxes;
    static unsigned int trkNum;
    static  unsigned int detNum;
    static vector<vector<double>> iouMatrix;
    static vector<int> assignment;

    static set<int> unmatchedDetections;
    static set<int> unmatchedTrajectories;
    static set<int> allItems;
    static set<int> matchedItems;
    static vector<cv::Point> matchedPairs;



};


#endif //SORT_H
