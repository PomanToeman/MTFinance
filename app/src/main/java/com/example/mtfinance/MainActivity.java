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
import com.example.mtfinance.src.Transaction;
import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Transaction transaction = new Transaction(123, "Hello world", LocalDateTime.of(2007, 12, 1, 5, 30), 1);

        TextView textview = findViewById(R.id.textView);
        textview.setText(transaction.toString());


    }
}