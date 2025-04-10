package com.example.practics_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.practics_2.ui.theme.Practics_2Theme

class PropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra("name") ?: "Unknown"
        val height = intent.getIntExtra("height", -1)
        val weight = intent.getIntExtra("weight", -1)
        val url = intent.getStringExtra("URL") ?: "Unknown"

        setContent {
            Practics_2Theme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "URL: $url")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Name: $name")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Height: $height")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Weight: $weight")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { finish() }) {
                        Text("Назад")
                    }
                }
            }
        }
    }
}