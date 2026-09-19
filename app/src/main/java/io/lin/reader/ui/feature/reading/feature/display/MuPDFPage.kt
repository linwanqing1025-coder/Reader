package io.lin.reader.ui.feature.reading.feature.display

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Link
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.viewer.R
import androidx.core.graphics.createBitmap
import com.artifex.mupdf.fitz.Point
import io.lin.reader.ui.feature.reading.LocalReadingTransform
import io.lin.reader.ui.feature.reading.LocalReadingViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight

private const val APP = "MuPDF"

data class SelectionArea(
    val start: Point,
    val end: Point
)

data class PageState(
    // mEntireBm
    val mEntireBm: Bitmap? = null,
    // mIsBlank
    val mIsLoading: Boolean = true,
    val mError: String? = null,
    // mLinks
    val mLinks: Array<Link> = emptyArray(),
    // mSearchBoxes
    val mSearchBoxes: Array<Array<Quad>>? = null,
    // new feature: text select
    val mSelectionQuads: Array<Quad>? = null,
    val mSelectedText: String? = null,
    val mSelectedArea: SelectionArea? = null,
)

data class PatchState(
    // mPatchBm
    val mPatchBm: Bitmap? = null,
    // patchViewSize: 缩放后整页的大小（单位为屏幕像素值）
    val patchViewSize: IntSize = IntSize.Zero,
    // patchArea: 缩放后瓦片层的位置（相对于整页，单位为屏幕像素值）
    val patchArea: Rect = Rect.Zero,
    val scale: Float = 1f
)


/**
 * 用 Kotlin 和 Compose 重写的 PageView.java
 */
