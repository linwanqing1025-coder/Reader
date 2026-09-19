# FavouriteScreenViewModel Refactoring Plan

This plan outlines the steps to refactor `FavouriteScreenViewModel` to use `ShelfPreferences` for series sorting, aligning it with `ShelfScreenViewModel`.

## Proposed Changes

### 1. Data Model Refinement

#### [MODIFY] [FavouriteScreenViewModel.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/favourite/FavouriteScreenViewModel.kt)
- Update `FavouriteUiState` to ensure it can hold the latest sorting information from `ShelfPreferences`.
- Add `shelfPreferences` property to `FavouriteScreenViewModel`.

### 2. Logic Implementation

#### [MODIFY] [FavouriteScreenViewModel.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/favourite/FavouriteScreenViewModel.kt)
- Use `snapshotFlow` to observe changes in `shelfPreferences.seriesSortMethod` and `shelfPreferences.seriesSortAscending`.
- Update `uiState` to react to these changes and fetch grouped favorites from `booksRepository`.
- Update `updateSortMethod` and `toggleSortAscending` to call the corresponding methods in `shelfPreferences`.

### 3. Cleanup

- Remove direct usage of `userPreferencesRepository.seriesSortPreferencesFlow` for writing/reading sorting preferences in this ViewModel, as `ShelfPreferences` now handles this.

## Verification Plan

### Manual Verification
- Navigate to the Favourite Screen.
- Change the sorting method or order via the Top Bar.
- Verify that the favorites are re-sorted immediately.
- Navigate to the Shelf Screen and verify that the sorting preference is synchronized (since they share the same DataStore keys via `ShelfPreferences`).
