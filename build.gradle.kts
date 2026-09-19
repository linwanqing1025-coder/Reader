// Top-level build file where you can add configuration options common to all subprojects/modules.
buildscript {
    extra.apply {

    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
    id("com.android.library") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}

tasks.register("clean", Delete::class) {
    description = ""
    delete(rootProject.layout.buildDirectory)
}
tasks.register("syncMaterial3Samples", Exec::class) {
    group = "help"
    description = "Synchronizes Material 3 samples from official GitHub repository."

    // 直接设置命令行，Exec 类型的任务会自动在执行阶段运行它
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "./sync_samples.ps1")
}