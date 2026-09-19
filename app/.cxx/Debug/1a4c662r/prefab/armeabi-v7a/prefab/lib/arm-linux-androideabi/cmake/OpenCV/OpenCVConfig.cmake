if(NOT TARGET OpenCV::opencv_java5)
add_library(OpenCV::opencv_java5 SHARED IMPORTED)
set_target_properties(OpenCV::opencv_java5 PROPERTIES
    IMPORTED_LOCATION "D:/Luo/Developer/JDK/caches/9.7.0/transforms/7ec220c9e420fc5271ef60e6d124b341/transformed/opencv-5.0.0.1/prefab/modules/opencv_java5/libs/android.armeabi-v7a/libopencv_java5.so"
    INTERFACE_INCLUDE_DIRECTORIES "D:/Luo/Developer/JDK/caches/9.7.0/transforms/7ec220c9e420fc5271ef60e6d124b341/transformed/opencv-5.0.0.1/prefab/modules/opencv_java5/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

