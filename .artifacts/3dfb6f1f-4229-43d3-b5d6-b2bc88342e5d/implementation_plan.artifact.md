# Refactor FileUtils and PdfUtils to support MuPDF and multiple document formats

Refactor `FileUtils.kt` and `PdfUtils.kt` to use `MuPDFCore` for document operations (page count, cover generation) instead of `PdfRenderer`, enabling support for PDF, EPUB, XPS, CBZ, and other MuPDF-supported formats.

## Proposed Changes

### [Utils Component]

#### [MODIFY] [FileUtils.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/utils/FileUtils.kt)
- Add `getMimeType(context, uri)` to help identify document types.
- Update `getFilePageCount` and `getCover` to delegate to the new `DocumentUtils`.

#### [NEW] [DocumentUtils.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/utils/DocumentUtils.kt)
- Rename from `PdfUtils.kt`.
- Use `MuPDFCore` to handle multiple document formats.
- Implement `openCore` helper to safely initialize `MuPDFCore` from a `Uri` (handling both memory buffer and stream).
- Implement `getPageCount` using `MuPDFCore.countPages()`.
- Implement `getCover` using `MuPDFCore.drawPage()` to render the first page into a Bitmap.

#### [DELETE] [PdfUtils.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/utils/PdfUtils.kt)
- Replaced by `DocumentUtils.kt`.

## Verification Plan

### Automated Tests
- Run existing unit tests if any.
- Since this involves hardware/native library interaction, manual verification on a device/emulator is preferred.

### Manual Verification
- Verify that PDFs still show correct page counts and covers.
- Verify that other formats (e.g., EPUB, XPS) now show page counts and covers.
- Check for memory leaks by ensuring `MuPDFCore.onDestroy()` is called.
