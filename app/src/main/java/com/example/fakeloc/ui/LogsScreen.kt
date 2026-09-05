package com.example.fakeloc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fakeloc.AppLogger

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(AppLogger.snapshot()) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        kotlinx.coroutines.delay(750)
        lines = AppLogger.snapshot()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back")
                }
                OutlinedButton(
                    onClick = {
                        lines = AppLogger.snapshot()
                        refreshTick++
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Refresh") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("FakeLoc logs", AppLogger.copyToString()))
                        AppLogger.i("logs copied to clipboard (${lines.size} lines)")
                        lines = AppLogger.snapshot()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Copy all") }
                OutlinedButton(
                    onClick = {
                        AppLogger.clear()
                        lines = emptyList()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
            }

            Text(
                text = "${lines.size} log line(s)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            val scroll = rememberScrollState()
            LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = Color(0xFF101418)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(8.dp)
                ) {
                    if (lines.isEmpty()) {
                        Text(
                            "(no log lines yet)",
                            color = Color(0xFF8A8A8A),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    } else {
                        lines.forEach { line ->
                            val color = when {
                                line.contains("ERROR") -> Color(0xFFFF6B6B)
                                line.contains("WARN")  -> Color(0xFFFFD166)
                                line.contains("INFO")  -> Color(0xFF8ECAE6)
                                else                   -> Color(0xFFCCCCCC)
                            }
                            Text(
                                text = line,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
