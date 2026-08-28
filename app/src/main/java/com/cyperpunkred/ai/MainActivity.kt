package com.cyperpunkred.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cyperpunkred.ai.data.local.datastore.UserPreferences
import com.cyperpunkred.ai.ui.navigation.CyberpunkRedNavHost
import com.cyperpunkred.ai.ui.theme.CyberpunkRedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsState(initial = com.cyperpunkred.ai.data.local.datastore.ThemeMode.DYNAMIC)
            CyberpunkRedTheme(themeMode = themeMode) {
                CyberpunkRedNavHost()
            }
        }
    }
}
