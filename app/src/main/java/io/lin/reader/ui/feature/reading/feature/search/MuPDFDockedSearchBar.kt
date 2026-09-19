package io.lin.reader.ui.feature.reading.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artifex.mupdf.fitz.Quad
import io.lin.reader.ui.feature.reading.DocumentSearchUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * 搜索内容展示状态的密封接口，用于在 AnimatedContent 中携带数据快照
 */
private sealed interface SearchDisplayState {
    data object EmptyQuery : SearchDisplayState
    data object StaleResults : SearchDisplayState
    data object NoResults : SearchDisplayState
    data class Success(
        val allItems: List<PageSearchResult>?,
        val focusedItem: PageSearchResult?
    ) : SearchDisplayState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuPDFDockedSearchBar(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    currentPage: Int,
    searchUiState: DocumentSearchUiState,
    muPDFSearch: MuPDFSearch,
    onQueryChange: (String) -> Unit,
    onStepSearch: suspend (Int) -> Boolean,
    onDocumentSearch: suspend () -> Boolean,
    onClearSearch: () -> Unit,
    onResultClick: (PageSearchResult) -> Unit
) {
    var notificationText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val currentSearchQuery = searchUiState.currentSearchQuery
    val textFieldState = rememberTextFieldState(initialText = currentSearchQuery)
    // 初始状态默认为收起，由 LaunchedEffect 或用户交互驱动
    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Collapsed)

    // Sync ViewModel query to textFieldState
    LaunchedEffect(currentSearchQuery) {
        if (textFieldState.text.toString() != currentSearchQuery) {
            textFieldState.setTextAndPlaceCursorAtEnd(currentSearchQuery)
        }
    }

