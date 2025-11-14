#include "EdgeProcessor.h"
#include <android/log.h>

#define LOG_TAG "EdgeProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

EdgeProcessor::EdgeProcessor() 
    : processingTime(0.0),
      cannyThreshold1(50.0),
      cannyThreshold2(150.0) {
    LOGD("EdgeProcessor initialized");
}

EdgeProcessor::~EdgeProcessor() {
    LOGD("EdgeProcessor destroyed");
}

cv::Mat EdgeProcessor::processFrame(const cv::Mat& inputFrame) {
    auto start = std::chrono::high_resolution_clock::now();
    
    if (inputFrame.empty()) {
        LOGE("Input frame is empty!");
        return cv::Mat();
    }
    
    cv::Mat gray;
    cv::Mat edges;
    cv::Mat output;
    
    try {
        // Convert to grayscale
        if (inputFrame.channels() == 4) {
            cv::cvtColor(inputFrame, gray, cv::COLOR_RGBA2GRAY);
        } else if (inputFrame.channels() == 3) {
            cv::cvtColor(inputFrame, gray, cv::COLOR_RGB2GRAY);
        } else if (inputFrame.channels() == 1) {
            gray = inputFrame.clone();
        } else {
            LOGE("Unsupported number of channels: %d", inputFrame.channels());
            return cv::Mat();
        }
        
        // Apply Gaussian blur to reduce noise and improve edge detection
        cv::GaussianBlur(gray, gray, cv::Size(blurKernelSize, blurKernelSize), blurSigma);
        
        // Apply Canny edge detection
        cv::Canny(gray, edges, cannyThreshold1, cannyThreshold2, cannyApertureSize);
        
        // Convert back to RGBA for OpenGL texture compatibility
        cv::cvtColor(edges, output, cv::COLOR_GRAY2RGBA);
        
    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception: %s", e.what());
        return cv::Mat();
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    processingTime = std::chrono::duration<double, std::milli>(end - start).count();
    
    LOGD("Frame processed in %.2f ms", processingTime);
    
    return output;
}

cv::Mat EdgeProcessor::toGrayscale(const cv::Mat& inputFrame) {
    if (inputFrame.empty()) {
        LOGE("Input frame is empty!");
        return cv::Mat();
    }
    
    cv::Mat gray;
    cv::Mat output;
    
    try {
        // Convert to grayscale
        if (inputFrame.channels() == 4) {
            cv::cvtColor(inputFrame, gray, cv::COLOR_RGBA2GRAY);
        } else if (inputFrame.channels() == 3) {
            cv::cvtColor(inputFrame, gray, cv::COLOR_RGB2GRAY);
        } else if (inputFrame.channels() == 1) {
            gray = inputFrame.clone();
        } else {
            LOGE("Unsupported number of channels: %d", inputFrame.channels());
            return cv::Mat();
        }
        
        // Convert back to RGBA for OpenGL compatibility
        cv::cvtColor(gray, output, cv::COLOR_GRAY2RGBA);
        
    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception in toGrayscale: %s", e.what());
        return cv::Mat();
    }
    
    return output;
}

double EdgeProcessor::getProcessingTime() const {
    return processingTime;
}

void EdgeProcessor::setCannyThresholds(double threshold1, double threshold2) {
    cannyThreshold1 = threshold1;
    cannyThreshold2 = threshold2;
    LOGD("Canny thresholds updated: %.1f, %.1f", threshold1, threshold2);
}