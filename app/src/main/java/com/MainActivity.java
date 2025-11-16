package com.yourname.edgedetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import gl.GLRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    // UI Components
    private PreviewView previewView;
    private GLSurfaceView glSurfaceView;
    private TextView serverStatusText;
    private TextView ipAddressText;
    private TextView fpsText;
    private Button startServerButton;
    private Button stopServerButton;
    private Button toggleEdgeButton;
    
    // WebSocket Server
    private EdgeDetectionWebSocketServer webSocketServer;
    
    // Camera
    private CameraFrameProcessor frameProcessor;
    private ExecutorService cameraExecutor;
    
    // OpenGL Renderer
    private GLRenderer glRenderer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        initializeUI();
        
        // Set up OpenGL renderer
        setupOpenGLRenderer();
        
        // Set up WebSocket server
        setupWebSocketServer();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Display device IP address
        displayIpAddress();
        
        // Check camera permission
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    private void initializeUI() {
        previewView = findViewById(R.id.previewView);
        glSurfaceView = findViewById(R.id.glSurfaceView);
        serverStatusText = findViewById(R.id.serverStatus);
        ipAddressText = findViewById(R.id.ipAddress);
        fpsText = findViewById(R.id.fpsText);
        startServerButton = findViewById(R.id.startServerButton);
        stopServerButton = findViewById(R.id.stopServerButton);
        toggleEdgeButton = findViewById(R.id.toggleEdgeButton);
        
        // Initial states
        stopServerButton.setEnabled(false);
        fpsText.setText("FPS: 0");
    }
    
    private void setupOpenGLRenderer() {
        glRenderer = new GLRenderer();
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
    
    private void setupWebSocketServer() {
        webSocketServer = new EdgeDetectionWebSocketServer(
            new EdgeDetectionWebSocketServer.ServerCallback() {
                @Override
                public void onServerStarted() {
                    runOnUiThread(() -> {
                        serverStatusText.setText("Server Status: Running");
                        startServerButton.setEnabled(false);
                        stopServerButton.setEnabled(true);
                        Toast.makeText(MainActivity.this, 
                            "WebSocket Server Started on port 8765", 
                            Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onServerStopped() {
                    runOnUiThread(() -> {
                        serverStatusText.setText("Server Status: Stopped");
                        startServerButton.setEnabled(true);
                        stopServerButton.setEnabled(false);
                        Toast.makeText(MainActivity.this, 
                            "WebSocket Server Stopped", 
                            Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onClientConnected(int clientCount) {
                    runOnUiThread(() -> {
                        String status = "Server: Running (" + clientCount + 
                                      " client" + (clientCount > 1 ? "s" : "") + ")";
                        serverStatusText.setText(status);
                        Toast.makeText(MainActivity.this, 
                            "Web viewer connected!", 
                            Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onClientDisconnected(int clientCount) {
                    runOnUiThread(() -> {
                        String status = "Server: Running (" + clientCount + 
                                      " client" + (clientCount > 1 ? "s" : "") + ")";
                        serverStatusText.setText(status);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> 
                        Toast.makeText(MainActivity.this, 
                            "Server Error: " + error, 
                            Toast.LENGTH_LONG).show()
                    );
                }
            }
        );
    }
    
    private void setupButtonListeners() {
        startServerButton.setOnClickListener(v -> startWebSocketServer());
        stopServerButton.setOnClickListener(v -> stopWebSocketServer());
        toggleEdgeButton.setOnClickListener(v -> toggleEdgeDetection());
    }
    
    private void startWebSocketServer() {
        try {
            webSocketServer.start();
        } catch (Exception e) {
            Toast.makeText(this, 
                "Failed to start server: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            Log.e(TAG, "Server start error", e);
        }
    }
    
    private void stopWebSocketServer() {
        if (webSocketServer != null) {
            webSocketServer.stopServer();
        }
    }
    
    private void toggleEdgeDetection() {
        if (frameProcessor != null) {
            frameProcessor.toggleEdgeDetection();
            String mode = frameProcessor.isEdgeDetectionEnabled() ? 
                         "Edge Detection" : "Raw Feed";
            toggleEdgeButton.setText("Mode: " + mode);
            Toast.makeText(this, "Switched to " + mode, Toast.LENGTH_SHORT).show();
            
            // Toggle between camera preview and OpenGL rendering
            if (frameProcessor.isEdgeDetectionEnabled()) {
                previewView.setVisibility(android.view.View.GONE);
                glSurfaceView.setVisibility(android.view.View.VISIBLE);
            } else {
                previewView.setVisibility(android.view.View.VISIBLE);
                glSurfaceView.setVisibility(android.view.View.GONE);
            }
        }
    }
    
    /**
     * Send processed frame to web viewer via WebSocket
     */
    public void sendFrameToWebViewer(Bitmap processedFrame) {
        if (webSocketServer != null && webSocketServer.hasConnectedClients()) {
            webSocketServer.broadcastFrame(processedFrame);
        }
    }
    
    /**
     * Send frame statistics to web viewer
     */
    public void sendStatsToWebViewer(int width, int height, double fps, 
                                      double processingTime, int frameCount) {
        if (webSocketServer != null && webSocketServer.hasConnectedClients()) {
            webSocketServer.broadcastStats(width, height, fps, processingTime, frameCount);
        }
    }
    
    /**
     * Update FPS display on UI
     */
    public void updateFpsDisplay(double fps) {
        fpsText.setText(String.format("FPS: %.1f", fps));
    }
    
    /**
     * Update OpenGL texture with processed frame
     */
    public void updateGLTexture(Bitmap processedFrame) {
        if (glRenderer != null && processedFrame != null) {
            // Convert bitmap to RGBA byte array for OpenGL
            int width = processedFrame.getWidth();
            int height = processedFrame.getHeight();
            int[] pixels = new int[width * height];
            processedFrame.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // Convert ARGB to RGBA
            byte[] rgbaBytes = new byte[width * height * 4];
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                rgbaBytes[i * 4] = (byte) ((pixel >> 16) & 0xFF);     // R
                rgbaBytes[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);  // G
                rgbaBytes[i * 4 + 2] = (byte) (pixel & 0xFF);         // B
                rgbaBytes[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // A
            }
            
            // Update OpenGL texture
            glRenderer.updateFrame(rgbaBytes, width, height);
            glSurfaceView.requestRender();
        }
    }
    
    private void displayIpAddress() {
        String ipAddress = getIPAddress();
        String displayText = "Connect to: ws://" + ipAddress + ":8765";
        ipAddressText.setText(displayText);
    }
    
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(
                NetworkInterface.getNetworkInterfaces()
            );
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // Return IPv4 address
                        if (sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address", e);
        }
        return "localhost";
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, 
                    "Camera initialization failed: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis use case (for OpenCV processing)
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        
        // Set up frame processor
        frameProcessor = new CameraFrameProcessor(this);
        imageAnalysis.setAnalyzer(cameraExecutor, frameProcessor);
        
        // Select back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();
        
        // Bind use cases to camera
        cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            preview,
            imageAnalysis
        );
    }
    
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.CAMERA},
            CAMERA_PERMISSION_REQUEST
        );
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, 
                                          @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, 
                    "Camera permission is required", 
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop WebSocket server
        if (webSocketServer != null) {
            webSocketServer.stopServer();
        }
        
        // Release frame processor
        if (frameProcessor != null) {
            frameProcessor.release();
        }
        
        // Shutdown camera executor
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}