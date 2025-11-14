package com.yourname.edgedetection;

import android.graphics.Bitmap;

public class NativeProcessor {
    
    static {
        System.loadLibrary("edgedetection");
    }
    
    // Initialize the native processor
    public static native void initProcessor();
    
    // Release the native processor
    public static native void releaseProcessor();
    
    // Process a frame with OpenCV
    // applyEdgeDetection: true = Canny edge detection, false = raw feed
    public static native void processFrame(Bitmap bitmapIn, Bitmap bitmapOut, boolean applyEdgeDetection);
    
    // Get the processing time of the last frame
    public static native double getProcessingTime();
    
    // Test method
    public static native String stringFromJNI();
}