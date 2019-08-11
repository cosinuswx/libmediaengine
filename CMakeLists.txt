cmake_minimum_required(VERSION 3.4.1)
project(MEDIA_ENGINE)

# 要求cmake查找自定义的目录，而不是单单SDK的目录
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE BOTH)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE BOTH)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${MEDIA_ENGINE_SOURCE_DIR}/cmake")

add_library(mediaengine
            SHARED
            src/main/cpp/utils.cpp)

# 指定ffmpeg所在的目录，包括include目录和lib目录
set(FFMPEG_PATH "${MEDIA_ENGINE_SOURCE_DIR}/src/main/external/ffmpeg/armeabi-v7a")
find_package(FFMPEG REQUIRED)

find_library(LOGLIB log)

include_directories(
        ${AVFORMAT_INCLUDE_DIRS}
        ${AVCODEC_INCLUDE_DIRS}
        ${AVUTIL_INCLUDE_DIRS}
        ${SWSCALE_INCLUDE_DIRS}
        ${AVFILTER_INCLUDE_DIRS}
        ${POSTPROC_INCLUDE_DIRS}
        ${SWRESAMPLE_INCLUDE_DIRS}
        ${AVDEVICE_INCLUDE_DIRS}
)

file(GLOB FFMPEG_DENPENDIES ${MEDIA_ENGINE_SOURCE_DIR}/src/main/external/libs/*.a)
MESSAGE(STATUS ${FFMPEG_DENPENDIES})
target_link_libraries(mediaengine
        ${LOGLIB}
        ${SWSCALE_LIBRARIES}
        ${AVFORMAT_LIBRARIES}
        ${AVCODEC_LIBRARIES}
        ${AVFILTER_LIBRARIES}
        ${POSTPROC_LIBRARIES}
        ${SWRESAMPLE_LIBRARIES}
        ${AVDEVICE_LIBRARIES}
        ${AVUTIL_LIBRARIES}
        ${FFMPEG_DENPENDIES}
)
