package io.lin.reader.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.lin.reader.ReaderApplication
import io.lin.reader.ui.feature.bookmark.BookmarkScreenViewModel
import io.lin.reader.ui.feature.favourite.FavouriteScreenViewModel
import io.lin.reader.ui.feature.history.HistoryScreenViewModel
import io.lin.reader.ui.feature.reading.ReadingScreenViewModel
import io.lin.reader.ui.feature.setting.SettingScreenViewModel
import io.lin.reader.ui.feature.shelf.ShelfScreenViewModel

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [ReaderApplication].
 */
fun CreationExtras.readerApplication(): ReaderApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as ReaderApplication)

object ViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for [AppViewModel]
        initializer {
            AppViewModel(
                booksRepository = readerApplication().container.booksRepository,
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
                userPreferencesRepository = readerApplication().container.userPreferencesRepository,
                booksRepository = readerApplication().container.booksRepository
            )
        }
    }
}
