package com.example.phoneshop;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import android.widget.TextView;
import android.widget.Toast;

public class EditProductActivity extends AppCompatActivity {
    private static final String LOG_TAG = RegisterActivity.class.getName();
    private static final int SECRET_KEY = 99;
    private static final String PREF_KEY = RegisterActivity.class.getPackage().toString();

    EditText nameEditText;
    EditText storageEditText;
    EditText ramEditText;
    EditText priceEditText;

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String productId = null; // Will be null if creating new product


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_product);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check secret key
        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);
        if (secret_key != SECRET_KEY) {
            finish();
        }

        // Fields
        nameEditText = findViewById(R.id.nameEditText);
        storageEditText = findViewById(R.id.storageEditText);
        ramEditText = findViewById(R.id.ramEditText);
        priceEditText = findViewById(R.id.priceEditText);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        productId = getIntent().getStringExtra("productId");
        // If editing existing product, load it
        if (productId != null) {
            mFirestore.collection("products").document(productId).get().addOnSuccessListener(document -> {
                if (document.exists()) {
                    nameEditText.setText(document.getString("name"));
                    storageEditText.setText(document.getString("storage"));
                    ramEditText.setText(document.getString("ram"));
                    priceEditText.setText(document.getDouble("price").toString());
                }
            });
        } else {
            // Change title if creating (not editing)
            TextView title = findViewById(R.id.titleTextView);
            title.setText("Új termék");
        }
    }

    public void cancel(View view) {
        finish();
    }

    public void submit(View view) {
        String name = nameEditText.getText().toString().trim();
        String storage = storageEditText.getText().toString().trim();
        String ram = ramEditText.getText().toString().trim();
        float price = Float.parseFloat(priceEditText.getText().toString().trim());

        if (name.isEmpty() || storage.isEmpty() || ram.isEmpty() || price < 0) {
            Toast.makeText(this, "Hiányzó vagy hibás mezők!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productId != null) {
            // Update existing product
            DocumentReference productRef = mFirestore.collection("products").document(productId);
            productRef.update("name", name, "storage", storage, "ram", ram, "price", price)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Sikeres szerkesztés!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Sikertelen szekesztés.", Toast.LENGTH_SHORT).show());
        } else {
            // Add new product
            ProductItem newProduct = new ProductItem(name, storage, ram, price, 0f, R.drawable.ic_phone, 0);
            mFirestore.collection("products").add(newProduct)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Sikeres létrehozás!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Sikertelen létrehozás.", Toast.LENGTH_SHORT).show());
        }
    }

}