package com.example.mtfinance;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;
import com.example.mtfinance.src.viewmodels.CategoryDashboardViewModel;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private CategoryDashboardViewModel categoryDashboardViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        Category category = new Category("Groceries", "Grocery shopping", BigDecimal.valueOf(100), TrackingType.EXPENSE);



        TextView textview = findViewById(R.id.textView);
        textview.setText(category.getDetails());


    }
}