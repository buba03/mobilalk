package com.example.phoneshop;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ProductsActivity extends AppCompatActivity {
    private static final String LOG_TAG = ProductsActivity.class.getName();
    private static final String PREF_KEY = ProductsActivity.class.getPackage().toString();
    private static final int SECRET_KEY = 99;

    // Elements
    private RecyclerView mRecyclerView;
    private ArrayList<ProductItem> mProductList;
    private ProductItemAdapter mAdapter;
    private int gridNumber = 1;
    private boolean viewRow = true;
    private int cartItemCount = 0;
    private FrameLayout redCircle;
    private TextView countTextView;

    // Firebase
    private FirebaseUser user;
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    // Notification
    private NotificationHandler mNotificationHandler;

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
        // Firestore
        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("products");
        queryData();

        // Notification
        mNotificationHandler = new NotificationHandler(this);
    }

    private void queryData() {
        // Load data from Firestore
        // If Firestore is empty, load from local storage
        mProductList.clear();

        mItems.orderBy("inCartCount", Query.Direction.DESCENDING).limit(8).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                ProductItem item = document.toObject(ProductItem.class);
                item.setId(document.getId());
                mProductList.add(item);
            }

            if (mProductList.isEmpty()) {
                initializeData();
                queryData();
            }

            mAdapter.notifyDataSetChanged();
        });
    }

    public void deleteProduct(ProductItem item) {
        DocumentReference ref = mItems.document(item._getId());
        ref.delete().addOnSuccessListener(success -> {
            Log.d(LOG_TAG, "Item successfully deleted with id: "+ item._getId());
            mNotificationHandler.send("Product deleted", item.getName());
        }).addOnFailureListener(failure -> {
            Toast.makeText(this, "Couldn't delete item with id: "+ item._getId(), Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "Couldn't delete item with id: "+ item._getId());
        });

        queryData();
    }

    private void initializeData() {
        // Get data locally
        String[] nameList = getResources().getStringArray(R.array.product_item_names);
        String[] storageList = getResources().getStringArray(R.array.product_item_storages);
        String[] ramList = getResources().getStringArray(R.array.product_item_rams);
        String[] priceList = getResources().getStringArray(R.array.product_item_prices);
        TypedArray imageResourcesList = getResources().obtainTypedArray(R.array.product_item_images);
        TypedArray ratingList = getResources().obtainTypedArray(R.array.product_item_rates);

        for (int i = 0; i < nameList.length; i++) {
            mItems.add(new ProductItem(
                    nameList[i],
                    storageList[i],
                    ramList[i],
                    priceList[i],
                    ratingList.getFloat(i, 0),
                    imageResourcesList.getResourceId(i, 0),
                    0   // inCartCount
            ));
        }
        imageResourcesList.recycle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.product_list_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchBar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                // After button press
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                // After text change
                Log.d(LOG_TAG, s);
                mAdapter.getFilter().filter(s);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // After menu item press
        int id = item.getItemId();

        if (id == R.id.logoutButton) {
            Log.d(LOG_TAG, "Log out button pressed!");
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        } else if (id == R.id.settingsButton) {
            Log.d(LOG_TAG, "Settings button pressed!");
            return true;
        } else if (id == R.id.viewSelector) {
            Log.d(LOG_TAG, "View button pressed!" + " grid: " + gridNumber + " viewRow: " + viewRow);
            if (viewRow) {
                changeSpanCount(item, R.drawable.ic_view_grid, 2);
            } else {
                changeSpanCount(item, R.drawable.ic_view_row, 1);
            }
            return true;
        } else if (id == R.id.cartButton) {
            Log.d(LOG_TAG, "Cart button pressed!");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.cartButton);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        assert rootView != null;
        redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
        countTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(alertMenuItem);
            }
        });
        return super.onPrepareOptionsMenu(menu);
    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);

        // Hide the image when span is increased
        mAdapter.setShowImages(viewRow);
        mAdapter.notifyDataSetChanged();
    }

    public void updateAlertIcon(ProductItem item) {
        cartItemCount += 1;
        Log.d(LOG_TAG, "Updating alert icon to " + cartItemCount);
        if (0 < cartItemCount) {
            countTextView.setText(String.valueOf(cartItemCount));
            redCircle.setVisibility(VISIBLE);
        } else {
            countTextView.setText("");
            redCircle.setVisibility(GONE);
        }

        redCircle.setVisibility((cartItemCount > 0) ? VISIBLE : GONE);
        mItems.document(item._getId()).update("inCartCount", item.getInCartCount() + 1)
            .addOnFailureListener(failure -> {
                Toast.makeText(this, "Couldn't update item's 'inCartCount' with id: "+ item._getId(), Toast.LENGTH_LONG).show();
            }
        );

        queryData();
    }
}