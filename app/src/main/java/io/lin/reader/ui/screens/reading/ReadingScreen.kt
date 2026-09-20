package io.lin.reader.ui.maintab.reading

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.ViewModelProvider

private const val TAG = "ReadingScreen.kt"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ReadingScreen(
    modifier: Modifier = Modifier,
    bookId: Long,
    pageNumber: Int = -1,
    onNavigateUp: () -> Unit = { },
    viewModel: ReadingScreenViewModel = viewModel(factory = ViewModelProvider.Factory)
) {
    // 确保在进入页面时加载正确的书籍
    LaunchedEffect(bookId, pageNumber) {
        viewModel.loadVolume(bookId, if (pageNumber == -1) null else pageNumber)
    }

    val uiState by viewModel.uiState.collectAsState()
    val mCore by viewModel.mCore.collectAsState() // 观察核心引擎状态
    val snackbarHostState = remember { SnackbarHostState() }

    val book: Volume? = uiState.volume
    val series: Series? = uiState.series
    val error = uiState.error

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalReadingViewModel provides viewModel
            ) {
                // 同时检查业务数据和渲染引擎是否就绪
                if (book != null && series != null && mCore != null) {
                    BookReader(
                        navigateBack = onNavigateUp,
                        snackbarHostState = snackbarHostState,
                    )
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // 加载中状态：业务数据已读但引擎正在初始化
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
