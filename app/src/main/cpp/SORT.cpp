//
// Created by pasan on 2022-03-07.
//

#include "SORT.h"

SORT *SORT::detector = nullptr;

int SORT::max_age = 1;
int SORT::min_hits = 3;
double SORT::iouThreshold = 0;
vector<KalmanTracker> SORT::trackers;

vector<Rect_<float>> SORT::predictedBoxes;
unsigned int SORT::trkNum = 0;
unsigned int SORT::detNum = 0;
vector<vector<double>> SORT::iouMatrix;
vector<int> SORT::assignment;

set<int> SORT::unmatchedDetections;
set<int> SORT::unmatchedTrajectories;
set<int> SORT::allItems;
set<int> SORT::matchedItems;
vector<cv::Point> SORT::matchedPairs;

SORT::SORT(double threshold) {
    SORT::iouThreshold = threshold;
}

SORT::~SORT() {}

vector<BoxInfo> SORT::predict(vector<BoxInfo> detections) {
    vector<BoxInfo> frameTrackingResult;

    if (trackers.empty()) // the first frame met
    {
        // initialize kalman trackers using first detections.
        for (unsigned int i = 0; i < detections.size(); i++) {
            KalmanTracker trk = KalmanTracker(toCVBox(Box(
                    detections[i].x1,
                    detections[i].y1,
                    detections[i].x2,
                    detections[i].y2
            )));
            trackers.push_back(trk);
        }
        // output the first frame detections
        for (unsigned int id = 0; id < detections.size(); id++) {
            BoxInfo tb = detections[id];
            return {tb};
        }
    }

    ///////////////////////////////////////
    // 3.1. get predicted locations from existing trackers.
    predictedBoxes.clear();

    for (auto it = trackers.begin(); it != trackers.end();) {
        Rect_<float> pBox = (*it).predict();
        if (pBox.x >= 0 && pBox.y >= 0) {
            predictedBoxes.push_back(pBox);
            it++;
        } else {
            it = trackers.erase(it);
            //cerr << "Box invalid at frame: " << frame_count << endl;
        }
    }

    ///////////////////////////////////////
    // 3.2. associate detections to tracked object (both represented as bounding boxes)
    // dets : detFrameData[fi]
    trkNum = predictedBoxes.size();
    detNum = detections.size();

    iouMatrix.clear();
    iouMatrix.resize(trkNum, vector<double>(detNum, 0));

    for (unsigned int i = 0; i < trkNum; i++) // compute iou matrix as a distance matrix
    {
        for (unsigned int j = 0; j < detNum; j++) {
            // use 1-iou because the hungarian algorithm computes a minimum-cost assignment.
            iouMatrix[i][j] = 1 - getIOU(predictedBoxes[i], toCVBox(Box(
                    detections[j].x1,
                    detections[j].y1,
                    detections[j].x2,
                    detections[j].y2
            )));
        }
    }

    // solve the assignment problem using hungarian algorithm.
    // the resulting assignment is [track(prediction) : detection], with len=preNum
    HungarianAlgorithm HungAlgo;
    assignment.clear();
    HungAlgo.Solve(iouMatrix, assignment);

    // find matches, unmatched_detections and unmatched_predictions
    unmatchedTrajectories.clear();
    unmatchedDetections.clear();
    allItems.clear();
    matchedItems.clear();

    if (detNum > trkNum) //	there are unmatched detections
    {
        for (unsigned int n = 0; n < detNum; n++)
            allItems.insert(n);

        for (unsigned int i = 0; i < trkNum; ++i)
            matchedItems.insert(assignment[i]);

        set_difference(allItems.begin(), allItems.end(),
                       matchedItems.begin(), matchedItems.end(),
                       insert_iterator<set<int>>(unmatchedDetections, unmatchedDetections.begin()));
    } else if (detNum < trkNum) // there are unmatched trajectory/predictions
    {
        for (unsigned int i = 0; i < trkNum; ++i)
            if (assignment[i] ==
                -1) // unassigned label will be set as -1 in the assignment algorithm
                unmatchedTrajectories.insert(i);
    } else;

    // filter out matched with low IOU
    matchedPairs.clear();
    for (unsigned int i = 0; i < trkNum; ++i) {
        if (assignment[i] == -1) // pass over invalid values
            continue;
        if (1 - iouMatrix[i][assignment[i]] < iouThreshold) {
            unmatchedTrajectories.insert(i);
            unmatchedDetections.insert(assignment[i]);
        } else
            matchedPairs.push_back(cv::Point(i, assignment[i]));
    }

    ///////////////////////////////////////
    // 3.3. updating trackers

    // update matched trackers with assigned detections.
    // each prediction is corresponding to a tracker
    int detIdx, trkIdx;
    for (unsigned int i = 0; i < matchedPairs.size(); i++) {
        trkIdx = matchedPairs[i].x;
        detIdx = matchedPairs[i].y;
        trackers[trkIdx].update(toCVBox(Box(
                detections[detIdx].x1,
                detections[detIdx].y1,
                detections[detIdx].x2,
                detections[detIdx].y2
        )));
    }

    // create and initialise new trackers for unmatched detections
    for (auto umd : unmatchedDetections) {
        KalmanTracker tracker = KalmanTracker(toCVBox(Box(
                detections[umd].x1,
                detections[umd].y1,
                detections[umd].x2,
                detections[umd].y2
        )));
        trackers.push_back(tracker);
    }

    // get trackers' output
    frameTrackingResult.clear();
    for (auto it = trackers.begin(); it != trackers.end();) {
        if (((*it).m_time_since_update < 1) &&
            ((*it).m_hit_streak >= min_hits)) {
            BoxInfo res;
            Box b = toBox((*it).get_state());
            res.x1 = b.x1;
            res.y1 = b.y1;
            res.x2 = b.x2;
            res.y2 = b.y2;
            res.id = (*it).m_id + 1;
//            res.frame = frame_count;
            frameTrackingResult.push_back(res);
            it++;
        } else
            it++;

        // remove dead tracklet
        if (it != trackers.end() && (*it).m_time_since_update > max_age)
            it = trackers.erase(it);

    }

    return frameTrackingResult;
}

double SORT::getIOU(cv::Rect_<float> bb_test, cv::Rect_<float> bb_gt) {
    float in = (bb_test & bb_gt).area();
    float un = bb_test.area() + bb_gt.area() - in;

    if (un < DBL_EPSILON)
        return 0;
    return (double) (in / un);
}

Box SORT::toBox(cv::Rect_<float> rect) {
    Box box = Box(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y);
    return box;
}

cv::Rect_<float> SORT::toCVBox(Box box) {
    float w = abs(box.x1 - box.x2);
    float h = abs(box.y1 - box.y2);
    cv::Rect_<float> rect = cv::Rect2f(box.x1, box.y1 - h, h, w);
    return rect;
}
