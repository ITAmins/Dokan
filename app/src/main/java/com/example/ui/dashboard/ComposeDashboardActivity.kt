package com.example.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.navigation.DokanAppScaffold
import com.example.ui.theme.DailyCashNotebookTheme

class ComposeDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DailyCashNotebookTheme {
                DokanAppScaffold(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
