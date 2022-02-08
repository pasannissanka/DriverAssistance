#ifndef DRIVERASSISTANCE_HUNGARIANOPER_H
#define DRIVERASSISTANCE_HUNGARIANOPER_H

#include "munkres.h"
#include "datatype.h"



class HungarianOper {
public:
    static Eigen::Matrix<float, -1, 2, Eigen::RowMajor> Solve(const DYNAMICM &cost_matrix);
};

#endif // DRIVERASSISTANCE_HUNGARIANOPER_H