    // Sync textFieldState to ViewModel and Reset Results on Change
    LaunchedEffect(textFieldState.text) {
        val newText = textFieldState.text.toString()
        onQueryChange(newText)
        // 如果输入词变了，且与当前搜索结果对应的词不一致，主动清理旧结果以防误导
        if (newText != muPDFSearch.txt) {
            muPDFSearch.clear()
        }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            modifier = Modifier.fillMaxWidth(),
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {
                scope.launch {
                    val res = onDocumentSearch()
                    if (!res) {
                        notificationText = "No result found."
                        snackbarHostState.showSnackbar(notificationText)
                    }
                }
            },
            placeholder = { Text("Search in document...") },
            leadingIcon = { LeadingContent(searchBarState, scope) },
            trailingIcon = if (textFieldState.text.isNotEmpty()) {
                {
                    TrailingContent(
                        searchUiState = searchUiState,
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        onClearSearch = onClearSearch,
                        onStepSearch = onStepSearch
                    )
                }
            } else null,
            colors = inputFieldColors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            )
        )
    }

    Box(modifier = modifier) {
        SearchBar(
            state = searchBarState,
            inputField = inputField,
            colors = SearchBarDefaults.colors(
                containerColor = Color.Transparent
            )
        )

        ExpandedDockedSearchBar(
            state = searchBarState,
            inputField = inputField,
            shape = RoundedCornerShape(16.dp),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            val focusedItem = muPDFSearch.focusedItem
            val allItems = muPDFSearch.allItems
            val currentInput = textFieldState.text.toString()

            val isQueryEmpty = currentInput.isBlank()
            val isResultsStale = currentInput != muPDFSearch.txt && currentInput.isNotBlank()
            val noResult = focusedItem == null && allItems.isNullOrEmpty()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                // 将数据封装进状态，确保 AnimatedContent 在做退出动画时能保留旧数据，避免 NPE
                val displayState = when {
                    isQueryEmpty -> SearchDisplayState.EmptyQuery
                    isResultsStale -> SearchDisplayState.StaleResults
                    noResult -> SearchDisplayState.NoResults
                    else -> SearchDisplayState.Success(allItems, focusedItem)
                }
                AnimatedContent(
                    targetState = displayState,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "SearchContentTransition"
                ) { state ->
                    when (state) {
                        SearchDisplayState.EmptyQuery -> {
                            SearchHintItem(
                                contentText = "Ready to search",
                                supportingContentText = "Type something to find it in the document."
                            )
                        }

                        SearchDisplayState.StaleResults -> {
                            SearchHintItem(
                                contentText = "Search not yet performed",
                                supportingContentText = "Tap the arrow icon on the right, or press the “Search” key on your keyboard."
                            )
                        }

                        SearchDisplayState.NoResults -> {
                            SearchHintItem(
                                contentText = "No results found",
                                supportingContentText = "Try a different key word."
                            )
                        }

                        is SearchDisplayState.Success -> {
                            Column {
                                if (state.allItems != null) {
                                    state.allItems.forEach { result ->
                                        SearchResultItem(
                                            currentPage = currentPage,
                                            result = result,
                                            onResultClick = {
                                                scope.launch { searchBarState.animateToCollapsed() }
                                                onResultClick(result)
                                            }
                                        )
                                    }
                                } else {
                                    // 这里使用 state.focusedItem 而不是外部的 focusedItem
                                    state.focusedItem?.let {
                                        SearchResultItem(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeadingContent(searchBarState: SearchBarState, scope: CoroutineScope) =
    if (searchBarState.currentValue == SearchBarValue.Expanded) {
        SearchBarActionIcon(
            imageVector = Icons.AutoMirrored.Default.ArrowBack,
            description = "Back"
        ) {
            scope.launch { searchBarState.animateToCollapsed() }
        }
    } else {
        Icon(Icons.Default.Search, contentDescription = null)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrailingContent(
    searchUiState: DocumentSearchUiState,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onClearSearch: () -> Unit,
    onStepSearch: suspend (Int) -> Boolean
) {
    var notificationText by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (searchUiState.isSearching) {
                CircularProgressIndicator(
                    progress = { searchUiState.searchProgress },
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    color = ProgressIndicatorDefaults.circularColor,
                    strokeWidth = 2.dp,
                    trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )
            } else {
                SearchBarActionIcon(Icons.Rounded.Close, "Clear", onClearSearch)
            }
        }
        SearchBarActionIcon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous") {
            scope.launch {
                val res = onStepSearch(-1)
                if (!res) {
                    notificationText = "No result found."
                    snackbarHostState.showSnackbar(notificationText)
                }
            }
        }
        SearchBarActionIcon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next") {
            scope.launch {
                val res = onStepSearch(1)
                if (!res) {
                    notificationText = "No result found."
                    snackbarHostState.showSnackbar(notificationText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarActionIcon(
    imageVector: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(
                modifier =
                    Modifier.semantics {
                        // TODO(b/496338253): Remove this modifier once bug where tooltip text is
                        //  not announced by a11y screen readers is resolved.
                        liveRegion = LiveRegionMode.Assertive
                        paneTitle = description
                    }
            ) {
                Text(description)
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = imageVector, contentDescription = description)
        }
    }
}

@Preview
@Composable
private fun SearchHintItem(
    contentText: String = "contentText",
    supportingContentText: String? = null,
) {
    ListItem(
        modifier = Modifier,
        leadingContent = {},
        supportingContent = { supportingContentText?.let { Text(supportingContentText) } },
        content = {
            Text(
                text = contentText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultItem(
    currentPage: Int,
    result: PageSearchResult,
    onResultClick: () -> Unit,
) {
    val hitCount = result.searchBoxes?.size ?: 0
    val isSelected = result.pageIndex == currentPage

    ListItem(
        selected = isSelected,
        onClick = onResultClick,
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        },
        leadingContent = {},
        supportingContent = { Text("$hitCount result(s) found. ") },
        shapes = ListItemDefaults.shapes(
            shape = RectangleShape,
            selectedShape = RoundedCornerShape(16.dp) // 选中状态：16dp 圆角
        ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        elevation = ListItemDefaults.elevation(),
        content = { Text("Page ${result.pageIndex + 1}") },
    )
}

@Composable
private fun SearchResultItem(
    result: PageSearchResult
) {
    val hitCount = result.searchBoxes?.size ?: 0

    ListItem(
        leadingContent = {},
        supportingContent = { Text("$hitCount result(s) found. ") },
        shapes = ListItemDefaults.shapes(shape = RectangleShape),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        elevation = ListItemDefaults.elevation(),
        content = { Text("Page ${result.pageIndex + 1}") },
    )
}

@Preview(showBackground = true)
@Composable
private fun MuPDFDockedSearchBarPreview() {
    var expanded by remember { mutableStateOf(true) }
    val searchUiState = remember {
        DocumentSearchUiState(
            currentSearchQuery = "Jetpack Compose",
            isSearching = false,
            searchProgress = 0.6f
        )
    }

    // 模拟搜索结果数据
    val dummyQuads = arrayOf(arrayOf(Quad(0f, 0f, 10f, 0f, 0f, 10f, 10f, 10f)))
    val muPDFSearch = remember {
        MuPDFSearch().apply {
            txt = "Jetpack Compose"
            allItems = listOf(
                PageSearchResult(pageIndex = 0, searchBoxes = dummyQuads),
                PageSearchResult(pageIndex = 4, searchBoxes = dummyQuads),
                PageSearchResult(pageIndex = 12, searchBoxes = dummyQuads)
            )
            focusedItem = allItems?.first()
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            MuPDFDockedSearchBar(
                modifier = Modifier.align(Alignment.TopCenter),
                snackbarHostState = remember { SnackbarHostState() },
                currentPage = 0,
                searchUiState = searchUiState,
                muPDFSearch = muPDFSearch,
                onQueryChange = {},
                onStepSearch = { true },
                onDocumentSearch = { true },
                onClearSearch = {},
                onResultClick = {}
            )
        }
    }
}