@OptIn(FlowPreview::class)
@Composable
fun MuPDFPage(
    modifier: Modifier,
    // mPageNumber, getPage()
    mPageNumber: Int,
    pdfSize: PointF,
    // mHighlightLinks, setLinkHighlighting()
    mHighlightLinks: Boolean = true,
) {
    // mContext
    val mContext = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Viewmodel
    val viewModel = LocalReadingViewModel.current!!

    // mCore
    val mCore = viewModel.mCore.collectAsState().value!!

    // SearchTask, SearchResult
    val searcher = viewModel.muPDFSearch.collectAsState().value!!

    // mSearchBoxes
    val focusedHit = searcher.focusedItem
    val allHits = searcher.allItems
    val mSearchBoxes = if (focusedHit != null && focusedHit.pageIndex == mPageNumber) {
        // 先在单页结果中找
        focusedHit.searchBoxes
    } else allHits?.find { it.pageIndex == mPageNumber }?.searchBoxes

    // mScale
    val transform = LocalReadingTransform.current

    // 当前单页状态
    var pageState by remember { mutableStateOf(PageState()) }
    // 记录当前的单页与屏幕交集
    var patchAreaCache by remember { mutableStateOf(Rect.Zero) }
    // 请求渲染的瓦片状态
    var patchState by remember { mutableStateOf(PatchState()) }

    // 更新文本选择
    val updateSelection: (Point?, Point?) -> Unit = { p1, p2 ->
        if (p1 != null && p2 != null) {
            scope.launch(Dispatchers.IO) {
                val sText = mCore.getStructuredText(mPageNumber)
                val quads = sText?.highlight(p1, p2)
                val text = sText?.copy(p1, p2)
                withContext(Dispatchers.Main) {
                    pageState = pageState.copy(
                        mSelectionQuads = quads,
                        mSelectedText = text,
                        mSelectedArea = SelectionArea(p1, p2)
                    )
                }
                sText?.destroy()
            }
        }
    }

    // Color
    val backgroundColor =
        if (mCore.isReflowable) Color(viewModel.reflowPreferences.backgroundColor)
        else Color(viewModel.readerPreferences.readerColor.page)
    val highlightColor = Color(0x80CC6600)
    val linkColor = Color(0x800066CC)

    // [New] 渲染方式：背景透明（默认）/填充白色底色
    val drawPage =
        if (true) mCore::drawPageTransparent // TODO: 新增开关控制
        else mCore::drawPage

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(pdfSize.x / pdfSize.y)
            .background(backgroundColor)
            .onGloballyPositioned { coordinates -> // calculate patchArea
                // 源码：
                // Rect viewArea = new Rect(getLeft(),getTop(),getRight(),getBottom());
                // final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
                // final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);
                // if (!patchArea.intersect(viewArea)) return;
                // 这里由于原文件的 View 的各边界值会相应缩放，而 BoxWithConstraints 的 constraints 不会随缩放变化，因此这里逻辑不同

                // 获取屏幕窗口坐标系下的可见区域矩形
                val patchRectInWindow = coordinates.boundsInWindow(clipBounds = true)

                if (!patchRectInWindow.isEmpty) {
                    // 将屏幕坐标系下矩形的 4 个顶点，通过矩阵逆变换映射回组件本地坐标系
                    val p1 = coordinates.windowToLocal(patchRectInWindow.topLeft)
                    val p2 = coordinates.windowToLocal(patchRectInWindow.topRight)
                    val p3 = coordinates.windowToLocal(patchRectInWindow.bottomLeft)
                    val p4 = coordinates.windowToLocal(patchRectInWindow.bottomRight)

                    // 计算本地坐标系下的真实外接矩形范围
                    val minX = minOf(p1.x, p2.x, p3.x, p4.x)
                    val maxX = maxOf(p1.x, p2.x, p3.x, p4.x)
                    val minY = minOf(p1.y, p2.y, p3.y, p4.y)
                    val maxY = maxOf(p1.y, p2.y, p3.y, p4.y)

                    val temp = Rect(minX, minY, maxX, maxY)
                    if (temp != patchAreaCache) {
                        patchAreaCache = temp
                    }
                } else {
                    patchAreaCache = Rect.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val viewMaxWidth = constraints.maxWidth
        val viewMaxHeight = constraints.maxHeight

        val ptToPxScale = viewMaxWidth.toFloat() / pdfSize.x
        val pxToPtScale = 1f / ptToPxScale

        // setPage(int page, PointF size)
        LaunchedEffect(mPageNumber) {
            pageState = pageState.copy(mIsLoading = true, mError = null)
            withContext(Dispatchers.IO) {
                val cookie = Cookie()
                try {
                    // getLinkInfo()
                    val links = try {
                        mCore.getPageLinks(mPageNumber) ?: emptyArray()
                    } catch (e: Exception) {
                        emptyArray()
                    }

                    // 计算动态倍率
                    val parentWidthPx = viewMaxWidth.toFloat()
                    // mSourceScale
                    // 由于定死了容器宽高比，这里 parentWidthPx / pdfSize.x = parentHeightPx / pdfSize.y
                    val mSourceScale = parentWidthPx / pdfSize.x

                    val pageW = (pdfSize.x * mSourceScale).toInt()
                    val pageH = (pdfSize.y * mSourceScale).toInt()
                    val bitmap = createBitmap(pageW, pageH)

                    // mDrawEntire, getDrawPageTask()
                    drawPage(
                        bitmap, mPageNumber,
                        pageW, pageH,
                        0, 0,
                        pageW, pageH,
                        cookie
                    )
                    if (!isActive) {
                        bitmap.recycle(); return@withContext
                    }

                    // TODO: 测试字体
                    // mCore.printFontInformation()

                    withContext(Dispatchers.Main) {
                        pageState = pageState.copy(
                            mEntireBm = bitmap,
                            mLinks = links,
                            mIsLoading = false,
                            mSearchBoxes = mSearchBoxes // setSearchBoxes()
                        )
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // setRenderError(String why)
                        pageState = pageState.copy(
                            mIsLoading = false,
                            mError = e.message ?: "Error loading page"
                        )
                    }
                } finally {
                    cookie.destroy()
                }
            }
        }

        // updateHq()
        // 依据 patchAreaCache 的值请求渲染瓦片层
        // TODO: 准备新增完整图高清渲染
        LaunchedEffect(mPageNumber) {
            snapshotFlow {
                val scale = transform.scaleAnim.value
                val patchArea = patchAreaCache
                // 如果未放大、或视口未传入、或底图还在加载，则返回 null，后续清空 Patch 并跳过
                if (
                    scale <= 1f // 未放大
                    || patchArea.isEmpty // 视口未传入，当前组件不在屏幕中
                    || pageState.mEntireBm == null // 底图还在加载
                ) null
                else scale to patchArea
            }
                .debounce(200.milliseconds)
                .collect { snapshot ->
                    if (snapshot != null) {
                        // updateHq()
                        withContext(Dispatchers.IO) {
                            val (scale, patchArea) = snapshot
                            val cookie = Cookie()
                            try {
                                // to Int
                                val patchX = (patchArea.left * scale).roundToInt()
                                val patchY = (patchArea.top * scale).roundToInt()
                                val patchW = (patchArea.width * scale).roundToInt()
                                val patchH = (patchArea.height * scale).roundToInt()

                                val pageW = (viewMaxWidth * scale).toInt()
                                val pageH = (viewMaxHeight * scale).toInt()
                                if (patchW <= 0 || patchH <= 0) return@withContext

                                val patchBitmap = createBitmap(patchW, patchH)
                                // 先上色让背景不透明，若背景透明则内容会与不清晰的底图重合
                                patchBitmap.eraseColor(backgroundColor.toArgb())

                                drawPage(
                                    patchBitmap, mPageNumber,
                                    pageW, pageH,
                                    patchX, patchY, patchW, patchH,
                                    cookie
                                )

                                // TODO: 测试用，在渲染完成后，盖上一层半透明红
                                val canvas = android.graphics.Canvas(patchBitmap)
                                canvas.drawColor(0x88FF0000.toInt())

                                if (!isActive) {
                                    patchBitmap.recycle(); return@withContext
                                }

                                withContext(Dispatchers.Main) {
                                    val oldBm = patchState.mPatchBm
                                    patchState = patchState.copy(
                                        mPatchBm = patchBitmap,
                                        patchViewSize = IntSize(pageW, pageH),
                                        patchArea = patchArea,
                                        scale = scale
                                    )
                                    oldBm?.recycle()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                cookie.destroy()
                            }
                        }
                    } else {
                        val oldBm = patchState.mPatchBm
                        patchState = PatchState() // removeHq()
                        oldBm?.recycle()
                    }
                }
        }

        // 同步搜索高亮
        LaunchedEffect(mSearchBoxes) {
            pageState = pageState.copy(mSearchBoxes = mSearchBoxes)
        }

        // reinit(), releaseResources(), releaseBitmaps()
        DisposableEffect(mPageNumber) {
            onDispose {
                pageState.mEntireBm?.recycle()
                patchState.mPatchBm?.recycle()
                pageState = PageState()
                patchState = PatchState()
            }
        }

        // UI
        if (pageState.mIsLoading) {
            // mBusyIndicator, blank(int page)
            CircularProgressIndicator()
        }
        // clearRenderError()
        else if (pageState.mError != null) {
            // mErrorIndicator
            Icon(
                painter = painterResource(id = R.drawable.ic_error_red_24dp),
                contentDescription = "Render Error",
                tint = Color.Unspecified
            )
        } else {
            pageState.mEntireBm?.let { entireBitmap ->
                Box(
                    modifier = Modifier
                        // onMeasure()
                        .fillMaxSize()
                        .pointerInput(entireBitmap, pageState.mLinks) {
                            awaitEachGesture {
                                // hitLink(float x, float y)
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val docRelX = down.position.x * pxToPtScale
                                val docRelY = down.position.y * pxToPtScale
                                val hitLink = pageState.mLinks.firstOrNull {
                                    it.bounds.contains(docRelX, docRelY)
                                }
                                if (hitLink != null) {
                                    down.consume() // 点中链接，消费事件
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        up.consume()
                                        if (hitLink.isExternal) {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(hitLink.uri)
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            try {
                                                mContext.startActivity(
                                                    Intent.createChooser(
                                                        intent,
                                                        null
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            viewModel.goToPage(mCore.resolveLink(hitLink))
                                        }
                                    }
                                } else { // 没点中链接
                                    val up = waitForUpOrCancellation() // 等待抬起
                                    if (up != null) {
                                        // 检查是否有选择文字
                                        if (pageState.mSelectionQuads != null) {
                                            up.consume() // 有选文，清除选文并消费事件
                                            pageState = pageState.copy(
                                                mSelectionQuads = null,
                                                mSelectedText = null,
                                                mSelectedArea = null
                                            )
                                        }
                                    }
                                    // 最终若不消费，则事件透传给外层
                                }
                            }
                        }
                        .pointerInput(entireBitmap) {
                            // Detect Text Selection
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val p = Point(
                                        offset.x * pxToPtScale,
                                        offset.y * pxToPtScale
                                    )
                                    updateSelection(p, p)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val p2 = Point(
                                        change.position.x * pxToPtScale,
                                        change.position.y * pxToPtScale
                                    )
                                    updateSelection(pageState.mSelectedArea?.start, p2)
                                }
                            )
                        }
                ) {
                    // Layer 1: mEntire
                    Image(
                        bitmap = entireBitmap.asImageBitmap(),
                        contentDescription = "PDF Page $mPageNumber Base",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // Layer 2: mPatch
                    patchState.mPatchBm?.let { patchBitmap ->
                        if (!patchBitmap.isRecycled) {
                            val density = LocalDensity.current
                            val patchArea = patchState.patchArea
                            //val scale = transform.scaleAnim.value
                            val scale = patchState.scale

                            Image(
                                bitmap = patchBitmap.asImageBitmap(),
                                contentDescription = "PDF Page $mPageNumber HQ Patch",
                                modifier = Modifier
                                    // onLayout()
                                    .offset {
                                        Log.d(APP, "$patchArea")
                                        IntOffset(
                                            x = patchArea.left.roundToInt(),
                                            y = patchArea.top.roundToInt()
                                        )
                                    }
                                    .size(
                                        width = with(density) { patchArea.width.toDp() },
                                        height = with(density) { patchArea.height.toDp() }
                                    )
                                    .graphicsLayer {
                                        // 让瓦片层拥有独立的 GPU Layer 以提升性能
                                        shadowElevation = 0f
                                    },
                            )
                        }
                    }

                    // Layer 3: mSearchView
                    Canvas(modifier = Modifier.matchParentSize()) {
                        // Search Highlight
                        pageState.mSearchBoxes?.forEach { searchBox ->
                            searchBox.forEach { q ->
                                val path = Path().apply {
                                    moveTo(q.ul_x * ptToPxScale, q.ul_y * ptToPxScale)
                                    lineTo(q.ur_x * ptToPxScale, q.ur_y * ptToPxScale)
                                    lineTo(q.lr_x * ptToPxScale, q.lr_y * ptToPxScale)
                                    lineTo(q.ll_x * ptToPxScale, q.ll_y * ptToPxScale)
                                    close()
                                }
                                drawPath(path = path, color = highlightColor)
                            }
                        }

                        // Link Highlight
                        if (mHighlightLinks && pageState.mLinks.isNotEmpty()) {
                            pageState.mLinks.forEach { link ->
                                val bounds = link.bounds
                                drawRect(
                                    color = linkColor,
                                    topLeft = Offset(
                                        bounds.x0 * ptToPxScale,
                                        bounds.y0 * ptToPxScale
                                    ),
                                    size = Size(
                                        (bounds.x1 - bounds.x0) * ptToPxScale,
                                        (bounds.y1 - bounds.y0) * ptToPxScale
                                    )
                                )
                            }
                        }

                        // Selection Highlight
                        pageState.mSelectionQuads?.forEach { q ->
                            val path = Path().apply {
                                moveTo(q.ul_x * ptToPxScale, q.ul_y * ptToPxScale)
                                lineTo(q.ur_x * ptToPxScale, q.ur_y * ptToPxScale)
                                lineTo(q.lr_x * ptToPxScale, q.lr_y * ptToPxScale)
                                lineTo(q.ll_x * ptToPxScale, q.ll_y * ptToPxScale)
                                close()
                            }
                            drawPath(path = path, color = highlightColor)
                        }
                    }

                    // Selection Handle
                    if (!pageState.mSelectionQuads.isNullOrEmpty()) {
                        val quads = pageState.mSelectionQuads!!
                        // Start
                        SelectionHandle(
                            isStart = true,
                            anchor = Offset(
                                quads.first().ll_x * ptToPxScale,
                                quads.first().ll_y * ptToPxScale
                            ),
                            diameter = with(density) {
                                (abs(quads.first().ll_y - quads.first().ul_y) * ptToPxScale).toDp()
                            },
                        ) { dragAmount ->
                            val newStart = Point(
                                (pageState.mSelectedArea?.start?.x
                                    ?: 0f) + dragAmount.x * pxToPtScale,
                                (pageState.mSelectedArea?.start?.y
                                    ?: 0f) + dragAmount.y * pxToPtScale
                            )
                            updateSelection(newStart, pageState.mSelectedArea?.end)
                        }
                        // End
                        SelectionHandle(
                            isStart = false,
                            anchor = Offset(
                                quads.last().lr_x * ptToPxScale,
                                quads.last().lr_y * ptToPxScale
                            ),
                            diameter = with(density) {
                                (abs(quads.first().ll_y - quads.first().ul_y) * ptToPxScale).toDp()
                            },
                        ) { dragAmount ->
                            val newEnd = Point(
                                (pageState.mSelectedArea?.end?.x
                                    ?: 0f) + dragAmount.x * pxToPtScale,
                                (pageState.mSelectedArea?.end?.y ?: 0f) + dragAmount.y * pxToPtScale
                            )
                            updateSelection(pageState.mSelectedArea?.start, newEnd)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 选文手柄，放 Chrome 风格
 */
@Composable
private fun SelectionHandle(
    isStart: Boolean,
    anchor: Offset,
    diameter: Dp,
    rotation: Int = 0, //TODO: Useless
    onDrag: (Offset) -> Unit
) {
    val handleColor = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current
    val size = diameter.coerceIn(16.dp, 48.dp)

    val onDragCache by rememberUpdatedState(onDrag)

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
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragCache(dragAmount)
                }
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