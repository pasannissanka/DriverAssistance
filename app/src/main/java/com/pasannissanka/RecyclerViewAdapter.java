package com.pasannissanka;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Stack;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    // Signs lookup table
    private static final int[] labelIds_v1 = {
            R.drawable.ic_bus_line, // "bus prority lane"
            R.drawable.ic_child_cross, // "children crossing"
            R.drawable.ic_hospital, // "hospital"
            R.drawable.ic_rail_cross, // "level crossing with gate"
            R.drawable.ic_no_honk, // "no honking"
            R.drawable.ic_no_left, // "no left turn"
            R.drawable.ic_no_right, // "no right turn"
            R.drawable.ic_no_u_turn, // "no u turn"
            R.drawable.ic_speed_circle, // "other"
            R.drawable.ic_pedestrian_cross, // "pedestrian crossing"
            R.drawable.ic_ped_cross_ahead, // "pedestrian crossing ahead"
            R.drawable.ic_speed_circle, // "speed_limit"
    };
//    TODO
    private static final int[] labelIds_v2 = {
            R.drawable.ic_bus_line,  // T_juc_ahead
            R.drawable.ic_child_cross,  // bend_ahead
            R.drawable.ic_bus_line,  // bus_only_lane
            R.drawable.ic_rail_cross,  // bus_stop
            R.drawable.ic_no_honk,  // chevron_markers
            R.drawable.ic_child_cross,  // children_crossing_ahead
            R.drawable.ic_no_right,  // directional_express_way
            R.drawable.ic_no_u_turn,  // directional_normal
            R.drawable.ic_speed_circle,  // expressway
            R.drawable.ic_pedestrian_cross,  // give_way
            R.drawable.ic_ped_cross_ahead,  // height_limit
            R.drawable.ic_hospital,  // hospital
            R.drawable.ic_bus_line,  // level_crossing
            R.drawable.ic_child_cross,  // level_crossing_gates_ahead
            R.drawable.ic_hospital,  // light_signal_ahead
            R.drawable.ic_rail_cross,  // merge_ahead
            R.drawable.ic_no_honk,  // no_entry
            R.drawable.ic_no_honk,  // no_horning
            R.drawable.ic_no_right,  // no_parking
            R.drawable.ic_no_left,  // no_turn
            R.drawable.ic_no_u_turn,  // no_u_turn
            R.drawable.ic_pedestrian_cross,  // one_way
            R.drawable.ic_ped_cross_ahead,  // parking
            R.drawable.ic_speed_circle,  // pass
            R.drawable.ic_pedestrian_cross,  // pedestrian_crossing
            R.drawable.ic_ped_cross_ahead,  // pedestrian_crossing_ahead
            R.drawable.ic_speed_circle,  // road_closed
            R.drawable.ic_no_left,  // road_narrows_ahead
            R.drawable.ic_no_right,  // road_works_ahead
            R.drawable.ic_no_u_turn,  // roundabout
            R.drawable.ic_speed_circle,  // roundabout_ahead
            R.drawable.ic_pedestrian_cross,  // side_road
            R.drawable.ic_speed_circle,  // speed_limit
            R.drawable.ic_speed_circle,  // stop
            R.drawable.ic_speed_circle,  // turn
    };

    private final Stack<Detection> detections;

    public RecyclerViewAdapter(Stack<Detection> detections, Context mContext) {
        this.detections = detections;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: called");
        // Lookup label drawable
        int labelId = detections.get(position).getLabel();
        Detector.MODEL selectedModel = Detector.getInstance().getSelectedModel();
        int label_res_id = selectedModel.equals(Detector.MODEL.YOLO_V4_TINY_1) ? labelIds_v1[labelId] : labelIds_v2[labelId];
        holder.signImageView.setImageResource(label_res_id);

        // pop detected sign after 10s
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!detections.empty()) {
                    detections.pop();
                }
            }
        }, 10000);
    }


    @Override
    public int getItemCount() {
        return detections.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView signImageView;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            signImageView = itemView.findViewById(R.id.iv_signs);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
