# Android 阅读器原生重排文档连续滚动阅读模式设计方案

本方案旨在为基于 MuPDF 的 Android 阅读器项目引入针对重排文档（TXT, EPUB, HTML）的连续滚动阅读模式（Continuous Scrolling）。该模式将摆脱当前“单页翻页”的限制，提供类似 Moon+ Reader 的流畅纵向阅读体验。

## 用户调研与市面方案参考

调研了 Moon+ Reader、ReadEra、Lithium 和 Apple Books 等主流阅读器后，总结其实现重排滚动阅读的共性：
- **逻辑分块**：重排文档（如 EPUB）通常按“章”或“屏”进行逻辑切分。
- **无感连接**：在滚动模式下隐藏或弱化页面边界，通过消除页边距使内容看起来像一条长河。
- **虚拟化加载**：使用类似 `RecyclerView` 或 `LazyColumn` 的机制，仅渲染视口附近的内存内容，保证长文档性能。
- **位置锚定**：在字体大小或窗口尺寸变化触发重排时，能够精准锚定当前阅读位置，避免跳页。

## 技术决策与对比

针对本项目使用的 MuPDF 引擎，对比以下三种实现路径：

| 方案 | 描述 | 优点 | 缺点 | 结论 |
| :--- | :--- | :--- | :--- | :--- |
| **方案 A：基于逻辑页的 LazyColumn 堆叠** | 使用 `mCore.layout` 按屏幕高度分页，用 `LazyColumn` 垂直排列这些页面。 | **低风险**：完美契合 MuPDF 分页模型，复用现有搜索、目录和进度逻辑。 | 文档内强制分页（如 `page-break`）可能导致空隙。 | **首选方案** |
| **方案 B：超高单页重排渲染** | 调用 `mCore.layout` 时传一个极大的高度（如 100w 像素），将整章或整本作为一页渲染。 | 真正意义上的零间隔。 | **性能风险**：内存占用高，MuPDF 渲染超大位图可能崩溃；破坏现有百分比进度条逻辑。 | 放弃 |
| **方案 C：WebView 直绘** | 绕过 MuPDF，直接用 Android WebView 渲染 EPUB/HTML。 | 渲染效果最标准，原生支持滚动。 | **架构冲突**：需重写搜索、高亮、选文等所有 MuPDF 相关功能，维护成本极大。 | 放弃 |

## 建议落地方案：逻辑页虚拟化滚动

### 1. 阅读模式扩展
在 `ReadingMode.kt` 中实现 `ScrollableLayout` 组件。
- **核心组件**：使用 `androidx.compose.foundation.lazy.LazyColumn`。
- **项渲染**：每一项为一个逻辑页，复用 `MuPDFPage` 组件。
- **样式调整**：当进入滚动模式时，通过 CSS 注入或 Compose 修饰符移除 `MuPDFPage` 顶部的固定占位，使页面紧密衔接。

### 2. 状态同步与交互
- **位置追踪**：监听 `LazyColumn` 的 `firstVisibleItemIndex`，将其同步至 `ReadingScreenViewModel` 的 `currentPage`。
- **重排锚定**：当用户修改字号触发 `performLayout` 后，`mCore.layout` 会返回新的 `currentPage`。此时 `LazyColumn` 应通过 `scrollToItem` 跳转到新页码，确保阅读连续性。
- **缩放处理**：滚动模式下，捏合缩放通常应优先调整“全局字号”（Reflow）而非“视图缩放”（Matrix Transform）。

### 3. 代码结构调整预览

#### [MODIFY] [ReadingMode.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/display/ReadingMode.kt)
- 在 `ReadingModeContainer` 中接入 `ReadingMode.Scroll` 分支。
- 新增 `ScrollableLayout` 私有组件。

#### [MODIFY] [ReadingScreenViewModel.kt](file:///D:/Luo/Developer/Android/MyProject/Reader/app/src/main/java/io/lin/reader/ui/feature/reading/ReadingScreenViewModel.kt)
- 优化 `performLayout` 后的状态回流，确保 UI 层能感知重排完成并调整滚动位置。

## 验证计划

### 自动化测试
- 模拟切换阅读模式至 `Scroll`，校验 `LazyColumn` 是否正确渲染。
- 模拟滚动 `LazyColumn`，校验 `uiState.currentPage` 是否随之更新。
- 在 `Scroll` 模式下触发字号改变，校验 `performLayout` 后是否能维持在当前阅读内容附近。

### 手动验证
- 打开一个大型 TXT 或 EPUB 文件，检查滚动是否掉帧。
- 观察章节过渡处是否存在异常空白。
- 验证搜索高亮和链接点击在滚动模式下是否依然精准。
