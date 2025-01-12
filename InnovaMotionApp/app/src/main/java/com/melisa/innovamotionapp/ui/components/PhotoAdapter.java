package com.melisa.innovamotionapp.ui.components;


import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final List<Integer> imagePaths; // Paths to images
    private final List<String> imageTimestamps; // Timestamps
    private final OnPhotoChangeListener listener; // Callback for timestamp updates
    private final Context context;

    public interface OnPhotoChangeListener {
        void onPhotoChanged(String timestamp);
    }

    public PhotoAdapter(Context context, List<Integer> imagePaths, List<String> imageTimestamps, OnPhotoChangeListener listener) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.imageTimestamps = imageTimestamps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Integer imageId = imagePaths.get(position);
        String timestamp = imageTimestamps.get(position);

        // Load image into the ImageView (e.g., with Glide or BitmapFactory)
        holder.imageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), imageId));

        // Notify the listener of the current timestamp
        listener.onPhotoChanged(timestamp);
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoImageView);
        }
    }
}
