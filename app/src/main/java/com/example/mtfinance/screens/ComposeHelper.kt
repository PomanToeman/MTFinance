package com.example.mtfinance.screens

// In ComposeHelpers.kt
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mtfinance.src.viewmodels.HomeViewModel

fun setComposeContent(activity: ComponentActivity, content: @Composable () -> Unit) {
    activity.setContent {
        androidx.compose.material3.MaterialTheme {
            setScafolding(content)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun setScafolding(content: @Composable () -> Unit, homeViewModel: HomeViewModel = hiltViewModel()) {
    Scaffold(
        topBar = {
            // Fixed Top Header
            TopAppBar(title = { Text("Mt finance") })
        },
        bottomBar = {
            // Fixed Bottom Footer
            BottomAppBar {
                Text("Bottom Bar", Modifier.padding(16.dp))
                Button(onClick = { homeViewModel }) {
                    Text("Test")
                }
            }
        }

    ) {

        Column(modifier = Modifier.padding(it)) {
            content()
        }
    }


}

