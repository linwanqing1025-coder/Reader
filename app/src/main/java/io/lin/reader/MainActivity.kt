package io.lin.reader

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.lin.reader.ui.theme.ReaderTheme
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appPreferences by viewModel.appPreferences.collectAsState()

            ReaderTheme(
                darkMode = appPreferences.theme,
                themeColor = appPreferences.themeColor,
                themeContrast = appPreferences.themeContrast
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReaderApp()
                }
            }
        }
    }
}
