if(NOT TARGET OpenCV::opencv_java5)
add_library(OpenCV::opencv_java5 SHARED IMPORTED)
set_target_properties(OpenCV::opencv_java5 PROPERTIES
    IMPORTED_LOCATION "D:/Luo/Developer/JDK/caches/8.14.5/transforms/bd6855742c3059a6d85eede64f673b51/transformed/opencv-5.0.0.1/prefab/modules/opencv_java5/libs/android.arm64-v8a/libopencv_java5.so"
    INTERFACE_INCLUDE_DIRECTORIES "D:/Luo/Developer/JDK/caches/8.14.5/transforms/bd6855742c3059a6d85eede64f673b51/transformed/opencv-5.0.0.1/prefab/modules/opencv_java5/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

