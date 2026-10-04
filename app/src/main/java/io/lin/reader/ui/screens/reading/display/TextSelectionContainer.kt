package io.lin.reader.ui.screens.reading.display

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.magnifier
import io.lin.reader.mupdf.selection.SelectionWalker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Quad
import io.lin.reader.ui.screens.reading.LocalReadingViewModel
import io.lin.reader.mupdf.core.MuPDFCoreExtended
import io.lin.reader.mupdf.selection.ToStructuredTextOptions.PARAGRAPH_BREAK
import io.lin.reader.mupdf.selection.ToStructuredTextOptions.SEGMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "TextSelectionContainer.kt"

data class PageRegistration(
    val coordinates: LayoutCoordinates,
    val pxToPtScale: Float
)

class TextSelectionState {
    private var isSelecting = false

    // 上报的数据：页面坐标和 PDF 尺寸
    val registeredPages = mutableMapOf<Int, PageRegistration>()

    // 下发的数据：选区高亮 (引发重组)
    val selectionQuads = mutableStateMapOf<Int, Array<Quad>>()

    // 当前选区的逻辑起点和终点
    var startPageIndex by mutableIntStateOf(-1)
    var startPointPt by mutableStateOf<Point?>(null)
    var endPageIndex by mutableIntStateOf(-1)
    var endPointPt by mutableStateOf<Point?>(null)

    // 用于定位左右手柄的坐标点
    var startHandlePos by mutableStateOf<Offset?>(null)
    var endHandlePos by mutableStateOf<Offset?>(null)

    // 用于菜单位置
    var menuPosition by mutableStateOf<Offset?>(null)

    // 用于放大镜位置 (如果为 null，则隐藏放大镜)
    var magnifierPosition by mutableStateOf<Offset?>(null)

    var containerCoords: LayoutCoordinates? = null


    fun registerPage(pageIndex: Int, coords: LayoutCoordinates, pxToPtScale: Float) {
        registeredPages[pageIndex] = PageRegistration(coords, pxToPtScale)
    }

    fun unregisterPage(pageIndex: Int) {
        registeredPages.remove(pageIndex)
    }

    fun startSelection() {
        isSelecting = true
    }

    fun endSelection() {
        isSelecting = false
    }

    fun isSelecting(): Boolean = isSelecting

    /**
     * 清空当前的选文高亮区域。
     */
    fun clearSelection() {
        selectionQuads.clear()
        startHandlePos = null
        endHandlePos = null
        menuPosition = null
        startPageIndex = -1
        startPointPt = null
        endPageIndex = -1
        endPointPt = null
    }

    /**
     * 将当前跨页选中的文本内容拼接为一个完整的字符串返回。
     * @param core MuPDFCoreExtended 实例，用于提取结构化文本
     * @return 选中的完整字符串；如果没有选中任何内容，则返回空字符串。
     */
    suspend fun getSelectedText(core: MuPDFCoreExtended): String {
        if (selectionQuads.isEmpty() || startPageIndex == -1 || endPageIndex == -1 || startPointPt == null || endPointPt == null) {
            return ""
        }

        val minPage = minOf(startPageIndex, endPageIndex)
        val maxPage = maxOf(startPageIndex, endPageIndex)

        val stringBuilder = StringBuilder()

        withContext(Dispatchers.IO) {
            for (p in minPage..maxPage) {
                // 使用底层分段和段落打断标记，依靠 SelectionWalker 自行提取高亮文本
                val options = "${SEGMENT},${PARAGRAPH_BREAK}"
                val sText = core.getStructuredText(p, options)

                if (sText != null) {
                    val quads = selectionQuads[p]
                    if (!quads.isNullOrEmpty()) {
                        val walker = SelectionWalker(quads)
                        sText.walk(walker)
                        stringBuilder.append(walker.resultBuilder.toString())
                    }
                    sText.destroy()
                }
            }
        }

        return stringBuilder.toString().trim()
    }

