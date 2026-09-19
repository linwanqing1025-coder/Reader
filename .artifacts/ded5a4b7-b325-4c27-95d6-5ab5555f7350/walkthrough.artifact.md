# GestureInteractionLayer 重构与功能瘦身实现

我们已经成功完成了 `GestureInteractionLayer` 的重构。通过引入环境注入和职责剥离，该组件现在的逻辑更加纯粹，代码量显著减少。

## 主要变更点

### 1. 职责解耦
- **[DELETE]** 彻底移除了对“点击链接”和“文字选择”的处理逻辑。这些职责现在由下层的 `MuPDFPage` 组件通过 `StructuredText` 接口独立完成，避免了手势层级冲突。
- **[REFINED]** 移除了 `onPreviousClick`, `onNextClick`, `onInternalJump` 等回调参数。组件现在直接通过 `LocalReadingViewModel` 触发翻页动作。

### 2. 环境化驱动 (Environmentalization)
- **[MODIFY]** 该组件现在直接从 `LocalReadingTransform`, `LocalReaderPreferences` 和 `LocalReadingViewModel` 中获取所需状态，不再依赖 BookReader 的长参数链。
- **[NEW]** 在 `ReadingEnvironment.kt` 中补全了 `LocalReaderPreferences` 和 `LocalReflowPreferences` 的接口传送门。

### 3. 核心手势逻辑保留
- **翻页交互**：保留了基于点击区域（左/中/右）的智能翻页逻辑，并支持缩放状态下的“边缘自动翻页”。
- **缩放与位移**：保留了双指捏合缩放（Pinch-to-zoom）、双击缩放/恢复以及缩放后的流畅平移。
- **重排模式缩放**：恢复了在重排（Reflow）模式下通过捏合手势实时调整字体大小的功能。

## 验证结果

> [!TIP]
> **响应速度**：由于减少了手势层级的预计算（如链接检测），基础点击翻页的响应变得更加即时。
> [!IMPORTANT]
> **自治性**：`GestureInteractionLayer` 现在是一个真正的“自治状态容器”，只要在有 ViewModel 的环境里就能独立工作，极大简化了 `BookReader` 的维护工作。

render_diffs(file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/BookReader.kt)
render_diffs(file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/ReadingEnvironment.kt)
