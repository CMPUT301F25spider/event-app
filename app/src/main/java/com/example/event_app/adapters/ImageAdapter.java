package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.ImageData;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying a grid of images with an overlaid delete button.
 * <p>
 * Each item represents an {@link ImageData} object and supports two interactions:
 * tapping the image itself or tapping the delete icon. Both actions are forwarded
 * through {@link OnImageClickListener}.
 * <p>
 * Used in admin tools such as AdminBrowseImagesActivity.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageData> images;
    private OnImageClickListener listener;

    /**
     * Creates a new {@code ImageAdapter} with an empty image list.
     */
    public ImageAdapter() {
        this.images = new ArrayList<>();
    }

    /**
     * Inflates the layout for an image item and wraps it in an {@link ImageViewHolder}.
     *
     * @param parent   the parent view that will contain this item
     * @param viewType not used (single view type)
     * @return a fully constructed {@link ImageViewHolder}
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Binds the {@link ImageData} at the given position to the provided ViewHolder.
     *
     * @param holder   the ViewHolder to update
     * @param position the adapter position of the item to bind
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageData imageData = images.get(position);
        holder.bind(imageData, listener);
    }

    /**
     * Returns the number of images currently stored in the adapter.
     *
     * @return total image count
     */
    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * Replaces the current list of images and refreshes the RecyclerView.
     *
     * @param images a list of {@link ImageData} objects to display
     */
    public void setImages(List<ImageData> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    /**
     * Registers a listener to receive callbacks when an image or its delete button is clicked.
     *
     * @param listener the callback interface implementation
     */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder representing a single image item in the admin image grid.
     * <p>
     * Displays the photo using an {@link ImageView} and overlays a delete
     * {@link ImageButton}. Provides callbacks for image tap and delete actions.
     */
    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnDelete;

        /**
         * Creates a new {@code ImageViewHolder} and locates all child views.
         *
         * @param itemView the inflated layout for one image grid item
         */
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        /**
         * Binds an {@link ImageData} object to the UI components of this ViewHolder.
         * <p>
         * - Loads the image via Glide
         * - Handles delete-button clicks
         * - Handles main image clicks
         *
         * @param imageData the image metadata, including its download URL
         * @param listener  callback for handling click actions (may be null)
         */
        public void bind(ImageData imageData, OnImageClickListener listener) {
            // Load actual image with Glide
            Glide.with(itemView.getContext())
                    .load(imageData.getImageUrl())
                    .centerCrop()
                    .placeholder(R.color.gray_light)
                    .error(android.R.drawable.ic_menu_gallery)  // Fallback if load fails
                    .into(ivImage);

            // Delete button click
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(imageData);
                }
            });

            // Optional: Click on image to view details
            ivImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(imageData);
                }
            });
        }
    }

    /**
     * Listener interface for handling user interactions on image items.
     * Provides callbacks for tapping the image itself and tapping its delete button.
     */
    public interface OnImageClickListener {
        /**
         * Called when the user taps an image item.
         *
         * @param imageData the {@link ImageData} associated with the tapped item
         */
        void onImageClick(ImageData imageData);

        /**
         * Called when the delete button is pressed for an image.
         *
         * @param imageData the {@link ImageData} that should be removed
         */
        void onDeleteClick(ImageData imageData);
    }
}