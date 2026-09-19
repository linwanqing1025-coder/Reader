# Implementation Plan - Implement Page Rotation in MuPDFPage

This plan details how to implement page rotation in `MuPDFPage` using Compose `Modifier.graphicsLayer`, ensuring that all internal elements (highlights, handles, etc.) rotate correctly with the page while maintaining consistent logic coordinates.

## Proposed Changes

### Reading Feature

#### [MODIFY] [MuPDFPage.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/display/MuPDFPage.kt)
1.  **Add `rotation: Int = 0` Parameter**: Update `MuPDFPage` signature.
2.  **Nested Layout for Rotation**:
    -   Introduce an outer `Box` that handles the **layout aspect ratio** (swapped if rotation is 90 or 270 degrees).
    -   Inside, use `BoxWithConstraints` for the **content surface**.
    -   Calculate `contentWidth` and `contentHeight` based on whether the page is rotated. If rotated 90/270, the content's width matches the container's height and vice versa.
    -   Apply `graphicsLayer { rotationZ = rotation.toFloat() }` to this content surface.
3.  **Coordinate Consistency**: Since the `BoxWithConstraints` is rotated as a whole, all internal logic (touch handling, Canvas drawing, Selection Handles) will operate in the "unrotated" 0-degree coordinate system of the PDF page, and the GPU will handle the visual rotation.

#### [MODIFY] [ReadingMode.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/display/ReadingMode.kt)
1.  **Extract Rotation**: In `SinglePageLayout`'s `pageContent` lambda, extract `rotation` from `uiState.pageSettings[pageIndex]`.
2.  **Pass to MuPDFPage**: Pass the rotation value to the `MuPDFPage` component.
3.  **Update contentSize**: Ensure the `onSizeChanged` reported to `readingTransform.contentSize` is attached to the **outer** (rotated) container, so gesture limits in `BookReader.kt` match the visual page dimensions.

## Verification Plan

### Manual Verification
- Open a PDF and perform page rotation (via UI controls).
- Verify the page rotates correctly and fits within the Pager.
- Verify that search highlights and text selection handles rotate with the page and remain correctly positioned relative to the text.
- Verify that touch interactions (tap to link, drag to select) work correctly on rotated pages.
