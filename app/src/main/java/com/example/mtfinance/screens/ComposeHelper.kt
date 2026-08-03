package com.example.mtfinance.screens

// In ComposeHelpers.kt
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

fun setComposeContent(activity: ComponentActivity, content: @Composable () -> Unit) {
    activity.setContent {
        androidx.compose.material3.MaterialTheme {
            content()
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetScaffolding(content: @Composable () -> Unit, nav: NavHostController) {

    Scaffold(
        topBar = {

            TopAppBar(title = { Text("Mt finance") })
        },
        bottomBar = {

            BottomAppBar(modifier = Modifier.padding(11.dp)) {


                Button(onClick = { nav.navigate("category") }, modifier = Modifier.padding(11.dp)) {
                    Text("Category")
                }
                Button(onClick = {nav.navigate("transaction")}) {
                    Text("Transaction")
                }
                Button(onClick = { nav.navigate("home")}) {
                    Text("Home")
                }



            }
        }

    ) {

        Column(modifier = Modifier.padding(it)) {
            content()
        }
    }


}

