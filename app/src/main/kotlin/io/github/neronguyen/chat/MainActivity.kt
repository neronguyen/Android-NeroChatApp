package io.github.neronguyen.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.neronguyen.chat.feature.auth.presentation.navigation.AuthNavGraph
import io.github.neronguyen.chat.ui.theme.ChatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthNavGraph(
                        onAuthSuccess = {
                            // Auth successful callback
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
