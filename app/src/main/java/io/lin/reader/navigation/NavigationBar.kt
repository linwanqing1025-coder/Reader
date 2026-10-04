@file:OptIn(ExperimentalMaterial3Api::class)

package io.lin.reader.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Composable
fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    scrollBehavior: FloatingToolbarScrollBehavior,
    isRootPage: Boolean,
    currentKey: NavKey?,
    navigationItemsList: List<NavigationItems>,
    navigationLabel: Boolean = true,
    onNavigate: (NavKey) -> Boolean,
) {
    // 存储 navigationItemsList 的导航项位置
    val itemPositions = remember { mutableStateMapOf<NavKey, Rect>() }

    // 锁定一个稳定的目标区域，防止切换瞬间 Rect.Zero 导致动画中断
    var activeRect by remember { mutableStateOf(Rect.Zero) }
    val currentTargetRect = itemPositions[currentKey]
    if (currentTargetRect != null && currentTargetRect != Rect.Zero) {
        activeRect = currentTargetRect
    }

    // 处理退场动画：如果不在根页面，则强制触发 scrollBehavior 的隐藏位移
    LaunchedEffect(isRootPage) {
        if (!isRootPage) {
            animate(
                initialValue = scrollBehavior.state.offset,
                targetValue = scrollBehavior.state.offsetLimit
            ) { value, _ ->
                scrollBehavior.state.offset = value
            }
        } else {
            animate(
                initialValue = scrollBehavior.state.offset,
                targetValue = 0f
            ) { value, _ ->
                scrollBehavior.state.offset = value
            }
        }
    }

    HorizontalFloatingToolbar(
        modifier = modifier
            .height(
                if (navigationLabel) 84.dp
                else 64.dp
            )
            .offset(y = -ScreenOffset),
        expanded = true,
        content = {
            // 记住 Row 的坐标系
            var rowCoordinates by remember {
                mutableStateOf<LayoutCoordinates?>(
                    null
                )
            }
            Row(
                modifier = Modifier
                    .padding(
                        horizontal =
                            if (navigationLabel) 6.dp
                            else 0.dp
                    )
                    .onGloballyPositioned { rowCoordinates = it },
                horizontalArrangement =
                    if (navigationLabel) Arrangement.spacedBy(2.dp)
                    else Arrangement.Center
            ) {
                if (activeRect != Rect.Zero) {
                    FancyNavBarIndicator(
                        navBarActive = isRootPage,
                        targetRect = activeRect
                    )
                }
                navigationItemsList.forEach { item ->
                    val itemDescription = stringResource(item.contentDescription)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(58.dp)
                                .onGloballyPositioned { boxCoords ->
                                    // 让 Row 直接计算出 Box 相对于 Row 的 Rect
                                    rowCoordinates?.let { rowCoords ->
                                        if (rowCoords.isAttached && boxCoords.isAttached) {
                                            itemPositions[item.key] =
                                                rowCoords.localBoundingBoxOf(boxCoords)
                                        }
                                    }
                                },
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
                                                liveRegion = LiveRegionMode.Assertive
                                                paneTitle = itemDescription
                                            }
                                    ) {
                                        Text(itemDescription)
                                    }
                                },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(
                                    onClick = { onNavigate(item.key) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(item.icon, contentDescription = itemDescription)
                                }
                            }
                        }
                        if (navigationLabel) {
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = stringResource(item.label),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun FancyNavBarIndicator(
    navBarActive: Boolean,
    targetRect: Rect,
    color: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val startAnim = remember { Animatable(targetRect.left) }
    val endAnim = remember { Animatable(targetRect.right) }

    // 记录上一帧的
    // 显示状态，用于判断是否是“刚从隐藏状态变为显示”
    var wasRootPage by remember { mutableStateOf(navBarActive) }

    LaunchedEffect(targetRect.left, targetRect.right) {
        val newStart = targetRect.left
        val newEnd = targetRect.right

        // 如果之前处于隐藏状态，或者当前正在隐藏状态中，直接定位，不要动画
        if (!wasRootPage || !navBarActive) {
            startAnim.snapTo(newStart)
            endAnim.snapTo(newEnd)
        } else {
            launch {
                endAnim.animateTo(
                    newEnd,
                    animationSpec = spring(
                        dampingRatio = 1f,
                        stiffness = if (endAnim.targetValue < newEnd) 1000f else 50f
                    )
                )
            }
            launch {
                startAnim.animateTo(
                    newStart,
                    animationSpec = spring(
                        dampingRatio = 1f,
                        stiffness = if (startAnim.targetValue < newStart) 50f else 1000f
                    )
                )
            }
        }
        // 更新历史状态
        wasRootPage = navBarActive
    }

    val indicatorStart = startAnim.value
    val indicatorEnd = endAnim.value

    Box(
        Modifier
            .layout { measurable, _ ->
                val width = (indicatorEnd - indicatorStart).roundToInt()
                val height = targetRect.height.roundToInt()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(0, 0) {
                    placeable.place(indicatorStart.roundToInt(), 0)
                }
            }
            .background(color, CircleShape)
    )
}


@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview
@Composable
private fun FloatingNavigationBarPreview() {
    val navigationBarScrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
    val navKeyListSaver = Saver<MutableList<NavKey>, String>(
        save = { Json.encodeToString(it.toList()) },
        restore = { Json.decodeFromString<List<NavKey>>(it).toMutableStateList() }
    )
    val backStack = rememberSaveable(saver = navKeyListSaver) {
        mutableStateListOf(NavKey.Shelf)
    }
    val backToTab = {
        backStack.clear()
        backStack.add(NavKey.Shelf)
    }
    val onNavigate: (NavKey) -> Boolean = { newKey: NavKey ->
        if (newKey is NavKey.Root) {
            // 切换顶级 Tab 时，清空并重新开始（标准 Bottom Nav 行为）
            backStack.clear()
            backStack.add(newKey)
        } else {
            backStack.add(newKey)
        }
    }
    val currentKey = backStack.lastOrNull()
    val isRootPage = NavigationItems.entries.any {
        it.key == currentKey
    }

    var navigationLabel by remember { mutableStateOf(true) }
    val toggleNavigationLabel = { navigationLabel = !navigationLabel }

    val scrollModifier =
        if (isRootPage) Modifier.nestedScroll(navigationBarScrollBehavior)
        else Modifier

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(scrollModifier),
        content = { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                FloatingNavigationBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
                    scrollBehavior = navigationBarScrollBehavior,
                    isRootPage = isRootPage,
                    currentKey = currentKey,
                    navigationItemsList = NavigationItems.entries,
                    navigationLabel = navigationLabel,
                    onNavigate = onNavigate
                )

                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = currentKey?.toString() ?: "Empty",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(top = 200.dp)
                                .fillMaxWidth()
                        )
                        Button(
                            onClick = { onNavigate(NavKey.Sort) }
                        ) {
                            Text(text = "Navigate to detail screen")
                        }
                        Button(onClick = { backToTab() }) {
                            Text(text = "Navigate back to tab screen")
                        }
                        Button(onClick = toggleNavigationLabel) {
                            Text(text = "Toggle Showing Navigation Label")
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1000.dp)
                        )
                    }
                }
            }
        },
    )
}