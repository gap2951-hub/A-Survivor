package com.a_survivor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.a_survivor.app.ui.GameScreen
import com.a_survivor.app.ui.theme.ASurvivorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASurvivorTheme {
                GameScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}