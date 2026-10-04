package io.lin.reader.data.preferences

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.edit
import io.lin.reader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


enum class ReadingMode(
    @StringRes val displayName: Int
) {
    Single(
        displayName = R.string.reading_mode_label_single
    ),
    Dual(
        displayName = R.string.reading_mode_label_dual
    ),
    Scroll(
        displayName = R.string.reading_mode_label_scrollable
    )
}

enum class PageAlignment(
    @StringRes val displayName: Int
) {
    Vertical(R.string.page_alignment_vertical),
    Horizontal(R.string.page_alignment_horizontal)
}

enum class InteractionStyle(
    @StringRes val displayName: Int
) {
    Style0(R.string.interaction_style_vertical),
    Style1(R.string.interaction_style_horizontal),
    Style2(R.string.interaction_style_L),
    Style3(R.string.interaction_style_Mixed)
}

data class ReaderColor(
    val background: Int = Color.BLACK,
    val page: Int = Color.WHITE,
    val filter: Int = Color.TRANSPARENT
)

enum class CropMode(
    @StringRes val displayName: Int,
    val icon: ImageVector
) {
    None(
        displayName = R.string.reading_crop_none,
        icon = Icons.Default.CropFree
    ),           // 不裁剪
    Horizontal(
        displayName = R.string.reading_crop_horizontal,
        icon = Icons.Default.CropLandscape
    ),     // 仅左右 (消除书沟)
    Vertical(
        displayName = R.string.reading_crop_vertical,
        icon = Icons.Default.CropPortrait
    ),       // 仅上下 (去除页眉页脚)
    All(
        displayName = R.string.reading_crop_all,
        icon = Icons.Default.Crop
    )             // 四周全部裁剪 (自动切边)
}

interface ReaderPreferencesInterface {
    val readingMode: ReadingMode
    val pageAlignment: PageAlignment
    val pagePaddingRatio: Float
    val separateCover: Boolean
    val rtlMode: Boolean
    val cropMode: CropMode
    val fixedPageIndicator: Boolean
    val interactionStyle: InteractionStyle
    val isInteractionHintPending: Boolean
    val readerColor: ReaderColor
    val isReflow: Boolean

    fun toggleRtlMode()
    fun updateCropMode(mode: CropMode)
    fun updateReadingMode(mode: ReadingMode)
    fun updatePageAlignment(alignment: PageAlignment)
    fun updatePagePaddingRatio(paddingRatio: Float)
    fun toggleSeparateCover()
    fun toggleFixedPageIndicator()
    fun updateInteractionStyle(style: InteractionStyle)
    fun clearInteractionStyleHint()
    fun updateBackgroundColor(color: Int)
    fun updatePageColor(color: Int)
    fun updateFilterColor(color: Int)
    fun toggleReflowMode()
}

