package com.example.fakeloc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.fakeloc.ui.FakeLocScreen
import com.example.fakeloc.ui.LogsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(applicationContext)
        AppLogger.i("MainActivity.onCreate, sdk=${android.os.Build.VERSION.SDK_INT} model=${android.os.Build.MODEL}")
        setContent {
            MaterialTheme {
                Surface { AppRoot() }
            }
        }
    }
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf("map") }
    when (screen) {
        "map"  -> FakeLocScreen(onShowLogs = { screen = "logs" })
        "logs" -> LogsScreen(onBack = { screen = "map" })
    }
}
