@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

// 加载 local.properties 文件
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    compileSdk = 37

    defaultConfig {
        applicationId = "io.lin.reader"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    signingConfigs {
        getByName("debug") {
            // 如果需要 debug 版也分包测试，建议保持一致
        }
        create("release") {
            // 从 local.properties 中读取配置，如果读取不到则为空
            val path = localProperties.getProperty("signing.storeFile")
            if (path != null) {
                storeFile = file(path)
                storePassword = localProperties.getProperty("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.getProperty("signing.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // 使用上面定义的 release 签名配置
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ABI 分包
    splits {
        abi {
            isEnable = true
            // 重置列表
            reset()
            // 包含所有真机支持的架构，x86 可选包含（主要用于模拟器）
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            // 不生成包含所有架构的巨大通用包
            isUniversalApk = false
        }
    }
    // --- ABI 分包配置结束 ---

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        prefab = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt") // 指向你的 CMake 文件
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    namespace = "io.lin.reader"
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha28")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.appcompat:appcompat:1.8.0")

    //Icon
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    //navigation
    implementation("androidx.navigation:navigation-compose:2.10.1")

    //ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.xr.compose.material3:material3:1.0.0-alpha17")

    //Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    //Room
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")

    //Paging
    implementation("androidx.paging:paging-runtime:3.5.1")
    implementation("androidx.paging:paging-compose:3.5.1")

    //Room Paging Support
    implementation("androidx.room:room-paging:2.8.5")

    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    //Navigation 3
    implementation("androidx.navigation3:navigation3-runtime:1.1.7")
    implementation("androidx.navigation3:navigation3-ui:1.1.7")

    //MuPDF
    implementation("com.artifex.mupdf:viewer:1.28.0a")

    //OpenCV
    implementation("org.opencv:opencv:5.0.0.1")

    //Testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
