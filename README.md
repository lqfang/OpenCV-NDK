# OpenCV-NDK

简介：

1.引入opencv_java4.so,在CMake中配置，可以生成自定义的so库；

2.通过引入封装的人脸检测库facesdk-release，实现图片的人脸检测框(FaceDetector)和关键点(FaceLandmark)

3.提供相机预览的人脸检测框和关键点等功能（首页长按进入）

环境准备：

Android Studio 3.5

Cmake:3.10.2

Android SDK:28.0.3

NDK: r16b

project下的：

Android Gradle Plugin Vsersion： 3.2.1

Gradle Version： 4.6

依赖库：

opencv_jave4(若只需要opencv的几个功能也可以根据需求导入相关的静态库)
