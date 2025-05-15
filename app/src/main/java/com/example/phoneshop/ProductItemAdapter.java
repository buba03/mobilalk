package com.example.phoneshop;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ProductItemAdapter extends RecyclerView.Adapter<ProductItemAdapter.ViewHolder> implements Filterable {
    private ArrayList<ProductItem> mProductItemsData;
    private ArrayList<ProductItem> mProductItemsDataAll;
    private Context mContext;
    private int lastPosition = -1;
    private boolean showImages = true;

    ProductItemAdapter(Context context, ArrayList<ProductItem> itemsData) {
        this.mContext = context;
        this.mProductItemsData = itemsData;
        this.mProductItemsDataAll = itemsData;
    }

    public void setShowImages(boolean showImages) {
        this.showImages = showImages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.product_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductItemAdapter.ViewHolder holder, int position) {
        ProductItem currentItem = mProductItemsData.get(position);

        holder.bindTo(currentItem);
        if (holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }

        // Hide image if grid view
        if (showImages) {
            holder.mImageView.setVisibility(View.VISIBLE);
        } else {
            holder.mImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mProductItemsData.size();
    }

    @Override
    public Filter getFilter() {
        return productFilter;
    }

    private Filter productFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<ProductItem> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if (charSequence == null || charSequence.length() == 0) {
                results.count = mProductItemsDataAll.size();
                results.values = mProductItemsDataAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for (ProductItem item : mProductItemsDataAll) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }

                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mProductItemsData = (ArrayList<ProductItem>) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;
        private TextView mStorageTextView;
        private TextView mRamTextView;
        private TextView mPriceTextView;
        private RatingBar mRatingBar;
        private ImageView mImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitleTextView = itemView.findViewById(R.id.itemTitle);
            mStorageTextView = itemView.findViewById(R.id.itemStorage);
            mRamTextView = itemView.findViewById(R.id.itemRAM);
            mPriceTextView = itemView.findViewById(R.id.itemPrice);
            mRatingBar = itemView.findViewById(R.id.itemRatingBar);
            mImageView = itemView.findViewById(R.id.itemImage);
        }

        public void bindTo(ProductItem currentItem) {
            mTitleTextView.setText(currentItem.getName());
            mStorageTextView.setText(currentItem.getStorage());
            mRamTextView.setText(currentItem.getRam());
            mPriceTextView.setText((int) currentItem.getPrice() + " Ft");
            Glide.with(mContext).load(currentItem.getImageResource()).into(mImageView);
            mRatingBar.setRating(currentItem.getRating());

            // Cart button
            itemView.findViewById(R.id.addToCartButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("Activity", "Add to cart button pressed!");
                    Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.scale);
                    view.startAnimation(animation);
                    ((ProductsActivity) mContext).updateAlertIcon(currentItem);
                }
            });
            // Delete button
            itemView.findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("Activity", "Delete from cart button pressed!");
                    Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.scale);
                    view.startAnimation(animation);
                    ((ProductsActivity) mContext).deleteProduct(currentItem);
                }
            });
            // Edit button
            itemView.findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("Activity", "Edit button pressed!");
                    Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.scale);
                    view.startAnimation(animation);
                    ((ProductsActivity) mContext).editProduct(currentItem);
                }
            });
        }
    }
}


