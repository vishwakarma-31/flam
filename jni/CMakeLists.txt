cmake_minimum_required(VERSION 3.22.1)

project("edgedetection")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Set OpenCV directory path
# Adjust this path based on where you placed OpenCV SDK
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs/OpenCV/sdk/native/jni)
find_package(OpenCV REQUIRED)
# Find OpenCV package
find_package(OpenCV REQUIRED)

# Log OpenCV information
message(STATUS "OpenCV library status:")
message(STATUS "    version: ${OpenCV_VERSION}")
message(STATUS "    libraries: ${OpenCV_LIBS}")
message(STATUS "    include path: ${OpenCV_INCLUDE_DIRS}")

# Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# Add source files
add_library(
    edgedetection
    SHARED
    native-lib.cpp
    EdgeProcessor.cpp
)

# Link libraries
target_link_libraries(
    edgedetection
    ${OpenCV_LIBS}
    android
    log
    jnigraphics
    GLESv2
    EGL
)

# Compiler flags
target_compile_options(
    edgedetection
    PRIVATE
    -Wall
    -Wextra
    -O3
    -frtti
    -fexceptions
)