class ReaderPreferences(
    private val repo: UserPreferencesRepository,
    private val scope: CoroutineScope
) : ReaderPreferencesInterface {
    override var readingMode by mutableStateOf(ReadingMode.Single)
    override var pageAlignment by mutableStateOf(PageAlignment.Vertical)
    override var pagePaddingRatio by mutableFloatStateOf(0f)
    override var separateCover by mutableStateOf(false)
    override var rtlMode by mutableStateOf(false)
    override var cropMode by mutableStateOf(CropMode.None)
    override var fixedPageIndicator by mutableStateOf(false)
    override var interactionStyle by mutableStateOf(InteractionStyle.Style0)
    override var isInteractionHintPending by mutableStateOf(true)
    override var readerColor by mutableStateOf(ReaderColor())
    override var isReflow by mutableStateOf(false)

    init {
        scope.launch {
            repo.dataStore.data.collect { preferences ->
                val readingModeName = preferences[UserPreferencesRepository.READING_MODE] ?: ReadingMode.Single.name
                readingMode = try {
                    ReadingMode.valueOf(readingModeName)
                } catch (e: Exception) {
                    ReadingMode.Single
                }

                val alignmentName = preferences[UserPreferencesRepository.PAGE_ALIGNMENT] ?: PageAlignment.Vertical.name
                pageAlignment = try {
                    PageAlignment.valueOf(alignmentName)
                } catch (e: Exception) {
                    PageAlignment.Vertical
                }

                pagePaddingRatio = preferences[UserPreferencesRepository.PAGE_PADDING_RATIO] ?: 0f
                rtlMode = preferences[UserPreferencesRepository.RTL_MODE] ?: false

                val cropModeName = preferences[UserPreferencesRepository.CROP_MODE] ?: CropMode.None.name
                cropMode = try {
                    CropMode.valueOf(cropModeName)
                } catch (e: Exception) {
                    CropMode.None
                }

                separateCover = preferences[UserPreferencesRepository.SEPARATE_COVER] ?: false
                fixedPageIndicator = preferences[UserPreferencesRepository.FIXED_PAGE_INDICATOR] ?: false
                
                val interactionStyleName = preferences[UserPreferencesRepository.INTERACTION_STYLE] ?: InteractionStyle.Style0.name
                interactionStyle = try {
                    InteractionStyle.valueOf(interactionStyleName)
                } catch (e: Exception) {
                    InteractionStyle.Style0
                }

                readerColor = ReaderColor(
                    background = preferences[UserPreferencesRepository.BACKGROUND_COLOR] ?: Color.BLACK,
                    page = preferences[UserPreferencesRepository.PAGE_COLOR] ?: Color.WHITE,
                    filter = preferences[UserPreferencesRepository.FILTER_COLOR] ?: Color.TRANSPARENT
                )

                isInteractionHintPending = preferences[UserPreferencesRepository.INTERACTION_STYLE_HINT_PENDING] ?: true
                isReflow = preferences[UserPreferencesRepository.IS_REFLOW] ?: false
            }
        }
    }

    override fun toggleRtlMode() {
        scope.launch {
            val current = rtlMode
            repo.dataStore.edit { it[UserPreferencesRepository.RTL_MODE] = !current }
        }
    }

    override fun updateCropMode(mode: CropMode) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.CROP_MODE] = mode.name }
        }
    }

    override fun updateReadingMode(mode: ReadingMode) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.READING_MODE] = mode.name }
        }
    }

    override fun updatePageAlignment(alignment: PageAlignment) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.PAGE_ALIGNMENT] = alignment.name }
        }
    }

    override fun updatePagePaddingRatio(paddingRatio: Float) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.PAGE_PADDING_RATIO] = paddingRatio }
        }
    }

    override fun toggleSeparateCover() {
        scope.launch {
            val current = separateCover
            repo.dataStore.edit { it[UserPreferencesRepository.SEPARATE_COVER] = !current }
        }
    }

    override fun toggleFixedPageIndicator() {
        scope.launch {
            val current = fixedPageIndicator
            repo.dataStore.edit { it[UserPreferencesRepository.FIXED_PAGE_INDICATOR] = !current }
        }
    }

    override fun updateInteractionStyle(style: InteractionStyle) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.INTERACTION_STYLE] = style.name
                it[UserPreferencesRepository.INTERACTION_STYLE_HINT_PENDING] = true
            }
        }
    }

    override fun clearInteractionStyleHint() {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.INTERACTION_STYLE_HINT_PENDING] = false
            }
        }
    }

    override fun updateBackgroundColor(color: Int) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.BACKGROUND_COLOR] = color }
        }
    }

    override fun updatePageColor(color: Int) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.PAGE_COLOR] = color }
        }
    }

    override fun updateFilterColor(color: Int) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.FILTER_COLOR] = color }
        }
    }

    override fun toggleReflowMode() {
        scope.launch {
            val current = isReflow
            repo.dataStore.edit { it[UserPreferencesRepository.IS_REFLOW] = !current }
        }
    }
}

class ReaderPreferencesForTest : ReaderPreferencesInterface {
    override var readingMode = ReadingMode.Single
    override var pageAlignment = PageAlignment.Vertical
    override var pagePaddingRatio = 0f
    override var separateCover = false
    override var rtlMode = false
    override var cropMode = CropMode.None
    override var fixedPageIndicator = false
    override var interactionStyle = InteractionStyle.Style0
    override var isInteractionHintPending = true
    override var readerColor = ReaderColor()
    override var isReflow = false

    override fun toggleRtlMode() {}
    override fun updateCropMode(mode: CropMode) {}
    override fun updateReadingMode(mode: ReadingMode) {}
    override fun updatePageAlignment(alignment: PageAlignment) {}
    override fun updatePagePaddingRatio(paddingRatio: Float) {}
    override fun toggleSeparateCover() {}
    override fun toggleFixedPageIndicator() {}
    override fun updateInteractionStyle(style: InteractionStyle) {}
    override fun clearInteractionStyleHint() {}
    override fun updateBackgroundColor(color: Int) {}
    override fun updatePageColor(color: Int) {}
    override fun updateFilterColor(color: Int) {}
    override fun toggleReflowMode() {}
}
