package io.lin.reader.data.appcontainer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.booksrepository.OfflineBooksRepository
import io.lin.reader.data.database.ReaderDatabase
import io.lin.reader.data.preferences.UserPreferencesRepository

private const val USER_PREFERENCES_NAME = "user_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val booksRepository: BooksRepository
    val userPreferencesRepository: UserPreferencesRepository
}

/**
 * [AppContainer] implementation that provides instance of [OfflineBooksRepository]
 */
class AppDataContainer(private val context: Context) : AppContainer {
    /**
     * Implementation for [BooksRepository]
     */
    override val booksRepository: BooksRepository by lazy {
        val database = ReaderDatabase.Companion.getDatabase(context)
        OfflineBooksRepository(
            database.seriesDao(),
            database.volumeDao(),
            database.bookmarkDao()
        )
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
    }
}
