package com.example.phoneshop;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.O)
public class ProductsActivity extends AppCompatActivity {
    private static final String LOG_TAG = ProductsActivity.class.getName();
    private static final String PREF_KEY = ProductsActivity.class.getPackage().toString();
    private static final int SECRET_KEY = 99;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final String COLLECTION_NAME = "Products_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH"));

    // Elements
    private RecyclerView mRecyclerView;
    private ArrayList<ProductItem> mProductList;
    private ProductItemAdapter mAdapter;
    private int gridNumber = 1;
    private boolean viewRow = true;
    private int cartItemCount = 0;
    private FrameLayout redCircle;
    private TextView countTextView;

    // Preferences
    private SharedPreferences preferences;

    // Firebase
    private FirebaseUser user;
    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    // Notification
    private NotificationHandler mNotificationHandler;
    // Alarm
    private AlarmManager mAlarmManager;

    // Filtering
    enum FilterMode {
        IN_CART_COUNT,
        BUDGET,
        TOP_5_RATED
    }

    private FilterMode queryMode;

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

        // Preferences
        preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE);

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.e(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        // Listing
        queryMode = FilterMode.IN_CART_COUNT;
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mProductList = new ArrayList<>();
        mAdapter = new ProductItemAdapter(this, mProductList);
        mRecyclerView.setAdapter(mAdapter);
        // Firestore
        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection(COLLECTION_NAME);
        queryData();

        // Permission
        checkNotificationPermission();
        requestNotificationPermission();
        // Notification
        mNotificationHandler = new NotificationHandler(this);
        // Alarm
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

    private void queryData() {
        // Load data from Firestore
        // If Firestore is empty, load from local storage
        mProductList.clear();

        if (queryMode == FilterMode.IN_CART_COUNT) {
            Log.d(LOG_TAG, "IN_CART_COUNT");
            mItems.orderBy("inCartCount", Query.Direction.DESCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {
                mProductList.clear();
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
                return;
            });
        }

        if (queryMode == FilterMode.BUDGET) {
            mItems.whereLessThan("price", 100000).orderBy("price", Query.Direction.ASCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {
                mProductList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    ProductItem item = document.toObject(ProductItem.class);
                    item.setId(document.getId());
                    mProductList.add(item);
                    Log.d(LOG_TAG, item.getName());
                }

                if (mProductList.isEmpty()) {
                    initializeData();
                    queryData();
                }

                mAdapter.notifyDataSetChanged();
                return;
            });

        }

        if (queryMode == FilterMode.TOP_5_RATED) {
            mItems.orderBy("rating", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener(queryDocumentSnapshots -> {
                mProductList.clear();
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
                return;
            });
        }
    }

    public void deleteProduct(ProductItem item) {
        DocumentReference ref = mItems.document(item._getId());
        ref.delete().addOnSuccessListener(success -> {
            Log.d(LOG_TAG, "Item successfully deleted with id: " + item._getId());
            mNotificationHandler.send("Termék törölve!", item.getName());
        }).addOnFailureListener(failure -> {
            Toast.makeText(this, "Couldn't delete item with id: " + item._getId(), Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "Couldn't delete item with id: " + item._getId());
        });

        queryData();
    }

    public void editProduct(ProductItem item) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        intent.putExtra("collectionName", COLLECTION_NAME);
        if (item != null) {
            intent.putExtra("productId", item._getId());
        }
        startActivity(intent);
    }

    private void initializeData() {
        // Get data locally
        String[] nameList = getResources().getStringArray(R.array.product_item_names);
        String[] storageList = getResources().getStringArray(R.array.product_item_storages);
        String[] ramList = getResources().getStringArray(R.array.product_item_rams);
        TypedArray priceList = getResources().obtainTypedArray(R.array.product_item_prices);
        TypedArray imageResourcesList = getResources().obtainTypedArray(R.array.product_item_images);
        TypedArray ratingList = getResources().obtainTypedArray(R.array.product_item_rates);

        for (int i = 0; i < nameList.length; i++) {
            mItems.add(new ProductItem(
                    nameList[i],
                    storageList[i],
                    ramList[i],
                    priceList.getFloat(i, 0),
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
        // Load
        getMenuInflater().inflate(R.menu.product_list_menu, menu);
        // Search
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

        // Logout
        if (id == R.id.logoutButton) {
            Log.d(LOG_TAG, "Log out button pressed!");
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;

        // Settings
        } else if (id == R.id.settingsButton) {
            Log.d(LOG_TAG, "Settings button pressed!");
            return true;

        // Add
        } else if (id == R.id.createButton) {
            Log.d(LOG_TAG, "Create button pressed!");
            editProduct(null);
            return true;

        // Filter
        } else if (id == R.id.filter_popular) {
            Log.d(LOG_TAG, "'filter_popular' button pressed!");
            queryMode = FilterMode.IN_CART_COUNT;
            queryData();
            return true;
        } else if (id == R.id.filter_budget) {
            Log.d(LOG_TAG, "'filter_budget' button pressed!");
            queryMode = FilterMode.BUDGET;
            queryData();
            return true;
        } else if (id == R.id.filter_top_rated) {
            Log.d(LOG_TAG, "'filter_top_rated' button pressed!");
            queryMode = FilterMode.TOP_5_RATED;
            queryData();
            return true;

        // Change view
        } else if (id == R.id.viewSelector) {
            Log.d(LOG_TAG, "View button pressed!" + " grid: " + gridNumber + " viewRow: " + viewRow);
            if (viewRow) {
                changeSpanCount(item, R.drawable.ic_view_grid, 2);
            } else {
                changeSpanCount(item, R.drawable.ic_view_row, 1);
            }
            return true;

        // Cart
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

        // Update item in Firestore
        mItems.document(item._getId()).update("inCartCount", item.getInCartCount() + 1)
                .addOnFailureListener(failure -> {
                            Toast.makeText(this, "Couldn't update item's 'inCartCount' with id: " + item._getId(), Toast.LENGTH_LONG).show();
                        }
                );

        queryData();
    }

    private void setAlarmManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!mAlarmManager.canScheduleExactAlarms()) {
                Log.w(LOG_TAG, "Exact alarms not permitted. Consider requesting permission.");
                Toast.makeText(this, "Hiányzó jogosultságok!", Toast.LENGTH_LONG);
                return;
            }
        }
        // Notification after 10 seconds
        long repeatInterval = 10 * 1000;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, FLAG_IMMUTABLE);

        mAlarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
        );
    }

    @Override
    protected void onResume() {
        // Refresh data after resuming (because of editing/creating)
        super.onResume();
        queryData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cartItemCount > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAlarmManager();
            }
        }
    }

    private void checkNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission not granted, show request popup
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to send notifications
            } else {
                // Permission denied, inform the user or disable notification-related features
            }
        }
    }
}