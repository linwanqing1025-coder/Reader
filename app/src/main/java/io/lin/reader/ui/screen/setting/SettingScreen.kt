package io.lin.reader.ui.screen.setting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.R
import io.lin.reader.data.preferences.HistoryPreferences
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.navigation.NavigationDestination
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.screenbar.StyledTitleScreenBar
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.screen.setting.details.AppAppearanceDetails
import io.lin.reader.ui.screen.setting.details.InteractionDetails
import io.lin.reader.ui.screen.setting.details.OtherReadingSettingDetails
import io.lin.reader.ui.screen.setting.details.ReaderColorDetails
import io.lin.reader.ui.screen.setting.details.ReadingModeDetails
import io.lin.reader.ui.screen.setting.details.SortDetails
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.SettingScreenViewModel
import kotlinx.coroutines.launch

object SettingScreenDestination : NavigationDestination {
    override val route = ScreenRoute.SettingScreen.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingScreenViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentDetailType by viewModel.currentDetailType.collectAsState()
    val isShowingDetails = currentDetailType != null

    val appPreferences by viewModel.app.state.collectAsState()
    val seriesSortPreference by viewModel.seriesSort.state.collectAsState()
    val volumeSortPreference by viewModel.volumeSort.state.collectAsState()
    val bookmarkSortMethod by viewModel.bookmarkSort.state.collectAsState()
    val historyPreferences by viewModel.history.state.collectAsState()
    val readingPreferences by viewModel.reading.state.collectAsState()

    BackHandler(enabled = isShowingDetails) {
        viewModel.closeDetail()
    }

