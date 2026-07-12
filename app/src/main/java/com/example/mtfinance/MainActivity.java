package com.example.mtfinance;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.CategoryWithTransactions;
import com.example.mtfinance.src.viewmodels.CategoryViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CategoryViewModel categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        TextView textview = findViewById(R.id.textView);

        categoryViewModel.getAllCategories().observe(this, categories -> {
            if (categories != null && !categories.isEmpty()) {

                CategoryWithTransactions category = categories.get(0);
                if (category != null) {
                    textview.setText(category.getDetails());
                }
            } else {
                textview.setText("No categories found");
            }
        });
    }
}
