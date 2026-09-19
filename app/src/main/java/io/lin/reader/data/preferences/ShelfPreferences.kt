package io.lin.reader.data.preferences

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "ShelfPreferences.kt"

enum class SeriesSortMethod {
    Name,
    CreateTime
}

enum class VolumeSortMethod {
    Name,
    CreateTime,
    LastReadTime
}

data class SortPreference<T>(
    val sortMethod: T,
    val isAscending: Boolean
)

interface ShelfPreferencesInterface {
    val seriesSortMethod: SeriesSortMethod
    val seriesSortAscending: Boolean
    val volumeSortMethod: VolumeSortMethod
    val volumeSortAscending: Boolean

    fun updateSeriesSortMethod(method: SeriesSortMethod)
    fun toggleSeriesSortAscending()
    fun updateVolumeSortMethod(method: VolumeSortMethod)
    fun toggleVolumeSortAscending()
}

data class ShelfPreferences(
    private val repo: UserPreferencesRepository,
    private val scope: CoroutineScope
) : ShelfPreferencesInterface {
    override var seriesSortMethod by mutableStateOf(SeriesSortMethod.Name)
    override var seriesSortAscending by mutableStateOf(true)
    override var volumeSortMethod by mutableStateOf(VolumeSortMethod.Name)
    override var volumeSortAscending by mutableStateOf(true)

    init {
        scope.launch {
            repo.dataStore.data.collect { preferences ->
                val seriesMethodName = preferences[UserPreferencesRepository.SERIES_SORT_METHOD]
                    ?: SeriesSortMethod.Name.name
                seriesSortMethod = try {
                    SeriesSortMethod.valueOf(seriesMethodName)
                } catch (e: Exception) {
                    e.message?.let { Log.e(TAG, it) }
                    SeriesSortMethod.Name
                }
                seriesSortAscending =
                    preferences[UserPreferencesRepository.SERIES_SORT_ASCENDING] ?: true

                val volumeMethodName = preferences[UserPreferencesRepository.VOLUME_SORT_METHOD]
                    ?: VolumeSortMethod.Name.name
                volumeSortMethod = try {
                    VolumeSortMethod.valueOf(volumeMethodName)
                } catch (e: Exception) {
                    e.message?.let { Log.e(TAG, it) }
                    VolumeSortMethod.Name
                }
                volumeSortAscending =
                    preferences[UserPreferencesRepository.VOLUME_SORT_ASCENDING] ?: true

            }
        }
    }

    override fun updateSeriesSortMethod(method: SeriesSortMethod) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.SERIES_SORT_METHOD] = method.name }
        }
    }

    override fun toggleSeriesSortAscending() {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.SERIES_SORT_ASCENDING] = !seriesSortAscending
            }
        }
    }

    override fun updateVolumeSortMethod(method: VolumeSortMethod) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.VOLUME_SORT_METHOD] = method.name }
        }
    }

    override fun toggleVolumeSortAscending() {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.VOLUME_SORT_ASCENDING] = !volumeSortAscending
            }
        }
    }
}

class ShelfPreferencesForTest : ShelfPreferencesInterface {
    override var seriesSortMethod = SeriesSortMethod.Name
    override var seriesSortAscending = true
    override var volumeSortMethod = VolumeSortMethod.Name
    override var volumeSortAscending = true

    override fun updateSeriesSortMethod(method: SeriesSortMethod) {}
    override fun toggleSeriesSortAscending() {}
    override fun updateVolumeSortMethod(method: VolumeSortMethod) {}
    override fun toggleVolumeSortAscending() {}
}