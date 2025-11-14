package com.yourname.edgedetection;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/**
 * Processes camera frames using OpenCV and sends to WebSocket server
 * This class works with CameraX ImageAnalysis
 */
public class CameraFrameProcessor implements ImageAnalysis.Analyzer {
    
    private static final String TAG = "FrameProcessor";
    
    private MainActivity mainActivity;
    private boolean applyEdgeDetection = true;
    private int frameCount = 0;
    private int totalFrameCount = 0;
    private long lastFpsTime = System.currentTimeMillis();
    private double currentFps = 0;
    
    public CameraFrameProcessor(MainActivity activity) {
        this.mainActivity = activity;
        NativeProcessor.initProcessor();
    }
    
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            // Convert ImageProxy to Bitmap
            Bitmap inputBitmap = imageProxyToBitmap(imageProxy);
            
            if (inputBitmap != null) {
                // Create output bitmap
                Bitmap processedBitmap = Bitmap.createBitmap(
                    inputBitmap.getWidth(),
                    inputBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888
                );
                
                // Process with OpenCV via JNI
                NativeProcessor.processFrame(inputBitmap, processedBitmap, applyEdgeDetection);
                
                // Calculate FPS
                frameCount++;
                totalFrameCount++;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFpsTime >= 1000) {
                    currentFps = frameCount * 1000.0 / (currentTime - lastFpsTime);
                    frameCount = 0;
                    lastFpsTime = currentTime;
                    
                    // Update FPS display on UI
                    mainActivity.runOnUiThread(() -> 
                        mainActivity.updateFpsDisplay(currentFps)
                    );
                }
                
                // Get processing time from native code
                double processingTime = NativeProcessor.getProcessingTime();
                
                // ⭐ SEND TO WEB VIEWER ⭐
                mainActivity.sendFrameToWebViewer(processedBitmap);
                
                // Send stats every 30 frames to reduce overhead
                if (totalFrameCount % 30 == 0) {
                    mainActivity.sendStatsToWebViewer(
                        processedBitmap.getWidth(),
                        processedBitmap.getHeight(),
                        currentFps,
                        processingTime,
                        totalFrameCount
                    );
                }
                
                // Update OpenGL texture (if you have GLRenderer)
                // mainActivity.updateGLTexture(processedBitmap);
                
                // Clean up
                inputBitmap.recycle();
                
            } else {
                Log.w(TAG, "Failed to convert ImageProxy to Bitmap");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        } finally {
            // CRITICAL: Always close the imageProxy
            imageProxy.close();
        }
    }
    
    /**
     * Convert ImageProxy to Bitmap (YUV to RGB)
     */
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        
        byte[] nv21 = new byte[ySize + uSize + vSize];
        
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        
        // Convert NV21 to Bitmap using Android's YuvImage
        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            imageProxy.getWidth(),
            imageProxy.getHeight(),
            null
        );
        
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuvImage.compressToJpeg(
            new android.graphics.Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
            100,
            out
        );
        
        byte[] imageBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    
    /**
     * Toggle between edge detection and raw camera feed
     */
    public void toggleEdgeDetection() {
        applyEdgeDetection = !applyEdgeDetection;

    }
    
    /**
     * Check if edge detection is enabled
     */
    public boolean isEdgeDetectionEnabled() {
        return applyEdgeDetection;
    }
    
    /**
     * Clean up resources
     */
    public void release() {
        NativeProcessor.releaseProcessor();
    }
}