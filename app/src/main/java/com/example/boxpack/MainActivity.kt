package com.example.boxpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.boxpack.ui.BoxPackViewModel
import com.example.boxpack.ui.navigation.BoxPackApp
import com.example.boxpack.ui.theme.BoxPackTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BoxPackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            BoxPackTheme(
                themeMode = uiState.settings.themeMode,
                dynamicColor = uiState.settings.dynamicColor
            ) {
                BoxPackApp(viewModel = viewModel)
            }
        }
    }
}
