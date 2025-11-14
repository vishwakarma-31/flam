#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include "EdgeProcessor.h"

#define LOG_TAG "NativeLib"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Global processor instance
static EdgeProcessor* g_processor = nullptr;

/**
 * Helper function to convert Android Bitmap to OpenCV Mat
 */
bool bitmapToMat(JNIEnv* env, jobject bitmap, cv::Mat& mat) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    
    // Get bitmap info
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return false;
    }
    
    // Lock bitmap pixels
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return false;
    }
    
    // Create Mat from bitmap data
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        mat = cv::Mat(info.height, info.width, CV_8UC4, pixels);
    } else if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        mat = cv::Mat(info.height, info.width, CV_8UC2, pixels);
    } else {
        LOGE("Unsupported bitmap format: %d", info.format);
        AndroidBitmap_unlockPixels(env, bitmap);
        return false;
    }
    
    // Note: Don't unlock here, caller should unlock after processing
    return true;
}

/**
 * Helper function to convert OpenCV Mat to Android Bitmap
 */
bool matToBitmap(JNIEnv* env, const cv::Mat& mat, jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    
    // Get bitmap info
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("Failed to get bitmap info");
        return false;
    }
    
    // Lock bitmap pixels
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return false;
    }
    
    // Check dimensions match
    if (mat.rows != static_cast<int>(info.height) || mat.cols != static_cast<int>(info.width)) {
        LOGE("Mat dimensions don't match bitmap: Mat(%dx%d) vs Bitmap(%dx%d)", 
             mat.cols, mat.rows, info.width, info.height);
        AndroidBitmap_unlockPixels(env, bitmap);
        return false;
    }
    
    // Copy Mat data to bitmap
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat bitmapMat(info.height, info.width, CV_8UC4, pixels);
        if (mat.channels() == 4) {
            mat.copyTo(bitmapMat);
        } else {
            cv::cvtColor(mat, bitmapMat, cv::COLOR_RGB2RGBA);
        }
    } else {
        LOGE("Unsupported bitmap format for output: %d", info.format);
        AndroidBitmap_unlockPixels(env, bitmap);
        return false;
    }
    
    // Note: Don't unlock here, caller should unlock
    return true;
}

// ============================================================================
// JNI METHOD IMPLEMENTATIONS
// ============================================================================

extern "C" JNIEXPORT jstring JNICALL
Java_com_yourname_edgedetection_NativeProcessor_stringFromJNI(
        JNIEnv* env,
        jclass /* clazz */) {
    
    std::string version = "OpenCV " + cv::getVersionString();
    LOGI("OpenCV Version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_yourname_edgedetection_NativeProcessor_initProcessor(
        JNIEnv* env,
        jclass /* clazz */) {
    
    if (g_processor == nullptr) {
        g_processor = new EdgeProcessor();
        LOGI("EdgeProcessor initialized successfully");
    } else {

    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_yourname_edgedetection_NativeProcessor_releaseProcessor(
        JNIEnv* env,
        jclass /* clazz */) {
    
    if (g_processor != nullptr) {
        delete g_processor;
        g_processor = nullptr;
        LOGI("EdgeProcessor released");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_yourname_edgedetection_NativeProcessor_processFrame(
        JNIEnv* env,
        jclass /* clazz */,
        jobject bitmapIn,
        jobject bitmapOut,
        jboolean applyEdgeDetection) {
    
    if (g_processor == nullptr) {
        LOGE("Processor not initialized! Call initProcessor() first.");
        return;
    }
    
    cv::Mat inputMat;
    cv::Mat outputMat;
    
    // Convert input bitmap to Mat
    if (!bitmapToMat(env, bitmapIn, inputMat)) {
        LOGE("Failed to convert input bitmap to Mat");
        AndroidBitmap_unlockPixels(env, bitmapIn);
        return;
    }
    
    try {
        // Process frame based on mode
        if (applyEdgeDetection) {
            outputMat = g_processor->processFrame(inputMat);
        } else {
            // Just copy input to output (raw camera feed)
            outputMat = inputMat.clone();
        }
        
        // Check if processing was successful
        if (outputMat.empty()) {
            LOGE("Processing resulted in empty Mat");
            AndroidBitmap_unlockPixels(env, bitmapIn);
            return;
        }
        
        // Convert output Mat to bitmap
        if (!matToBitmap(env, outputMat, bitmapOut)) {
            LOGE("Failed to convert output Mat to bitmap");
        }
        
    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception: %s", e.what());
    } catch (const std::exception& e) {
        LOGE("Standard exception: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception occurred");
    }
    
    // Unlock bitmaps
    AndroidBitmap_unlockPixels(env, bitmapOut);
    AndroidBitmap_unlockPixels(env, bitmapIn);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_yourname_edgedetection_NativeProcessor_getProcessingTime(
        JNIEnv* env,
        jclass /* clazz */) {
    
    if (g_processor != nullptr) {
        return static_cast<jdouble>(g_processor->getProcessingTime());
    }
    
    return 0.0;
}

// ============================================================================
// OPTIONAL: Additional utility methods
// ============================================================================

extern "C" JNIEXPORT void JNICALL
Java_com_yourname_edgedetection_NativeProcessor_setCannyThresholds(
        JNIEnv* env,
        jclass /* clazz */,
        jdouble threshold1,
        jdouble threshold2) {
    
    if (g_processor != nullptr) {
        g_processor->setCannyThresholds(threshold1, threshold2);
        LOGI("Canny thresholds set: %.1f, %.1f", threshold1, threshold2);
    } else {
        LOGE("Processor not initialized");
    }
}

// JNI_OnLoad - Called when the native library is loaded
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("Native library loaded successfully");
    return JNI_VERSION_1_6;
}

// JNI_OnUnload - Called when the native library is unloaded
JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved) {
    if (g_processor != nullptr) {
        delete g_processor;
        g_processor = nullptr;
    }
    LOGI("Native library unloaded");
}