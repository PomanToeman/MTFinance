package com.example.mtfinance;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Transaction transaction = new Transaction.Builder("Groceries", BigDecimal.valueOf(1.0)).description("Shopping for bananas").build();
        Category category = new Category("Groceries", "Grocery shopping", BigDecimal.TEN, TrackingType.EXPENSE);
        Category categoryTwo = new Category("Bananas", "Grocery shopping", BigDecimal.TEN, TrackingType.EXPENSE);


        categoryTwo.setParent(category);



        TextView textview = findViewById(R.id.textView);
        textview.setText(category.getDetails());


    }
}