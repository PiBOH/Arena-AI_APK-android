package com.arenaai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arenaai.app.ui.screens.WebViewScreen
import com.arenaai.app.ui.theme.AgonAppTheme
import kotlinx.coroutines.flow.MutableStateFlow

// Global state to trigger the info dialog from the WebView
val showAppInfoDialog = MutableStateFlow(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AgonAppTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val showInfo by showAppInfoDialog.collectAsState(initial = false)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            WebViewScreen(url = "https://arena.ai/")
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog.value = false },
            title = { Text("App Info") },
            text = {
                Column {
                    Text("Author: PiBOH", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version: Arena AI_V1.0.1e-stable_gem3.1proprew")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Builted with Design Arena AI", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("website:", style = MaterialTheme.typography.bodySmall)
                    Text("https://arena.ai/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog.value = false }) {
                    Text("Chiudi")
                }
            }
        )
    }
}
