package io.lin.reader.ui.screen.reading

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.navigation.NavigationDestination
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.ReadingScreenViewModel

private const val TAG = "ReadingScreen"

object ReadingScreenDestination : NavigationDestination {
    override val route = ScreenRoute.ReadingScreen.name
    const val bookIdArg = "bookId"
    const val pageNumberArg = "pageNumber"

    // 修改为支持可选页码参数的形式
    val routeWithArgs = "$route/{$bookIdArg}?$pageNumberArg={$pageNumberArg}"
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ReadingScreen(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit = { },
    viewModel: ReadingScreenViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val book: Volume? = uiState.volume
    val series: Series? = uiState.series
    val error = uiState.error

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (book != null && series != null) {
                ReaderSurface(
                    viewModel = viewModel,
                    navigateBack = onNavigateUp,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $error")
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
