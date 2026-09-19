#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>

// 定义 Log 标签，方便在 Logcat 查看输出
#define LOG_TAG "ReaderOpenCV"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_io_lin_reader_utils_ImageProcessor_nativeCalculateCropRatios(
        JNIEnv *env,
        jobject thiz,
        jobject bitmap,
        jdouble sensitivity) {

    // 1. 获取 Bitmap 信息
    AndroidBitmapInfo info;
    void *pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return nullptr;

    // 锁定 Bitmap 像素，使其不被 GC 回收
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return nullptr;

    // 2. 将 Bitmap 转换为 OpenCV 的 Mat
    // Android Bitmap 通常是 RGBA_8888 格式
    cv::Mat src(info.height, info.width, CV_8UC4, pixels);

    // 3. 图像预处理 (与你原来的 Kotlin 逻辑一致)
    cv::Mat gray, thresh;
    // 色彩转换：RGBA 转 灰度
    cv::cvtColor(src, gray, cv::COLOR_RGBA2GRAY);

    // 二值化：使用 OTSU 算法自动寻找阈值
    // THRESH_BINARY_INV 会将文字变白(255)，背景变黑(0)
    cv::threshold(gray, thresh, 0, 255, cv::THRESH_BINARY_INV | cv::THRESH_OTSU);

    // 4. 核心步骤：投影分析 (Projection)
    // 统计每一行和每一列的像素总和
    cv::Mat colSum, rowSum;
    cv::reduce(thresh, colSum, 0, cv::REDUCE_SUM, CV_32S);
    cv::reduce(thresh, rowSum, 1, cv::REDUCE_SUM, CV_32S);

    // 5. 计算边界阈值
    int w = info.width;
    int h = info.height;
    int colThreshold = (int)(h * 255 * sensitivity);
    int rowThreshold = (int)(w * 255 * sensitivity);

    int left = 0, right = w - 1;
    int top = 0, bottom = h - 1;

    // 从四个方向扫描内容边界
    for (int x = 0; x < w; x++) {
        if (colSum.at<int>(0, x) > colThreshold) { left = x; break; }
    }
    for (int x = w - 1; x >= left; x--) {
        if (colSum.at<int>(0, x) > colThreshold) { right = x; break; }
    }
    for (int y = 0; y < h; y++) {
        if (rowSum.at<int>(y, 0) > rowThreshold) { top = y; break; }
    }
    for (int y = h - 1; y >= top; y--) {
        if (rowSum.at<int>(y, 0) > rowThreshold) { bottom = y; break; }
    }

    // 处理完后必须解锁像素
    AndroidBitmap_unlockPixels(env, bitmap);

    // 6. 返回结果给 Kotlin (四个边的裁切比例)
    jfloatArray result = env->NewFloatArray(4);
    float ratios[4];
    ratios[0] = (float)left / w;               // leftRatio
    ratios[1] = (float)(w - 1 - right) / w;    // rightRatio
    ratios[2] = (float)top / h;                // topRatio
    ratios[3] = (float)(h - 1 - bottom) / h;   // bottomRatio

    env->SetFloatArrayRegion(result, 0, 4, ratios);
    return result;
}
