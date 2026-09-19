cmake_minimum_required(VERSION 3.22.1)

project("reader_native")

# 1. 强制要求 C++17，这对 OpenCV 5.0 和高性能算法非常重要
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 2. 通过 Prefab 寻找 OpenCV
# 注意：opencv_java5 是 OpenCV 5.x AAR 默认导出的 Prefab 组件名
find_package(OpenCV REQUIRED COMPONENTS opencv_java5)

# 3. 定义你的 JNI 库
add_library(reader_opencv SHARED reader_jni.cpp)

# 4. 针对 ARM 架构开启 NEON 指令集加速 (图像处理必选)
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(reader_opencv PRIVATE -mfpu=neon)
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    # arm64-v8a 默认支持 NEON，但可以显式开启一些优化
    target_compile_options(reader_opencv PRIVATE -O3)
endif()

# 5. 链接库
# OpenCV::opencv_java5: 核心图像处理
# jnigraphics: 必须！用于操作 Android Bitmap 像素 (AndroidBitmap_lockPixels)
# log: 用于 C++ 层的日志输出 (TAG "reader_jni")
target_link_libraries(reader_opencv
    OpenCV::opencv_java5
    jnigraphics
    log
)
