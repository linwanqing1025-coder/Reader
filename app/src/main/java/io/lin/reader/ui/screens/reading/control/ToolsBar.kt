package io.lin.reader.ui.maintab.reading.control

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.ui.maintab.reading.LocalReadingViewModel
import io.lin.reader.ui.maintab.reading.ReadingScreenViewModelForTest
import io.lin.reader.mupdf.search.MuPDFDockedSearchBar
import io.lin.reader.mupdf.search.MuPDFSearch
import io.lin.reader.ui.maintab.setting.details.ReflowDetails
import io.lin.reader.ui.theme.ReaderTheme

/**
 * 第二层面板类型
 */
enum class SecondaryPanel {
    Reflow,
    Search
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsBar(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {
    val viewModel = LocalReadingViewModel.current!!
    val uiState by viewModel.uiState.collectAsState()
    val currentPage = uiState.currentPage
    val currentRotation = (uiState.pageSettings[currentPage]?.rotation) ?: 0
    val currentSearchQuery = uiState.searchUiState.currentSearchQuery

    // 统一背景颜色
    val unifiedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)

    var expanded by rememberSaveable { mutableStateOf(false) }
    // 单变量控制：当前显示的第二层组件类型
    var activePanel by remember { mutableStateOf<SecondaryPanel?>(null) }
    // 记录最后一次非空的面板，用于在整个区域退出动画期间保持内容和高度稳定
    var displayPanel by remember { mutableStateOf(activePanel ?: SecondaryPanel.Search) }
    LaunchedEffect(activePanel) {
        if (activePanel != null) displayPanel = activePanel!!
    }

    // 监听 searchUiState 中的输入，若非空优先打开搜索栏
    LaunchedEffect(currentSearchQuery) {
        if (currentSearchQuery.isNotBlank()) {
            if (activePanel != SecondaryPanel.Search) {
                expanded = true
                activePanel = SecondaryPanel.Search
            }
        }
    }

    // 没见过大于 450dp 的【手机】设备
    BoxWithConstraints(modifier.widthIn(max = 450.dp)) {
        val itemWidth = (maxWidth - 16.dp) / 7
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // 第一层：图标行
            HorizontalFloatingToolbar(
                expanded = expanded,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = unifiedContainerColor,
                    toolbarContentColor = unifiedContainerColor
                ),
                leadingContent = {
                    // 黑夜模式
                    ToolIconButton(
                        modifier = Modifier.width(itemWidth),
                        imageVector = Icons.Rounded.DarkMode,
                        description = "Night Mode",
                        active = false,
                        expanded = false
                    ) {
                        //TODO
                    }
                    // 重排
                    ToolIconButton(
                        modifier = Modifier.width(itemWidth),
                        imageVector = Icons.AutoMirrored.Rounded.FormatAlignLeft,
                        description = "Reflow",
                        active = activePanel == SecondaryPanel.Reflow,
                    ) {
                        activePanel =
                            if (activePanel == SecondaryPanel.Reflow) null
                            else SecondaryPanel.Reflow
                    }
                    // 批注
                    ToolIconButton(
                        modifier = Modifier.width(itemWidth),
                        imageVector = Icons.Rounded.EditNote,
                        description = "Annotation",
                    ) {
                        // TODO
                    }
                    // 页旋转
                    val rotationAngle by animateFloatAsState(
                        targetValue = currentRotation.toFloat(),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "RotateIconAnimation"
                    )
                    ToolIconButton(
                        modifier = Modifier
                            .width(itemWidth)
                            .rotate(rotationAngle),
                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                        description = "Rotate Page",
                        active = currentRotation != 0,
                    ) {
                        viewModel.rotateCurrentPage(currentPage)
                    }

                    // 链接高亮
                    ToolIconButton(
                        modifier = Modifier.width(itemWidth),
                        imageVector = Icons.Rounded.Link,
                        description = "Highlight Links",
                        active = uiState.highlightLinks,
                        onClick = viewModel::toggleHighlightLinks
                    )


                    // 搜索
                    ToolIconButton(
                        modifier = Modifier.width(itemWidth),
                        imageVector = Icons.Rounded.Search,
                        description = "Search",
                        active = activePanel == SecondaryPanel.Search,
                        expanded = activePanel == SecondaryPanel.Search
                    ) {
                        if (activePanel == SecondaryPanel.Search) {
                            activePanel = null
                            viewModel.clearSearch()
                        } else {
                            activePanel = SecondaryPanel.Search
                        }
                    }
                },
                content = {
                    Box(
                        modifier = Modifier.width(itemWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier =
                                        Modifier.semantics {
                                            // TODO(b/496338253): Remove this modifier once bug
                                            //  where tooltip text is not announced by a11y screen
                                            //  readers is resolved.
                                            liveRegion = LiveRegionMode.Assertive
                                            paneTitle = "Localized description"
                                        }
                                ) {
                                    Text("Expand/Unflod ToolBar")
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            val defaultContainer = MaterialTheme.colorScheme.primary
                            val defaultContent = MaterialTheme.colorScheme.onPrimary
                            val animatedContainerColor by animateColorAsState(
                                targetValue = if (expanded) defaultContainer else Color.Transparent,
                                label = "FilledIconButtonContainerColor"
                            )
                            val animatedContentColor by animateColorAsState(
                                targetValue = if (expanded) defaultContent else defaultContainer,
                                label = "FilledIconButtonContentColor"
                            )
                            FilledIconButton(
                                modifier = Modifier,
                                onClick = { expanded = !expanded },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = animatedContainerColor,
                                    contentColor = animatedContentColor
                                )
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "Expand/Unflod ToolBar"
                                )
                            }
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // 第二层：动态面板区域
            AnimatedVisibility(
                visible = expanded && activePanel != null,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
            ) {
                // 使用稳定状态 displayPanel，确保退出动画时内容不会立即消失或塌陷
                AnimatedContent(
                    targetState = displayPanel,
                    transitionSpec = {
                        val initial = initialState
                        val target = targetState
                        // 根据枚举顺序计算方向：目标索引大则从右滑入，反之从左滑入
                        val direction = if (target.ordinal > initial.ordinal) 1 else -1
                        slideInHorizontally { it * direction } togetherWith
                                slideOutHorizontally { -it * direction }
                    },
                    label = "SecondaryPanelTransition",
                    modifier = Modifier.fillMaxWidth()
                ) { panel ->
                    when (panel) {
                        SecondaryPanel.Search -> {
                            DynamicPanelCard {
                                val muPDFSearch =
                                    viewModel.muPDFSearch.collectAsState().value ?: MuPDFSearch()
                                MuPDFDockedSearchBar(
                                    modifier = Modifier.fillMaxWidth(),
                                    snackbarHostState = snackbarHostState,
                                    currentPage = currentPage,
                                    searchUiState = uiState.searchUiState,
                                    muPDFSearch = muPDFSearch,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onStepSearch = {
                                        viewModel.executeStepSearch(
                                            it,
                                            currentPage
                                        )
                                    },
                                    onDocumentSearch = { viewModel.executeFullSearch() },
                                    onClearSearch = viewModel::clearSearch,
                                    onResultClick = {
                                        muPDFSearch.focusedItem = it
                                        viewModel.goToPage(it.pageIndex)
                                    },
                                )
                            }
                        }

                        SecondaryPanel.Reflow -> {
                            DynamicPanelCard {
                                val readerPreferences = viewModel.readerPreferences
                                val reflowPreferences = viewModel.reflowPreferences
                                ReflowDetails(
                                    readerPreferences = readerPreferences,
                                    reflowPreferences = reflowPreferences
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicPanelCard(
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)
){
    val unifiedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            contentColor = unifiedContainerColor,
            containerColor = unifiedContainerColor,
            disabledContentColor = unifiedContainerColor,
            disabledContainerColor = unifiedContainerColor
        )
    ){
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolIconButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    description: String,
    active: Boolean = false,
    expanded: Boolean = false,
    onClick: () -> Unit,
) {
    // 1. 背景色动画：expanded 为 true 时显示 secondaryContainer，否则透明
    val animatedContainerColor by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "ToolIconButtonContainerColor"
    )

    // 2. 图标颜色动画：active 为 true 时突显 primary 主色；仅 expanded 时匹配容器前景色；平时为常规图标色
    val animatedIconColor by animateColorAsState(
        targetValue = when {
            active -> MaterialTheme.colorScheme.primary
            expanded -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "ToolIconButtonIconColor"
    )

    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip(
                modifier =
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                        paneTitle = description
                    }
            ) {
                Text(description)
            }
        },
        state = rememberTooltipState(),
        modifier = modifier//.size(16.dp)
    ) {
        IconButton(
            onClick = onClick,
            // 绑定动画颜色值
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = animatedContainerColor,
                contentColor = animatedIconColor
            ),
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = description
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToolsBarPreview() {
    // 1. 创建虚假的 ViewModel 实现（使用你已经在 ReadingScreenViewModel.kt 中定义好的 ForTest 类）
    val fakeViewModel = remember { ReadingScreenViewModelForTest() }
    val snackbarHostState = remember { SnackbarHostState() }

    ReaderTheme {
        // 2. 通过 CompositionLocalProvider 注入虚假环境
        CompositionLocalProvider(LocalReadingViewModel provides fakeViewModel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 3. 渲染 ToolsBar
                ToolsBar(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 72.dp, start = 8.dp, end = 8.dp),
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}