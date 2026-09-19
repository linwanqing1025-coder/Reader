# 项目插件版本升级计划 (2026-07)

我们将把项目的基础构建插件升级到当前的最新稳定版本，以获得更好的编译性能、Compose 优化以及对新版本 Android SDK (API 36/37) 的更佳支持。

## 用户评审确认

> [!IMPORTANT]
> **KSP 版本绑定**：KSP 插件版本必须与 Kotlin 版本精确匹配。本次升级将两者统一提升至 `2.4.10` 系列。
> **Gradle Sync**：升级后会触发一次完整的 Gradle 同步，可能需要几分钟时间下载新版插件。

## 拟议变更

### 根目录配置

#### [MODIFY] [build.gradle.kts](file:///D:/Luo/Developer/Android/MyProject/Reader/build.gradle.kts)
- 将 `com.android.application` 和 `com.android.library` 版本从 `8.8.0` 升级到 `9.3.0`。
- 将 `org.jetbrains.kotlin.android` 和 `org.jetbrains.kotlin.plugin.compose` 版本从 `2.1.0` 升级到 `2.4.10`。

---

### App 模块配置

#### [MODIFY] [app/build.gradle.kts](file:///D:/Luo/Developer/Android/MyProject/Reader/app/build.gradle.kts)
- 将 `com.google.devtools.ksp` 插件版本从 `2.1.0-1.0.29` 升级到 `2.4.10-1.0.31`。

## 验证计划

### 自动化验证
- 执行 `gradle_sync` 确保依赖解析正常。
- 执行 `gradle_build(commandLine = "help")` 验证基本构建逻辑。

### 手动验证
- 检查 `MainActivity.kt` 等核心文件的代码高亮和跳转是否依然正常（确保 IDE 索引已更新）。
