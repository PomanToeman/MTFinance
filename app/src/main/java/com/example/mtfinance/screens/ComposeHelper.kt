package com.example.mtfinance.screens

// In ComposeHelpers.kt
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

fun setComposeContent(activity: ComponentActivity, content: @Composable () -> Unit) {
    activity.setContent {
        androidx.compose.material3.MaterialTheme {
            content()
        }
    }
}