    /**
     * 根据容器坐标系内的偏移量（Offset），查找该手指落在哪一页上。
     * @param containerCoords 外部容器的 LayoutCoordinates
     * @param offset 手指在容器上的相对坐标
     * @return 返回匹配的页面索引 (pageIndex) 以及对应在 PDF 上的真实 pt 坐标，找不到则返回 null
     */
    fun findPageAndPoint(containerCoords: LayoutCoordinates, offset: Offset): Pair<Int, Point>? {
        for ((pageIndex, reg) in registeredPages) {
            val pageCoords = reg.coordinates
            val pxToPtScale = reg.pxToPtScale
            val localPos = pageCoords.localPositionOf(containerCoords, offset)

            // 检查坐标是否落入该页面的边界内
            if (localPos.x >= 0 && localPos.x <= pageCoords.size.width &&
                localPos.y >= 0 && localPos.y <= pageCoords.size.height
            ) {
                val pt = Point(localPos.x * pxToPtScale, localPos.y * pxToPtScale)
                return Pair(pageIndex, pt)
            }
        }
        return null
    }

    /**
     * 跨页或单页高亮查询逻辑。
     * 这是一个耗时操作，应在后台协程中调用。
     * 查询完成后会自动切回主线程更新 selectionQuads 以触发重组。
     */
    suspend fun querySelectionQuads(
        core: MuPDFCoreExtended,
        startPageIndex: Int,
        startPointPt: Point,
        endPageIndex: Int,
        endPointPt: Point
    ) {
        val newQuads = mutableMapOf<Int, Array<Quad>>()

        val minPage = minOf(startPageIndex, endPageIndex)
        val maxPage = maxOf(startPageIndex, endPageIndex)

        val actualStartPt = if (startPageIndex <= endPageIndex) startPointPt else endPointPt
        val actualEndPt = if (startPageIndex <= endPageIndex) endPointPt else startPointPt

        // 此处处于 Dispatchers.IO 线程
        for (p in minPage..maxPage) {
            val sText = core.getStructuredText(p)
            if (sText != null) {
                val quads = when {
                    // 单页选文
                    minPage == maxPage -> sText.highlight(actualStartPt, actualEndPt)

                    // 跨页：处于起始页，从起点选到页尾
                    p == minPage -> sText.highlight(actualStartPt, Point(10000f, 10000f))

                    // 跨页：处于结束页，从页首选到终点
                    p == maxPage -> sText.highlight(Point(-10000f, -10000f), actualEndPt)

                    // 跨页：中间页，全选
                    else -> sText.highlight(Point(-10000f, -10000f), Point(10000f, 10000f))
                }
                if (quads != null) {
                    newQuads[p] = quads
                }
                sText.destroy()
            }
        }

        // 切回主线程更新 StateMap，触发对应页面的 Recomposition
        withContext(Dispatchers.Main) {
            selectionQuads.clear()
            selectionQuads.putAll(newQuads)
        }
    }

    fun updateHandleAndMenuPositions(coords: LayoutCoordinates? = this.containerCoords) {
        if (coords == null) return
        this.containerCoords = coords
        if (selectionQuads.isEmpty()) {
            startHandlePos = null
            endHandlePos = null
            menuPosition = null
            return
        }

        val sortedPages = selectionQuads.keys.sorted()
        val firstPage = sortedPages.first()
        val lastPage = sortedPages.last()

        val firstQuad = selectionQuads[firstPage]?.firstOrNull()
        val lastQuad = selectionQuads[lastPage]?.lastOrNull()

        val firstReg = registeredPages[firstPage]
        val lastReg = registeredPages[lastPage]

        if (firstQuad != null && firstReg != null && firstReg.coordinates.isAttached) {
            val pxX = firstQuad.ll_x / firstReg.pxToPtScale
            val pxY = firstQuad.ll_y / firstReg.pxToPtScale
            val localOffset = Offset(pxX, pxY)
            // 将页面的本地坐标转换为 TextSelectionContainer 的容器坐标
            startHandlePos = coords.localPositionOf(firstReg.coordinates, localOffset)
        }

        if (lastQuad != null && lastReg != null && lastReg.coordinates.isAttached) {
            val pxX = lastQuad.lr_x / lastReg.pxToPtScale
            val pxY = lastQuad.lr_y / lastReg.pxToPtScale
            val localOffset = Offset(pxX, pxY)
            endHandlePos = coords.localPositionOf(lastReg.coordinates, localOffset)
        }

        // 菜单显示在首个手柄上方
        menuPosition = startHandlePos
    }
}

val LocalTextSelectionState = staticCompositionLocalOf<TextSelectionState> {
    error("No TextSelectionState provided")
}

/**
 * 文本选择全局手势拦截容器。
 */
