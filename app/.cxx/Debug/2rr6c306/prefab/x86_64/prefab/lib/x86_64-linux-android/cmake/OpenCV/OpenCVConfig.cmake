if(NOT TARGET OpenCV::opencv_java5)
add_library(OpenCV::opencv_java5 SHARED IMPORTED)
set_target_properties(OpenCV::opencv_java5 PROPERTIES
    IMPORTED_LOCATION "D:/Luo/Developer/JDK/caches/8.13/transforms/d7402da05451affc6a5651be0acde313/transformed/opencv-5.0.0.1/prefab/modules/opencv_java5/libs/android.x86_64/libopencv_java5.so"
    INTERFACE_INCLUDE_DIRECTORIES "D:/Luo/Developer/JDK/caches/8.13/transforms/d7402da05451affc6a5651be0acde313/transformed/opencv-5.0.0.1/prefab/modules/opencv_java5/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

