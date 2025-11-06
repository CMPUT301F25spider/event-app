package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.ImageData;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying images in RecyclerView
 * Used in BrowseImagesActivity for Admin to view all images
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageData> images;
    private OnImageClickListener listener;

    public ImageAdapter() {
        this.images = new ArrayList<>();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageData imageData = images.get(position);
        holder.bind(imageData, listener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * Update the list of images
     */
    public void setImages(List<ImageData> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    /**
     * Set the click listener
     */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder for image items
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivImagePreview;
        private TextView tvImageType;
        private TextView tvImageUrl;
        private MaterialButton btnDeleteImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);

            ivImagePreview = itemView.findViewById(R.id.ivImagePreview);
            tvImageType = itemView.findViewById(R.id.tvImageType);
            tvImageUrl = itemView.findViewById(R.id.tvImageUrl);
            btnDeleteImage = itemView.findViewById(R.id.btnDeleteImage);
        }

        public void bind(ImageData imageData, OnImageClickListener listener) {
            // Set image type
            String type = imageData.getType() != null ? imageData.getType() : "Unknown";
            tvImageType.setText(formatType(type));

            // Set image URL (shortened)
            tvImageUrl.setText(imageData.getImageUrl());

            // TODO: Load actual image using Glide or Picasso
            // For now, just show placeholder
            ivImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);

            // Set delete button click listener
            btnDeleteImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(imageData);
                }
            });

            // Set item click listener (to view full image)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(imageData);
                }
            });
        }

        private String formatType(String type) {
            if (type.equals("event_poster")) {
                return "Event Poster";
            } else if (type.equals("profile_picture")) {
                return "Profile Picture";
            }
            return type;
        }
    }

    /**
     * Interface for handling click events
     */
    public interface OnImageClickListener {
        void onImageClick(ImageData imageData);
        void onDeleteClick(ImageData imageData);
    }
}
