package com.example.mtfinance;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.TextView;

import com.example.mtfinance.src.Category;
import com.example.mtfinance.src.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Transaction transaction = new Transaction.Builder("Groceries", BigDecimal.valueOf(1.0)).description("Shopping for bananas").build();
        Category category = new Category("Groceries", "Grocery shopping", BigDecimal.TEN);
        Category categoryTwo = new Category("Bananas", "Grocery shopping", BigDecimal.TEN);


        categoryTwo.setParent(category);



        TextView textview = findViewById(R.id.textView);
        textview.setText(category.getDetails());


    }
}