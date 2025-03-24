package com.example.phoneshop;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class ProductsActivity extends AppCompatActivity {
    private static final String LOG_TAG = ProductsActivity.class.getName();
    private static final String PREF_KEY = ProductsActivity.class.getPackage().toString();
    private static final int SECRET_KEY = 99;

    private RecyclerView mRecyclerView;
    private ArrayList<ProductItem> mProductList;
    private ProductItemAdapter mAdapter;
    private int gridNumber = 1;

    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_products);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check secret key
        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);
        if (secret_key != 99) {
            finish();
        }

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.e(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        // Listing
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mProductList = new ArrayList<>();
        mAdapter = new ProductItemAdapter(this, mProductList);
        mRecyclerView.setAdapter(mAdapter);
        initializeData();
    }

    private void initializeData() {
        String[] nameList = getResources().getStringArray(R.array.product_item_names);
        String[] storageList = getResources().getStringArray(R.array.product_item_storages);
        String[] ramList = getResources().getStringArray(R.array.product_item_rams);
        String[] priceList = getResources().getStringArray(R.array.product_item_prices);
        TypedArray imageResourcesList = getResources().obtainTypedArray(R.array.product_item_images);
        TypedArray ratingList = getResources().obtainTypedArray(R.array.product_item_rates);

        mProductList.clear();

        for (int i = 0; i < nameList.length; i++) {
            mProductList.add(new ProductItem(
                    nameList[i],
                    storageList[i],
                    ramList[i],
                    priceList[i],
                    ratingList.getFloat(i, 0),
                    imageResourcesList.getResourceId(i, 0)
            ));
        }

        imageResourcesList.recycle();
        mAdapter.notifyDataSetChanged();
    }
}