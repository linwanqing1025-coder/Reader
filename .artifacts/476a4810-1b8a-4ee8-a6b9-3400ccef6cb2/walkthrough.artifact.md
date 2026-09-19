# Walkthrough - Fix IndexOutOfBoundsException in `ReadingScreen`

I have fixed the crash that occurred when entering the `ReadingScreen` (especially when starting from the first page) by unifying the page indexing logic to be 0-based.

## Changes

### [UI Components - Reading]

#### [BookReader.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/BookReader.kt)

- **Standardized Indexing**: Changed `currentPageNumber` to consistently use 0-based indexing, matching the ViewModel's `uiState.currentPage`.
- **Fixed Pager Crash**: Removed the `- 1` offset when passing the index to `ReadingModeContainer`. This prevents passing `-1` to the Pager component, which was the direct cause of the `IndexOutOfBoundsException`.
- **Corrected Display Logic**:
    - In the **Add Bookmark** dialog, I now use `currentPageNumber + 1` to show the human-readable page number to the user.
    - In the **Delete Bookmark** confirmation notification, I also added `+ 1` for consistent display.
- **Maintained Internal Logic**: The calls to `viewModel.addBookmark` and `viewModel.deleteBookmark` continue to use the 0-based `currentPageNumber`, ensuring that bookmarks are stored at the correct positions in the database.

## Verification Results

### Automated Tests
- `analyze_file` passed with no errors.

### Manual Verification (Required)
- Please run the app and open any book. It should no longer crash on the first page.
- Verify that when you are on the first page, the bookmark dialog shows "Page 1" instead of "Page 0".