    Scaffold(
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = isShowingDetails,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    }
                },
                label = "TopBarTransition"
            ) { showingDetails ->
                if (showingDetails && currentDetailType != null) {
                    val detailTitle = stringResource(currentDetailType!!.labelRes)
                    StyledTitleScreenBar(
                        title = detailTitle,
                        canNavigateBack = true,
                        onNavIconClick = { viewModel.closeDetail() }
                    )
                } else {
                    StyledTitleScreenBar(title = stringResource(R.string.navigation_label_setting))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDetailType,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(
                            animationSpec = tween(300)
                        ))
                } else {
                    (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(
                            animationSpec = tween(300)
                        ))
                }
            },
            label = "SettingDetailsTransition"
        ) { detailType ->
            if (detailType != null) {
                Box(modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()) {
                    when (detailType) {
                        SettingScreenViewModel.SettingDetailType.APP_APPEARANCE -> AppAppearanceDetails(
                            appPreferences = appPreferences,
                            updateAppDarkMode = viewModel.app::updateDarkMode,
                            updateAppThemeColor = viewModel.app::updateThemeColor,
                            updateAppThemeContrast = viewModel.app::updateThemeContrast,
                            updateAppLanguage = viewModel.app::updateLanguage
                        )

                        SettingScreenViewModel.SettingDetailType.SORT -> SortDetails(
                            seriesSortPreferences = seriesSortPreference,
                            volumeSortPreferences = volumeSortPreference,
                            bookmarkSortMethod = bookmarkSortMethod,
                            updateSeriesSortMethod = viewModel.seriesSort::updateMethod,
                            updateSeriesOrder = viewModel.seriesSort::updateOrder,
                            updateVolumeSortMethod = viewModel.volumeSort::updateMethod,
                            updateVolumeOrder = viewModel.volumeSort::updateOrder,
                            updateBookmarkSortMethod = viewModel.bookmarkSort::updateMethod,
                        )

                        SettingScreenViewModel.SettingDetailType.READER_COLOR -> ReaderColorDetails(
                            readerColor = readingPreferences.colors,
                            onBackgroundColorChange = { viewModel.reading.updateBackgroundColor(it.toArgb()) },
                            onPageColorChange = { viewModel.reading.updatePageColor(it.toArgb()) },
                            onFilterColorChange = { viewModel.reading.updateFilterColor(it.toArgb()) }
                        )

                        SettingScreenViewModel.SettingDetailType.READING_MODE -> ReadingModeDetails(
                            readingMode = readingPreferences.readingMode,
                            onReadingModeChange = { viewModel.reading.updateReadingMode(it) },
                            pageAlignment = readingPreferences.pageAlignment,
                            onPageAlignmentChange = { viewModel.reading.updatePageAlignment(it) },
                            paddingRatio = readingPreferences.pagePaddingRatio,
                            onPaddingRatioChange = { viewModel.reading.updatePagePaddingRatio(it) },
                        )

                        SettingScreenViewModel.SettingDetailType.INTERACTION -> InteractionDetails(
                            interactionStyle = readingPreferences.interactionStyle,
                            rtlMode = readingPreferences.rtlMode,
                            updateInteractionStyle = viewModel.reading::updateInteractionStyle
                        )

                        SettingScreenViewModel.SettingDetailType.OTHER_READING_SETTING -> OtherReadingSettingDetails(
                            rtlMode = readingPreferences.rtlMode,
                            toggleRtlMode = viewModel.reading::toggleRtlMode,
                            separateCover = readingPreferences.separateCover,
                            toggleSeparateCover = viewModel.reading::toggleSeparateCover,
                            removeGutter = readingPreferences.removeGutter,
                            toggleRemoveGutter = viewModel.reading::toggleRemoveGutter,
                            fixedPageIndicator = readingPreferences.fixedPageIndicator,
                            toggleFixedPageIndicator = viewModel.reading::toggleFixedPageIndicator
                        )
                    }
                }
            } else {
                SettingScreenContent(
                    modifier = modifier.padding(innerPadding),
                    historyPreferences = historyPreferences,
                    readingPreferences = readingPreferences,

                    entryAppAppearanceDetails = { viewModel.showDetail(SettingScreenViewModel.SettingDetailType.APP_APPEARANCE) },
                    entrySortDetails = { viewModel.showDetail(SettingScreenViewModel.SettingDetailType.SORT) },

                    clearHistory = {
                        val appContext = context.applicationContext
                        viewModel.history.clearAllHistory(
                            onResult = { count ->
                                val text = if (count == 0) {
                                    appContext.getString(R.string.history_no_history_to_clear)
                                } else {
                                    appContext.getString(R.string.history_history_cleared, count)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(text)
                                }
                            }
                        )
                    },
                    toggleShowUnreadBooks = { viewModel.history.toggleShowUnreadBooks() },
                    toggleUnreadBooksExpanded = { viewModel.history.toggleUnreadBooksExpanded() },

                    entryReadingColorDetails = { viewModel.showDetail(SettingScreenViewModel.SettingDetailType.READER_COLOR) },
                    entryReadingModeDetails = { viewModel.showDetail(SettingScreenViewModel.SettingDetailType.READING_MODE) },
                    entryInteractionDetails = { viewModel.showDetail(SettingScreenViewModel.SettingDetailType.INTERACTION) },
                    entryOtherReadingSettingDetails = { viewModel.showDetail(SettingScreenViewModel.SettingDetailType.OTHER_READING_SETTING) },
                    resetAllSetting = {
                        val appContext = context.applicationContext
                        viewModel.resetAllSettings(
                            onResult = {
                                val text = appContext.getString(R.string.setting_reset_done)
                                scope.launch {
                                    snackbarHostState.showSnackbar(text)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingScreenContent(
    modifier: Modifier = Modifier,
    historyPreferences: HistoryPreferences = HistoryPreferences(
        showUnreadBooks = false,
        unreadBooksExpanded = true
    ),
    readingPreferences: ReaderPreferences = ReaderPreferences(),
    // app
    entryAppAppearanceDetails: () -> Unit = { },
    //shelf
    entrySortDetails: () -> Unit = { },
    //history
    clearHistory: () -> Unit = { },
    toggleShowUnreadBooks: () -> Unit = { },
    toggleUnreadBooksExpanded: () -> Unit = { },
    //reading
    entryReadingColorDetails: () -> Unit = { },
    entryReadingModeDetails: () -> Unit = { },
    entryInteractionDetails: () -> Unit = { },
    entryOtherReadingSettingDetails: () -> Unit = { },
    //reset
    resetAllSetting: () -> Unit = { }
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
            contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
        ) {
            // APP Appearance
            item {
                SettingSnippet {
                    SettingItem(
                        imageVector = Icons.Default.AutoAwesome,
                        hint = stringResource(R.string.setting_appearance),
                        onClick = entryAppAppearanceDetails
                    )
                    SettingItem(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        hint = stringResource(R.string.setting_sort),
                        onClick = entrySortDetails
                    )
                }
            }

            // history
            item {
                SettingSnippet {
                    var isClearing by remember { mutableStateOf(false) }
                    SettingItem(
                        imageVector = Icons.Filled.DeleteSweep,
                        hint = stringResource(R.string.setting_clear_history),
                        onClick = { isClearing = true }
                    )
                    if (isClearing) {
                        NotificationDialog(
                            title = stringResource(R.string.history_dialog_title_clear),
                            notification = stringResource(R.string.history_clear_history_notification),
                            onDismiss = { isClearing = false },
                            onConfirm = {
                                isClearing = false
                                clearHistory()
                            }
                        )
                    }
                    SettingItem(
                        imageVector = if (historyPreferences.showUnreadBooks) Icons.Filled.RemoveRedEye else Icons.Outlined.VisibilityOff,
                        hint = stringResource(R.string.setting_show_unread_books),
                        onClick = toggleShowUnreadBooks,
                        isSwitchOption = true,
                        switchChecked = historyPreferences.showUnreadBooks,
                        onSwitchChange = { toggleShowUnreadBooks() }
                    )
                    if (historyPreferences.showUnreadBooks) {
                        SettingItem(
                            imageVector = if (historyPreferences.unreadBooksExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            leadingIconSize = 40.dp,
                            hint = stringResource(R.string.setting_expand_unread_books),
                            onClick = toggleUnreadBooksExpanded,
                            isSwitchOption = true,
                            switchChecked = historyPreferences.unreadBooksExpanded,
                            onSwitchChange = { toggleUnreadBooksExpanded() }
                        )
                    }
                }
            }

            // reading
            item {
                SettingSnippet {
                    SettingItem(
                        imageVector = Icons.Default.ColorLens,
                        hint = stringResource(R.string.setting_reader_color),
                        onClick = entryReadingColorDetails,
                    )
                    SettingItem(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        leadingIconSize = 26.dp,
                        hint = stringResource(R.string.setting_reading_mode),
                        onClick = entryReadingModeDetails
                    )
                    SettingItem(
                        imageVector = Icons.Default.TouchApp,
                        hint = stringResource(R.string.setting_interaction_style),
                        onClick = entryInteractionDetails
                    )
                    SettingItem(
                        imageVector = Icons.Default.MoreHoriz,
                        hint = stringResource(R.string.setting_other_reading_settings),
                        onClick = entryOtherReadingSettingDetails
                    )
                }
            }

            // reset
            item {
                SettingSnippet {
                    var isResetting by remember { mutableStateOf(false) }
                    SettingItem(
                        imageVector = Icons.Default.RestartAlt,
                        hint = stringResource(R.string.setting_reset_all),
                        onClick = { isResetting = true }
                    )
                    if (isResetting) {
                        NotificationDialog(
                            title = stringResource(R.string.setting_reset_dialog_title),
                            notification = stringResource(R.string.setting_reset_notification),
                            onDismiss = { isResetting = false },
                            onConfirm = {
                                isResetting = false
                                resetAllSetting()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenContentPreview() {
    SettingScreenContent(
        historyPreferences = HistoryPreferences(
            showUnreadBooks = true,
            unreadBooksExpanded = true
        ),
        readingPreferences = ReaderPreferences()
    )
}

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    leadingIconSize: Dp = 28.dp,
    hint: String,
    onClick: () -> Unit,
    isSwitchOption: Boolean = false,
    switchChecked: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = { }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                vertical = dimensionResource(R.dimen.half_inner_padding_of_container),
                horizontal = dimensionResource(R.dimen.inner_padding_of_container)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageVector != null) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = hint,
                        modifier = Modifier.size(leadingIconSize),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.inner_padding_of_container)))
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }
            Text(
                text = hint,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (isSwitchOption) {
            Switch(
                checked = switchChecked,
                onCheckedChange = onSwitchChange,
                modifier = Modifier.height(28.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingSnippet(
    modifier: Modifier = Modifier,
    settingItems: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            settingItems()
        }
    }
}
