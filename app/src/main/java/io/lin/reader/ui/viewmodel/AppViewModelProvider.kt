package io.lin.reader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.lin.reader.ReaderApplication

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [ReaderApplication].
 */
fun CreationExtras.readerApplication(): ReaderApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as ReaderApplication)

object AppViewModelProvider {
    val Factory = viewModelFactory {

        // Initializer for [MainViewModel]
        initializer {
            MainViewModel(
                userPreferencesRepository = readerApplication().container.userPreferencesRepository
            )
        }

        // Initializer for [ShelfScreenViewModel]
        initializer {
            ShelfScreenViewModel(
                booksRepository = readerApplication().container.booksRepository,
                userPreferencesRepository = readerApplication().container.userPreferencesRepository,
                application = readerApplication()
            )
        }

        // Initializer for [FavouriteScreenViewModel]
        initializer {
            FavouriteScreenViewModel(
                booksRepository = readerApplication().container.booksRepository,
                userPreferencesRepository = readerApplication().container.userPreferencesRepository,
            )
        }

        // Initializer for [BookmarkScreenViewModel]
        initializer {
            BookmarkScreenViewModel(
                booksRepository = readerApplication().container.booksRepository,
                userPreferencesRepository = readerApplication().container.userPreferencesRepository
            )
        }

        // Initializer for [HistoryScreenViewModel]
        initializer {
            HistoryScreenViewModel(
                booksRepository = readerApplication().container.booksRepository,
                userPreferencesRepository = readerApplication().container.userPreferencesRepository,
            )
        }

        // Initializer for [SettingScreenViewModel]
        initializer {
            SettingScreenViewModel(
                booksRepository = readerApplication().container.booksRepository,
                userPreferencesRepository = readerApplication().container.userPreferencesRepository,
            )
        }

        // Initializer for [ReadingScreenViewModel]
        initializer {
            ReadingScreenViewModel(
                application = readerApplication(),
                savedStateHandle = this.createSavedStateHandle(),
                userPreferencesRepository = readerApplication().container.userPreferencesRepository,
                booksRepository = readerApplication().container.booksRepository
            )
        }
    }
}
