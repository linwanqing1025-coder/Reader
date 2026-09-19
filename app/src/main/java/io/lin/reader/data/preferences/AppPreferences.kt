package io.lin.reader.data.preferences

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import io.lin.reader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ThemeColor(@get:StringRes val labelRes: Int) {
    Dynamic(R.string.theme_color_dynamic),
    Default(R.string.theme_color_default),
    Maple(R.string.theme_color_maple),
    Meadow(R.string.theme_color_meadow),
    Breeze(R.string.theme_color_breeze),
    Honey(R.string.theme_color_honey)
}

enum class ThemeContrast(@get:StringRes val labelRes: Int) {
    Light(R.string.contrast_light),
    Medium(R.string.contrast_medium),
    High(R.string.contrast_high)
}

enum class DarkMode {
    System, Light, Dark
}

enum class AppLanguage(@get:StringRes val labelRes: Int) {
    System(R.string.language_system),
    English(R.string.language_english),
    ChineseSimplified(R.string.language_chinese_simplified),
    ChineseTraditional(R.string.language_chinese_traditional),
    Japanese(R.string.language_japanese)
}

interface AppPreferencesInterface {
    val theme: DarkMode
    val language: AppLanguage
    val themeColor: ThemeColor
    val themeContrast: ThemeContrast
    val floatingNavigationBar: Boolean

    fun updateTheme(theme: DarkMode)
    fun updateThemeColor(color: ThemeColor)
    fun updateThemeContrast(contrast: ThemeContrast)
    fun updateLanguage(language: AppLanguage)
    fun toggleFloatingNavigationBar()
}

class AppPreferences(
    private val repo: UserPreferencesRepository,
    private val scope: CoroutineScope
) : AppPreferencesInterface {
    override var theme by mutableStateOf(DarkMode.System)
    override var language by mutableStateOf(AppLanguage.System)
    override var themeColor by mutableStateOf(ThemeColor.Default)
    override var themeContrast by mutableStateOf(ThemeContrast.Light)
    override var floatingNavigationBar by mutableStateOf(true)

    init {
        scope.launch {
            repo.dataStore.data.collect { preferences ->
                theme = try {
                    DarkMode.valueOf(
                        preferences[UserPreferencesRepository.APP_THEME] ?: DarkMode.System.name
                    )
                } catch (e: Exception) {
                    DarkMode.System
                }

                language = try {
                    AppLanguage.valueOf(
                        preferences[UserPreferencesRepository.APP_LANGUAGE]
                            ?: AppLanguage.System.name
                    )
                } catch (e: Exception) {
                    AppLanguage.System
                }

                themeColor = try {
                    ThemeColor.valueOf(
                        preferences[UserPreferencesRepository.APP_THEME_COLOR]
                            ?: ThemeColor.Default.name
                    )
                } catch (e: Exception) {
                    ThemeColor.Default
                }

                themeContrast = try {
                    ThemeContrast.valueOf(
                        preferences[UserPreferencesRepository.APP_THEME_CONTRAST]
                            ?: ThemeContrast.Light.name
                    )
                } catch (e: Exception) {
                    ThemeContrast.Light
                }

                floatingNavigationBar = preferences[UserPreferencesRepository.APP_FLOATING_NAVIGATION_BAR] ?: true
            }
        }
    }

    override fun updateTheme(theme: DarkMode) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.APP_THEME] = theme.name }
        }
    }

    override fun updateThemeColor(color: ThemeColor) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.APP_THEME_COLOR] = color.name }
        }
    }

    override fun updateThemeContrast(contrast: ThemeContrast) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.APP_THEME_CONTRAST] = contrast.name }
        }
    }

    override fun updateLanguage(language: AppLanguage) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.APP_LANGUAGE] = language.name }
        }
    }

    override fun toggleFloatingNavigationBar() {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.APP_FLOATING_NAVIGATION_BAR] =
                    !floatingNavigationBar
            }
        }
    }
}

class AppPreferencesForTest : AppPreferencesInterface {
    override var theme = DarkMode.System
    override var language = AppLanguage.System
    override var themeColor = ThemeColor.Default
    override var themeContrast = ThemeContrast.Light
    override val floatingNavigationBar: Boolean = true

    override fun updateTheme(theme: DarkMode) {}
    override fun updateThemeColor(color: ThemeColor) {}
    override fun updateThemeContrast(contrast: ThemeContrast) {}
    override fun updateLanguage(language: AppLanguage) {}
    override fun toggleFloatingNavigationBar() {}
}