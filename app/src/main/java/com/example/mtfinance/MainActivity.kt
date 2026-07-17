package com.example.mtfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.mtfinance.screens.MyApp
import com.example.mtfinance.screens.setComposeContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(this) {
            MyApp()
        }
    }
}
