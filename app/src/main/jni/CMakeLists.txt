# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# 设置CMAKE的版本号
cmake_minimum_required(VERSION 3.4.1)

# 导出编译生成的so库
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ../../../../src/main/jniLibs/${ANDROID_ABI})

# 1.判断编译器类型,如果是gcc编译器,则在编译选项中加入c++11支持
if (CMAKE_COMPILER_IS_GNUCXX)
    set(CMAKE_CXX_FLAGS "-std=c++11 ${CMAKE_CXX_FLAGS}")
    message(STATUS "optional:-std=c++11")
endif (CMAKE_COMPILER_IS_GNUCXX)

# 2.配置加载native依赖，引入OpenCV头文件（include文件夹的地址,需要引入的头文件）
include_directories(${CMAKE_SOURCE_DIR}/include)

# 定义添加第三方so的路径 path_to_so
set(path_to_so "../../../../libs")

# 导入opencv的动态库（参数1：库名称 参数2：动态链接库类型 参数3：导入到库）
add_library(libopencv_java4 SHARED IMPORTED)
# 添加libopencv_java4库的名称、目标位置
set_target_properties(libopencv_java4 PROPERTIES IMPORTED_LOCATION
        ${path_to_so}/${ANDROID_ABI}/libopencv_java4.so)

#定义全局 source_path 变量
file(GLOB source_path ${CMAKE_SOURCE_DIR}/*.h)

add_library( # Sets the name of the library.
        #  //这里要写上我们要生成的so文件名称，要与MainActivity.java里的名称对应
        native

        SHARED

        # 这里要写上我们放在项目里的c/c++文件
        native.cpp
        ${source_path})

target_link_libraries( # Specifies the target library.
        # //这里要写上我们要生成的so文件名称，要与上面的名称对应
        native

        # 需要导入链接的库
        libopencv_java4
        jnigraphics

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
