cmake_minimum_required(VERSION 3.6.0)
project(MEDIA_ENGINE)

# 要求cmake查找自定义的目录，而不是单单SDK的目录
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE BOTH)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE BOTH)

add_library(mediaengine
            SHARED
            src/main/cpp/utils.cpp)

# 指定ffmpeg所在的目录，包括include目录和lib目录
set(FFMPEG_PATH ${MEDIA_ENGINE_SOURCE_DIR}/src/main/external/ffmpeg/${CMAKE_ANDROID_ARCH_ABI})

find_library(LOGLIB log)

include_directories(
        ${FFMPEG_PATH}/include
)

file(GLOB FFMPEG_DENPENDIES ${MEDIA_ENGINE_SOURCE_DIR}/src/main/external/libs/${CMAKE_ANDROID_ARCH_ABI}/*.a)
target_link_libraries(mediaengine
        ${LOGLIB}
        ${FFMPEG_PATH}/lib
        ${FFMPEG_DENPENDIES}
)
