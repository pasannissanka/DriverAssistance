package com.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    // Signs lookup table
    private static final int[] labelIds = {
            R.drawable.ic_bus_line,
            R.drawable.ic_child_cross,
            R.drawable.ic_hospital,
            R.drawable.ic_rail_cross,
            R.drawable.ic_no_honk,
            R.drawable.ic_no_left,
            R.drawable.ic_no_right,
            R.drawable.ic_no_u_turn,
            R.drawable.ic_speed_circle,
            R.drawable.ic_pedestrian_cross,
            R.drawable.ic_ped_cross_ahead,
            R.drawable.ic_speed_circle,
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
        holder.signImageView.setImageResource(labelIds[labelId]);

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
