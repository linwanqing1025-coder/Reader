package io.lin.reader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.lin.reader.ui.screen.setting.details.DarkMode
import io.lin.reader.ui.screen.setting.details.ThemeColor
import io.lin.reader.ui.screen.setting.details.ThemeContrast
import io.lin.reader.ui.theme.color.BreezeTheme
import io.lin.reader.ui.theme.color.DefaultTheme
import io.lin.reader.ui.theme.color.HoneyTheme
import io.lin.reader.ui.theme.color.MapleTheme
import io.lin.reader.ui.theme.color.MeadowTheme

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

interface AppThemePalette {
    val lightScheme: ColorScheme
    val darkScheme: ColorScheme
    val mediumContrastLightColorScheme: ColorScheme
    val highContrastLightColorScheme: ColorScheme
    val mediumContrastDarkColorScheme: ColorScheme
    val highContrastDarkColorScheme: ColorScheme
}

@Composable
fun ReaderTheme(
    darkMode: DarkMode = DarkMode.System,
    themeColor: ThemeColor = ThemeColor.Default,
    themeContrast: ThemeContrast = ThemeContrast.Light,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkMode) {
        DarkMode.System -> isSystemInDarkTheme()
        DarkMode.Light -> false
        DarkMode.Dark -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = if (themeColor == ThemeColor.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val palette: AppThemePalette = when (themeColor) {
            ThemeColor.Default, ThemeColor.Dynamic -> DefaultTheme
            ThemeColor.Maple -> MapleTheme
            ThemeColor.Meadow -> MeadowTheme
            ThemeColor.Breeze -> BreezeTheme
            ThemeColor.Honey -> HoneyTheme
        }
        if (darkTheme) {
            when (themeContrast) {
                ThemeContrast.Light -> palette.darkScheme
                ThemeContrast.Medium -> palette.mediumContrastDarkColorScheme
                ThemeContrast.High -> palette.highContrastDarkColorScheme
            }
        } else {
            when (themeContrast) {
                ThemeContrast.Light -> palette.lightScheme
                ThemeContrast.Medium -> palette.mediumContrastLightColorScheme
                ThemeContrast.High -> palette.highContrastLightColorScheme
            }
        }
    }

    // 设置状态栏和导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
