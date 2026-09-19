# Implementation Plan - Fix `ReadingTransform` Instance Mismatch

The goal is to ensure that both the gesture detection layer and the content rendering layer share the same `ReadingTransform` instance by correctly positioning the `CompositionLocalProvider`.

## Proposed Changes

### [UI Components - Reading]

#### [MODIFY] [BookReader.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/BookReader.kt)

- **Move Provider**: Move the `CompositionLocalProvider(LocalReadingTransform provides readingTransform)` to encompass the entire reading layout.
- **Cleanup**: Remove the inner, redundant `CompositionLocalProvider` that was previously placed deep inside the gesture layer.

```kotlin
// Desired structure
CompositionLocalProvider(LocalReadingTransform provides readingTransform) {
    Box(modifier = ...) {
        GestureInteractionLayer(...) {
            ReadingModeContainer(...)
        }
    }
}
```

## Verification Plan

### Manual Verification
1.  **Pinch-to-zoom**: Perform a zoom gesture. The page should scale in real-time.
2.  **Double Tap**: Double tap the screen. The page should animate to 2.5x scale and pan to the tapped location.
3.  **Visual Check**: Verify that `graphicsLayer` in `ReadingMode.kt` is finally receiving and applying the `scaleAnim.value` from the correct instance.
