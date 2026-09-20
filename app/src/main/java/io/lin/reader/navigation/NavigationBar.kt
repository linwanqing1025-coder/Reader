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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
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
    onNavigate: (NavKey) -> Boolean,
) {
    // 存储 navigationItemsList 的导航项位置
    val itemPositions = remember { mutableStateMapOf<NavKey, Rect>() }

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

    // The toolbar should receive focus before the screen content, so place it first.
    // Make sure to set its zIndex so it's above the screen content visually.
    HorizontalFloatingToolbar(
        modifier = modifier
            .height(64.dp)
            .offset(y = -ScreenOffset),
        expanded = true,
        content = {
            FancyRowIndicator(
                targetRect = itemPositions[currentKey] ?: Rect.Zero,
            )
            navigationItemsList.forEachIndexed { index, item ->
                val itemDescription = stringResource(item.contentDescription)
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .onGloballyPositioned { coords ->
                            val rect = Rect(
                                coords.positionInParent(),
                                coords.size.toSize()
                            )
                            itemPositions[item.key] = rect
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
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(item.icon, contentDescription = itemDescription)
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun FancyRowIndicator(
    targetRect: Rect,
    color: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val startAnim = remember { Animatable(targetRect.left) }
    val endAnim = remember { Animatable(targetRect.right) }

    LaunchedEffect(targetRect.left, targetRect.right) {
        val newStart = targetRect.left
        val newEnd = targetRect.right

        if (endAnim.targetValue != newEnd) {
            launch {
                endAnim.animateTo(
                    newEnd,
                    animationSpec = spring(
                        dampingRatio = 1f,
                        stiffness = if (endAnim.targetValue < newEnd) 1000f else 50f
                    )
                )
            }
        }
        if (startAnim.targetValue != newStart) {
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
                    onNavigate = onNavigate
                )

                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        val current = backStack.lastOrNull()
                        Text(
                            text = current?.toString() ?: "Empty",
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