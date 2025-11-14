#ifndef EDGEDETECTION_EDGEPROCESSOR_H
#define EDGEDETECTION_EDGEPROCESSOR_H

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <chrono>

/**
 * EdgeProcessor - Handles OpenCV image processing operations
 * Performs Canny edge detection and grayscale conversion
 */
class EdgeProcessor {
public:
    EdgeProcessor();
    ~EdgeProcessor();
    
    /**
     * Process frame with Canny edge detection
     * @param inputFrame Input image in RGBA format
     * @return Processed image with edges in RGBA format
     */
    cv::Mat processFrame(const cv::Mat& inputFrame);
    
    /**
     * Convert to grayscale
     * @param inputFrame Input image
     * @return Grayscale image in RGBA format for OpenGL compatibility
     */
    cv::Mat toGrayscale(const cv::Mat& inputFrame);
    
    /**
     * Get processing time of last operation in milliseconds
     * @return Processing time in ms
     */
    double getProcessingTime() const;
    
    /**
     * Set Canny edge detection thresholds
     * @param threshold1 First threshold for hysteresis (default: 50)
     * @param threshold2 Second threshold for hysteresis (default: 150)
     */
    void setCannyThresholds(double threshold1, double threshold2);
    
private:
    double processingTime;
    
    // Canny parameters (adjustable for different edge sensitivity)
    double cannyThreshold1;
    double cannyThreshold2;
    const int cannyApertureSize = 3;
    
    // Gaussian blur parameters
    const int blurKernelSize = 5;
    const double blurSigma = 1.5;
};

#endif //EDGEDETECTION_EDGEPROCESSOR_H