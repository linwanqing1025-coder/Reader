# 解决 Gradle Sync 报错

## 问题分析
1. **主要报错**：`foojay-resolver-convention` 插件找不到。
2. **潜在根源**：从 Sync 日志发现 Gradle 正在尝试通过 `127.0.0.1:7890` 代理连接网络，但连接被拒绝（Connection refused）。这导致 Gradle 无法访问任何仓库（包括阿里云镜像）。
3. **插件问题**：由于网络不通，插件版本检索失败。

## 解决方案
我们将通过以下步骤修复：
1. **禁用代理配置**：在项目 `gradle.properties` 中显式清空代理设置，确保 Gradle 直接连接阿里云镜像源。
2. **恢复并优化插件配置**：恢复 `foojay-resolver-convention` 插件（版本设为稳定的 `0.8.0`），并确保仓库配置包含 `public` 和 `mavenCentral`。

## 拟修改文件

### [gradle.properties](file:///D:/Luo/Developer/Android/MyProject/Reader/gradle.properties)
- 添加禁用代理的系统属性（防止全局配置干扰）。

### [settings.gradle.kts](file:///D:/Luo/Developer/Android/MyProject/Reader/settings.gradle.kts)
- 恢复插件配置并使用 `0.8.0` 版本。
- 确保 `pluginManagement` 仓库配置完整。

## 验证计划
1. 应用更改后执行 Gradle Sync。
2. 检查下载日志是否不再尝试连接 `7890` 端口。