@Composable
fun TextSelectionContainer(
    modifier: Modifier = Modifier,
    selectionEnabled: Boolean = true,
    hideMenu: Boolean = false,
    content: @Composable () -> Unit
) {
    val selectionState = remember { TextSelectionState() }

    TextSelectionContainerImpl(
        selectionState = selectionState,
        modifier = modifier,
        selectionEnabled = selectionEnabled,
        hideMenu = hideMenu,
        content = content
    )
}

@Composable
fun TextSelectionContainer(
    selectionState: TextSelectionState,
    modifier: Modifier = Modifier,
    selectionEnabled: Boolean = true,
    hideMenu: Boolean = false,
    content: @Composable () -> Unit = {}
) = TextSelectionContainerImpl(
    selectionState = selectionState,
    modifier = modifier,
    selectionEnabled = selectionEnabled,
    hideMenu = hideMenu,
    content = content
)

@Composable
private fun TextSelectionContainerImpl(
    selectionState: TextSelectionState,
    modifier: Modifier = Modifier,
    selectionEnabled: Boolean = true,
    hideMenu: Boolean = false,
    content: @Composable () -> Unit
) {
    val viewModel = LocalReadingViewModel.current!!
    val core = viewModel.mCore.collectAsState().value ?: return

    val scope = rememberCoroutineScope()

    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val density = LocalDensity.current
    // 放大镜通常悬浮在手指上方 60dp
    val magnifierVerticalOffset = with(density) { -60.dp.toPx() }

    // DpSize 用于设置放大镜本身的物理尺寸（胶囊形状通常比较宽）
    val magnifierSize = DpSize(96.dp, 48.dp)

    Box(
        modifier = modifier
            .onGloballyPositioned {
                containerCoords = it
                selectionState.updateHandleAndMenuPositions(it)
            }
            .magnifier(
                sourceCenter = {
                    selectionState.magnifierPosition ?: Offset.Unspecified
                },
                magnifierCenter = {
                    val source = selectionState.magnifierPosition
                    if (source != null) {
                        Offset(source.x, source.y + magnifierVerticalOffset)
                    } else {
                        Offset.Unspecified
                    }
                },
                zoom = 1.5f,
                size = magnifierSize,
                cornerRadius = magnifierSize.height / 2 // 让高度的一半作为圆角，形成完美的胶囊状
            )
            .then(
                if (selectionEnabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    selectionState.startSelection()

                                    if (containerCoords == null) return@detectDragGesturesAfterLongPress

                                    // 清除旧的高亮
                                    selectionState.clearSelection()

                                    // 寻找手指落在哪一页
                                    val hitResult =
                                        selectionState.findPageAndPoint(containerCoords!!, offset)
                                    if (hitResult != null) {
                                        selectionState.startPageIndex = hitResult.first
                                        selectionState.startPointPt = hitResult.second
                                        selectionState.endPageIndex = hitResult.first
                                        selectionState.endPointPt = hitResult.second
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    // 实时更新放大镜的焦点位置为手指当前位置
                                    selectionState.magnifierPosition = change.position

                                    if (containerCoords == null || selectionState.startPageIndex == -1 || selectionState.startPointPt == null) return@detectDragGesturesAfterLongPress

                                    // 寻找手指当前滑到了哪一页
                                    val hitResult = selectionState.findPageAndPoint(
                                        containerCoords!!,
                                        change.position
                                    )

                                    if (hitResult != null) {
                                        selectionState.endPageIndex = hitResult.first
                                        selectionState.endPointPt = hitResult.second

                                        // 跨页/单页查询逻辑封装到 State 中
                                        scope.launch(Dispatchers.IO) {
                                            selectionState.querySelectionQuads(
                                                core = core,
                                                startPageIndex = selectionState.startPageIndex,
                                                startPointPt = selectionState.startPointPt!!,
                                                endPageIndex = selectionState.endPageIndex,
                                                endPointPt = selectionState.endPointPt!!
                                            )
                                            withContext(Dispatchers.Main) {
                                                if (containerCoords != null) {
                                                    selectionState.updateHandleAndMenuPositions(
                                                        containerCoords!!
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    selectionState.endSelection()
                                    selectionState.magnifierPosition = null
                                },
                                onDragCancel = {
                                    selectionState.clearSelection()
                                    selectionState.endSelection()
                                    selectionState.magnifierPosition = null
                                }
                            )
                        }
                } else Modifier
            )
            .then(
                if (selectionState.selectionQuads.isNotEmpty()) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { selectionState.clearSelection() }
                        )
                    }
                } else Modifier
            )
    ) {
        CompositionLocalProvider(LocalTextSelectionState provides selectionState) {
            content()
        }

        if (selectionState.selectionQuads.isNotEmpty()) {
            // --- 渲染手柄和菜单 ---
            val showMenu = !selectionState.isSelecting() && !hideMenu
            SelectionMenu(
                visible = showMenu,
                windowPosition = selectionState.menuPosition,
                surfaceOffset = Offset.Zero, // 因为是在当前 Box 内，不需要额外补偿
                viewWidth = containerCoords?.size?.width?.toFloat() ?: 0f,
                onCopy = {
                    scope.launch {
                        val text = selectionState.getSelectedText(core)
                        Log.d(TAG, "Selected Text: $text")
                        // TODO: 将提取的文本 text 复制到系统剪贴板
                        selectionState.clearSelection()
                    }
                },
                onSelectAll = { /* TODO */ },
                onSearch = { /* TODO */ }
            )

            // 手柄
            val density = LocalDensity.current

            // 开始手柄 (左侧)
            selectionState.startHandlePos?.let { pos ->
                // 第一个 Quad 的高度
                val firstPage = selectionState.selectionQuads.keys.minOrNull()
                val firstQuad =
                    firstPage?.let { selectionState.selectionQuads[it]?.firstOrNull() }
                val firstReg = firstPage?.let { selectionState.registeredPages[it] }
                val firstLineHeight = if (firstQuad != null && firstReg != null) {
                    with(density) {
                        ((abs(firstQuad.ll_y - firstQuad.ul_y)) / firstReg.pxToPtScale).toDp()
                    }
                } else null
                if (firstLineHeight == null) return@let
                SelectionHandle(
                    selectionState = selectionState,
                    isStart = true,
                    anchor = pos,
                    lineHeight = firstLineHeight,
                    onDragStart = {
                        selectionState.startSelection()
                    },
                    onDrag = { tipPos ->
                        val hitResult =
                            selectionState.findPageAndPoint(containerCoords!!, tipPos)
                        if (hitResult != null) {
                            selectionState.startPageIndex = hitResult.first
                            selectionState.startPointPt = hitResult.second

                            scope.launch(Dispatchers.IO) {
                                selectionState.querySelectionQuads(
                                    core = core,
                                    startPageIndex = selectionState.startPageIndex,
                                    startPointPt = selectionState.startPointPt!!,
                                    endPageIndex = selectionState.endPageIndex,
                                    endPointPt = selectionState.endPointPt!!
                                )
                                withContext(Dispatchers.Main) {
                                    if (containerCoords != null) {
                                        selectionState.updateHandleAndMenuPositions(
                                            containerCoords!!
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        selectionState.endSelection()
                        selectionState.magnifierPosition = null
                    }
                )
            }

            // 结束手柄 (右侧)
            selectionState.endHandlePos?.let { pos ->
                // 最后一个 Quad 的高度
                val lastPage = selectionState.selectionQuads.keys.maxOrNull()
                val lastQuad = lastPage?.let { selectionState.selectionQuads[it]?.lastOrNull() }
                val lastReg = lastPage?.let { selectionState.registeredPages[it] }
                val lastLineHeight = if (lastQuad != null && lastReg != null) {
                    with(density) {
                        ((abs(lastQuad.ll_y - lastQuad.ul_y)) / lastReg.pxToPtScale).toDp()
                    }
                } else null
                if (lastLineHeight == null) return@let
                SelectionHandle(
                    selectionState = selectionState,
                    isStart = false,
                    anchor = pos,
                    lineHeight = lastLineHeight,
                    onDragStart = {
                        selectionState.startSelection()
                    },
                    onDrag = { tipPos ->
                        val hitResult =
                            selectionState.findPageAndPoint(containerCoords!!, tipPos)
                        if (hitResult != null) {
                            selectionState.endPageIndex = hitResult.first
                            selectionState.endPointPt = hitResult.second

                            scope.launch(Dispatchers.IO) {
                                selectionState.querySelectionQuads(
                                    core = core,
                                    startPageIndex = selectionState.startPageIndex,
                                    startPointPt = selectionState.startPointPt!!,
                                    endPageIndex = selectionState.endPageIndex,
                                    endPointPt = selectionState.endPointPt!!
                                )
                                withContext(Dispatchers.Main) {
                                    if (containerCoords != null) {
                                        selectionState.updateHandleAndMenuPositions(
                                            containerCoords!!
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        selectionState.endSelection()
                    }
                )
            }
        }
    }
}

/**
 * 选文手柄，放 Chrome 风格
 */
@Composable
private fun SelectionHandle(
    selectionState: TextSelectionState,
    isStart: Boolean,
    anchor: Offset,
    lineHeight: Dp,
    rotation: Int = 0,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit = {}
) {
    val handleColor = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current
    val size = lineHeight.coerceIn(16.dp, 48.dp)

    val anchorCache by rememberUpdatedState(anchor)
    val onDragStartCache by rememberUpdatedState(onDragStart)
    val onDragCache by rememberUpdatedState(onDrag)
    val onDragEndCache by rememberUpdatedState(onDragEnd)

    val shape = remember(isStart) {
        if (isStart) {
            RoundedCornerShape(
                topStartPercent = 50,
                topEndPercent = 0,
                bottomStartPercent = 50,
                bottomEndPercent = 50
            )
        } else {
            RoundedCornerShape(
                topStartPercent = 0,
                topEndPercent = 50,
                bottomStartPercent = 50,
                bottomEndPercent = 50
            )
        }
    }

    Box(
        modifier = Modifier
            .offset {
                val sizePx = with(density) { size.toPx() }
                if (isStart) {
                    IntOffset((anchor.x - sizePx).roundToInt(), anchor.y.roundToInt())
                } else {
                    IntOffset(anchor.x.roundToInt(), anchor.y.roundToInt())
                }
            }
            .size(size)
            .graphicsLayer {
                rotationZ = rotation.toFloat()
                transformOrigin = if (isStart) TransformOrigin(1f, 0f) else TransformOrigin(0f, 0f)
            }
            .background(handleColor, shape)
            .pointerInput(Unit) {
                var currentTipPos = Offset.Zero

                detectDragGestures(
                    onDragStart = {
                        currentTipPos = anchorCache
                        onDragStartCache()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentTipPos += dragAmount

                        // 【更新放大镜位置】：将其更新为手指当前所在的绝对屏幕坐标
                        // 由于手柄自身带有 offset 偏移和 padding 尺寸，手柄内部的 change.position
                        // 相对的是这个手柄自身的小方块。要获得对于整个 TextSelectionContainer 的绝对坐标，
                        // 最简单的办法就是直接使用外界传递进来的 currentTipPos （即物理尖端坐标）。
                        // 这样放大镜就会精确放大文字本身，而不是放大用户的胖手指。
                        selectionState.magnifierPosition = currentTipPos

                        onDragCache(currentTipPos)
                    },
                    onDragEnd = {
                        selectionState.magnifierPosition = null
                        onDragEndCache()
                    },
                    onDragCancel = {
                        selectionState.magnifierPosition = null
                        onDragEndCache()
                    }
                )
            }
    )
}

/**
 * 选文菜单组件，仿 Google Chrome 风格
 */
@Composable
private fun SelectionMenu(
    visible: Boolean,
    windowPosition: Offset?,
    surfaceOffset: Offset,
    viewWidth: Float,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onSearch: () -> Unit
) {
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // 记录上一次有效位置，避免在消失动画过程中因位置重置为 null 而闪现到左上角
    var lastValidPosition by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(windowPosition) {
        if (windowPosition != null) {
            lastValidPosition = windowPosition
        }
    }

    val currentPos = windowPosition ?: lastValidPosition

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.offset {
            val localX = currentPos.x - surfaceOffset.x
            val localY = currentPos.y - surfaceOffset.y
            val finalX = (localX - menuSize.width / 2)
                .coerceIn(16.dp.toPx(), viewWidth - menuSize.width - 16.dp.toPx())
            val finalY = (localY - menuSize.height - (16 * density.density))
                .coerceAtLeast(16.dp.toPx())
            IntOffset(finalX.roundToInt(), finalY.roundToInt())
        }
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            modifier = Modifier.onSizeChanged { menuSize = it }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionMenuItem(text = "复制", onClick = onCopy)
                SelectionMenuItem(text = "全选", onClick = onSelectAll)
                SelectionMenuItem(text = "搜索", onClick = onSearch)
            }
        }
    }
}

@Composable
private fun SelectionMenuItem(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}