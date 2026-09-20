package io.lin.reader.ui.maintab.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.ui.theme.ReaderTheme

@Immutable
interface SettingListScope {
    @Composable
    fun SettingListItem(
        headlineContent: @Composable () -> Unit,
        modifier: Modifier,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        leadingIcon: ImageVector?,
        trailingContent: @Composable (() -> Unit)?,
    )

    @Composable
    fun SettingListItem(
        headlineContent: @Composable () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        leadingIcon: ImageVector?,
        trailingContent: @Composable (() -> Unit)?,
    )
}

internal object SettingListScopeImpl : SettingListScope {
    @Composable
    override fun SettingListItem(
        headlineContent: @Composable () -> Unit,
        modifier: Modifier,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        leadingIcon: ImageVector?,
        trailingContent: @Composable (() -> Unit)?,
    ) {
        ListItem(
            modifier = modifier,
            leadingContent = leadingIcon?.let {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingContent = trailingContent?.let {
                {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.bodyMedium
                    ) {
                        Box(
                            modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp),
                            contentAlignment = Alignment.Center,
                            propagateMinConstraints = true
                        ) {
                            it()
                        }
                    }
                }
            },
            overlineContent = overlineContent?.let {
                {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.labelSmall
                    ) {
                        it()
                    }
                }
            },
            supportingContent = supportingContent?.let {
                {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Thin
                        )
                    ) {
                        it()
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
            content = {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.W600
                    )
                ) {
                    headlineContent()
                }
            },
        )
    }

    @Composable
    override fun SettingListItem(
        headlineContent: @Composable () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        leadingIcon: ImageVector?,
        trailingContent: @Composable (() -> Unit)?,
    ) {
        ListItem(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingContent = leadingIcon?.let {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingContent = trailingContent?.let {
                {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.bodyMedium
                    ) {
                        Box(
                            modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp),
                            contentAlignment = Alignment.Center,
                            propagateMinConstraints = true
                        ) {
                            it()
                        }
                    }
                }
            },
            overlineContent = overlineContent?.let {
                {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.labelSmall
                    ) {
                        it()
                    }
                }
            },
            supportingContent = supportingContent?.let {
                {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Thin
                        )
                    ) {
                        it()
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
            content = {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.W600
                    )
                ) {
                    headlineContent()
                }
            },
        )
    }
}

/**
 * A helper function to make it easier to call SettingListItem with default values within the scope.
 */
@Composable
fun SettingListScope.SettingListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SettingListItem(
        headlineContent = headlineContent,
        modifier = modifier,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingIcon = leadingIcon,
        trailingContent = trailingContent
    )
}

@Composable
fun SettingListScope.SettingListItem(
    headlineContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SettingListItem(
        headlineContent = headlineContent,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingIcon = leadingIcon,
        trailingContent = trailingContent
    )
}

@Composable
fun SettingList(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable SettingListScope.() -> Unit,
) {
    Column(modifier = modifier) {
        title?.let {
            Box(
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) { it() }
            }
        }
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.outlinedCardElevation(0.5.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )
        ) {
            Column {
                ProvideTextStyle(
                    value = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    SettingListScopeImpl.content()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingListPreview() {
    ReaderTheme {
        Column(modifier = Modifier.padding(4.dp)) {
            SettingList(title = { Text("通用设置") }) {
                SettingListItem(
                    headlineContent = { Text("个人资料") },
                    supportingContent = { Text("修改头像、昵称和个人说明") },
                    leadingIcon = Icons.Default.Settings,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                SettingListItem(
                    headlineContent = { Text("夜间模式") },
                    overlineContent = { Text("显示选项") },
                    trailingContent = {
                        Switch(checked = true, onCheckedChange = {})
                    }
                )
            }
        }
    }
}