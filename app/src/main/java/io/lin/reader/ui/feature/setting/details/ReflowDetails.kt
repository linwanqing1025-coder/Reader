package io.lin.reader.ui.feature.setting.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Margin
import androidx.compose.material.icons.filled.Padding
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lin.reader.R
import io.lin.reader.data.preferences.ReaderPreferencesForTest
import io.lin.reader.data.preferences.ReaderPreferencesInterface
import io.lin.reader.data.preferences.ReflowPreferencesForTest
import io.lin.reader.data.preferences.ReflowPreferencesInterface
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.feature.reading.feature.font.PRESET_FONTS
import io.lin.reader.ui.feature.setting.SettingList
import io.lin.reader.ui.feature.setting.SettingListItem
import io.lin.reader.ui.theme.ReaderTheme

@Composable
fun ReflowDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    readerPreferences: ReaderPreferencesInterface,
    reflowPreferences: ReflowPreferencesInterface
) {
    val listState = rememberLazyListState()
    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 150f).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DynamicTopAppBar(
                title = stringResource(R.string.setting_reflow),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        ReflowDetails(
            modifier = Modifier.padding(innerPadding),
            readerPreferences = readerPreferences,
            reflowPreferences = reflowPreferences,
            listState = listState
        )
    }
}

@Composable
fun ReflowDetails(
    modifier: Modifier = Modifier,
    readerPreferences: ReaderPreferencesInterface,
    reflowPreferences: ReflowPreferencesInterface,
    listState: LazyListState = rememberLazyListState()
) {
    var isShowingColorSelector by remember { mutableStateOf(false) }
    var colorPickerTitle by remember { mutableStateOf("") }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var onColorChangeLambda by remember { mutableStateOf<(Color) -> Unit>({}) }

    BackHandler(enabled = isShowingColorSelector) {
        isShowingColorSelector = false
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        // 总开关
        item {
            SettingList(
                title = { Text("Reflow") }
            ) {
                SettingListItem(
                    headlineContent = { Text("Reflow Mode") }
                ) {
                    Switch(
                        checked = readerPreferences.isReflow,
                        onCheckedChange = { readerPreferences.toggleReflowMode() },
                    )
                }
                SettingListItem(
                    headlineContent = { Text("Use Publisher Style") },
                    supportingContent = { Text("Ignore custom reflow settings") }
                ) {
                    Switch(
                        checked = reflowPreferences.usePublisherStyle,
                        onCheckedChange = { reflowPreferences.toggleUsePublisherStyle() },
                    )
                }
            }
        }

        // Typography
        item {
            SettingList(
                title = { Text("Typography") }
            ) {
                // 字体
                SettingListItem(
                    leadingIcon = Icons.Default.TextFormat,
                    headlineContent = { Text("Font Family") }
                ) {
                    StringDropdownSetting(
                        value = reflowPreferences.fontFamily,
                        onValueChange = reflowPreferences::updateFontFamily,
                        options = PRESET_FONTS.map { it.family },
                        modifier = Modifier.width(140.dp)
                    )
                }
                // 字号
                SettingListItem(
                    leadingIcon = Icons.Default.FormatSize,
                    headlineContent = { Text("Font Size") }
                ) {
                    NumericDropdownSetting(
                        value = reflowPreferences.fontSize,
                        onValueChange = reflowPreferences::updateFontSize,
                        options = listOf(4, 6, 8, 10, 12, 14, 16),
                        suffix = "pt"
                    )
                }
                // 行距
                SettingListItem(
                    leadingIcon = Icons.Default.FormatLineSpacing,
                    headlineContent = { Text("Line Height") }
                ) {
                    NumericDropdownSetting(
                        value = reflowPreferences.lineHeight,
                        onValueChange = reflowPreferences::updateLineHeight,
                        options = listOf(1.0f, 1.2f, 1.5f, 1.8f, 2.0f),
                        suffix = "x"
                    )
                }
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Font Weight")
                        Spacer(Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val isBold = reflowPreferences.fontBold
                            SegmentedButton(
                                selected = !isBold,
                                onClick = { reflowPreferences.updateFontBold(false) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                label = { Text("Normal", fontSize = 11.sp) }
                            )
                            SegmentedButton(
                                selected = isBold,
                                onClick = { reflowPreferences.updateFontBold(true) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = { SegmentedButtonDefaults.Icon(active = isBold) },
                                label = { Text("Bold", fontSize = 11.sp) }
                            )
                        }
                    }
                    Column {
                        Text("Font Style")
                        Spacer(Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val isItalic = reflowPreferences.fontItalic
                            SegmentedButton(
                                selected = !isItalic,
                                onClick = { reflowPreferences.updateFontItalic(false) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                label = { Text("Regular", fontSize = 11.sp) }
                            )
                            SegmentedButton(
                                selected = isItalic,
                                onClick = { reflowPreferences.updateFontItalic(true) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = { SegmentedButtonDefaults.Icon(active = isItalic) },
                                label = { Text("Italic", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Colors
        item {
            AnimatedContent(
                targetState = isShowingColorSelector,
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
                label = stringResource(R.string.color_picker)
            ) { showingSelector ->
                if (!showingSelector) {
                    SettingList(
                        title = { Text(stringResource(R.string.reader_color_label_color)) }
                    ) {
                        SettingListItem(
                            headlineContent = { Text("Background Color") },
                            onClick = {
                                colorPickerTitle = "Background Color"
                                currentColor = Color(reflowPreferences.backgroundColor)
                                onColorChangeLambda = reflowPreferences::updateBackgroundColor
                                isShowingColorSelector = true
                            },
                        )
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(reflowPreferences.backgroundColor))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        SettingListItem(
                            headlineContent = { Text("Text Color") },
                            onClick = {
                                colorPickerTitle = "Text Color"
                                currentColor = Color(reflowPreferences.textColor)
                                onColorChangeLambda = reflowPreferences::updateTextColor
                                isShowingColorSelector = true
                            },
                        )
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(reflowPreferences.textColor))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                } else {
                    ColorPicker(
                        title = colorPickerTitle,
                        currentColor = currentColor,
                        onColorChange = {
                            currentColor = it
                            onColorChangeLambda(it)
                        },
                        onBackClick = { isShowingColorSelector = false }
                    )
                }
            }
        }

        // Paragraph
        item {
            SettingList(
                title = { Text("Paragraph") }
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Alignment",
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val aligns = listOf("left", "center", "right", "justify")
                    aligns.forEachIndexed { index, align ->
                        val isSelected = reflowPreferences.textAlign == align
                        SegmentedButton(
                            selected = isSelected,
                            onClick = { reflowPreferences.updateTextAlign(align) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = aligns.size
                            ),
                            icon = {
                                SegmentedButtonDefaults.Icon(
                                    active = isSelected,
                                    activeContent = {
                                        Icon(
                                            imageVector = when (align) {
                                                "left" -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                                "center" -> Icons.Default.FormatAlignCenter
                                                "right" -> Icons.AutoMirrored.Filled.FormatAlignRight
                                                else -> Icons.Default.FormatAlignJustify
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    })
                            },
                            label = {
                                Text(
                                    align.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 段落缩进
                SettingListItem(
                    headlineContent = { Text("Text Indent") },
                    leadingIcon = Icons.AutoMirrored.Filled.FormatIndentIncrease
                ) {
                    NumericDropdownSetting(
                        value = reflowPreferences.textIndent,
                        onValueChange = reflowPreferences::updateTextIndent,
                        options = listOf(0f, 12f, 24f, 32f, 48f),
                        suffix = "pt"
                    )
                }
                // 段落间距
                SettingListItem(
                    headlineContent = { Text("Paragraph Spacing") },
                ) {
                    NumericDropdownSetting(
                        value = reflowPreferences.paragraphSpacing,
                        onValueChange = reflowPreferences::updateParagraphSpacing,
                        options = listOf(0f, 8f, 12f, 16f, 24f, 32f),
                        suffix = "pt"
                    )
                }
                // 连字符控制
                SettingListItem(
                    headlineContent = { Text("Hyphenation") },
                ) {
                    Switch(
                        checked = reflowPreferences.hyphenation,
                        onCheckedChange = { reflowPreferences.toggleHyphenation() }
                    )
                }
            }
        }

        // List Styles
        item {
            SettingList(
                title = { Text("List Styles") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bullet Type",
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val types = listOf("disc", "circle", "square", "decimal")
                        types.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = reflowPreferences.listStyleType == type,
                                onClick = { reflowPreferences.updateListStyleType(type) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = types.size
                                ),
                                label = {
                                    Text(
                                        type.replaceFirstChar { it.uppercase() },
                                        fontSize = 10.sp
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.5f
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Position",
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val positions = listOf("outside", "inside")
                        positions.forEachIndexed { index, pos ->
                            SegmentedButton(
                                selected = reflowPreferences.listStylePosition == pos,
                                onClick = { reflowPreferences.updateListStylePosition(pos) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = positions.size
                                ),
                                label = {
                                    Text(
                                        pos.replaceFirstChar { it.uppercase() },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Spacing
        item {
            SettingList(
                title = { Text("Spacing") }
            ) {
                SettingListItem(
                    headlineContent = { Text("Margin") },
                    leadingIcon = Icons.Default.Margin
                ) {
                    NumericDropdownSetting(
                        value = reflowPreferences.horizontalMargin,
                        onValueChange = {
                            reflowPreferences.updateMargins(
                                it,
                                it
                            )
                        },
                        options = listOf(0f, 8f, 16f, 24f, 32f, 48f),
                        suffix = "pt"
                    )
                }
                SettingListItem(
                    headlineContent = { Text("Padding") },
                    leadingIcon = Icons.Default.Padding
                ) {
                    NumericDropdownSetting(
                        value = reflowPreferences.horizontalPadding,
                        onValueChange = {
                            reflowPreferences.updatePaddings(
                                it,
                                it
                            )
                        },
                        options = listOf(0f, 8f, 16f, 24f, 32f, 48f),
                        suffix = "pt"
                    )
                }
            }
        }

        // Page Breaks
        item {
            SettingList(
                title = { Text("Page Breaks") }
            ) {
                SettingListItem(
                    headlineContent = { Text("Before") }
                ) {
                    StringDropdownSetting(
                        value = reflowPreferences.pageBreakBefore,
                        onValueChange = reflowPreferences::updatePageBreakBefore,
                        options = listOf("auto", "always", "avoid", "left", "right"),
                        modifier = Modifier.width(100.dp)
                    )
                }
                SettingListItem(
                    headlineContent = { Text("After") }
                ) {
                    StringDropdownSetting(
                        value = reflowPreferences.pageBreakAfter,
                        onValueChange = reflowPreferences::updatePageBreakAfter,
                        options = listOf("auto", "always", "avoid", "left", "right"),
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StringDropdownSetting(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .height(36.dp),
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember { MutableInteractionSource() },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = remember { MutableInteractionSource() },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            focusedBorderThickness = 1.dp,
                            unfocusedBorderThickness = 1.dp
                        )
                    }
                )
            }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onValueChange(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericDropdownSetting(
    value: Int,
    onValueChange: (Int) -> Unit,
    options: List<Int>,
    suffix: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    var textValue by remember(value) {
        mutableStateOf(value.toString())
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(80.dp)
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        textValue = newValue
                        newValue.toIntOrNull()?.let { onValueChange(it) }
                    }
                },
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = true
                    )
                    .fillMaxWidth()
                    .height(36.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = textValue,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = true,
                                isError = false,
                                interactionSource = interactionSource,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                focusedBorderThickness = 1.dp,
                                unfocusedBorderThickness = 1.dp,
                            )
                        }
                    )
                }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = {
                        Text(
                            text = option.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = { onValueChange(option); expanded = false })
                }
            }
        }
        if (suffix.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericDropdownSetting(
    value: Float,
    onValueChange: (Float) -> Unit,
    options: List<Float>,
    suffix: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    var textValue by remember(value) {
        mutableStateOf(
            if (value % 1.0f == 0.0f) value.toInt().toString() else value.toString()
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(80.dp)
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                        textValue = newValue
                        newValue.toFloatOrNull()?.let { onValueChange(it) }
                    }
                },
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = true
                    )
                    .fillMaxWidth()
                    .height(36.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = textValue,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = true,
                                isError = false,
                                interactionSource = interactionSource,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                focusedBorderThickness = 1.dp,
                                unfocusedBorderThickness = 1.dp,
                            )
                        }
                    )
                }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = {
                        Text(
                            if (option % 1.0f == 0.0f) option.toInt()
                                .toString() else option.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, onClick = { onValueChange(option); expanded = false })
                }
            }
        }
        if (suffix.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingItemIcon(imageVector: ImageVector) {
    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(modifier = Modifier.width(12.dp))
}

@Preview(showBackground = true)
@Composable
private fun ReflowDetailsScreenPreview() {
    ReaderTheme {
        ReflowDetailsScreen(
            readerPreferences = ReaderPreferencesForTest(),
            reflowPreferences = ReflowPreferencesForTest()
        )
    }
}