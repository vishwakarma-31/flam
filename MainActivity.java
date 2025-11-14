package com.yourname.edgedetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    private EdgeDetectionWebSocketServer webSocketServer;
    private TextView serverStatusText;
    private TextView ipAddressText;
    private Button startServerButton;
    private Button stopServerButton;
    
    // Your existing camera and rendering components
    // private CameraManager cameraManager;
    // private GLRenderer glRenderer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        serverStatusText = findViewById(R.id.serverStatus);
        ipAddressText = findViewById(R.id.ipAddress);
        startServerButton = findViewById(R.id.startServerButton);
        stopServerButton = findViewById(R.id.stopServerButton);
        
        // Set up WebSocket server callbacks
        setupWebSocketServer();
        
        // Button listeners
        startServerButton.setOnClickListener(v -> startWebSocketServer());
        stopServerButton.setOnClickListener(v -> stopWebSocketServer());
        
        // Check camera permission
        if (checkCameraPermission()) {
            initializeCamera();
        } else {
            requestCameraPermission();
        }
        
        // Display device IP address
        displayIpAddress();
    }
    
    private void setupWebSocketServer() {
        webSocketServer = new EdgeDetectionWebSocketServer(new EdgeDetectionWebSocketServer.ServerCallback() {
            @Override
            public void onServerStarted() {
                runOnUiThread(() -> {
                    serverStatusText.setText("Server Status: Running");
                    startServerButton.setEnabled(false);
                    stopServerButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "WebSocket Server Started", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onServerStopped() {
                runOnUiThread(() -> {
                    serverStatusText.setText("Server Status: Stopped");
                    startServerButton.setEnabled(true);
                    stopServerButton.setEnabled(false);
                    Toast.makeText(MainActivity.this, "WebSocket Server Stopped", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onClientConnected(int clientCount) {
                runOnUiThread(() -> {
                    String status = "Server Status: Running (" + clientCount + " client" + (clientCount > 1 ? "s" : "") + ")";
                    serverStatusText.setText(status);
                    Toast.makeText(MainActivity.this, "Web viewer connected!", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onClientDisconnected(int clientCount) {
                runOnUiThread(() -> {
                    String status = "Server Status: Running (" + clientCount + " client" + (clientCount > 1 ? "s" : "") + ")";
                    serverStatusText.setText(status);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Server Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void startWebSocketServer() {
        try {
            webSocketServer.start();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start server: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopWebSocketServer() {
        if (webSocketServer != null) {
            webSocketServer.stopServer();
        }
    }
    
    /**
     * Call this method when you have a processed frame ready to send to web viewer
     */
    public void sendFrameToWebViewer(Bitmap processedFrame) {
        if (webSocketServer != null && webSocketServer.hasConnectedClients()) {
            // Send frame (throttle if needed - maybe every 5th frame or based on time)
            webSocketServer.broadcastFrame(processedFrame);
        }
    }
    
    /**
     * Call this method to send statistics to web viewer
     */
    public void sendStatsToWebViewer(int width, int height, double fps, double processingTime, int frameCount) {
        if (webSocketServer != null && webSocketServer.hasConnectedClients()) {
            webSocketServer.broadcastStats(width, height, fps, processingTime, frameCount);
        }
    }
    
    private void displayIpAddress() {
        String ipAddress = getIPAddress();
        String displayText = "Connect web viewer to: ws://" + ipAddress + ":8765";
        ipAddressText.setText(displayText);
    }
    
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // IPv4
                        if (sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "localhost";
    }
    
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void initializeCamera() {
        // Your existing camera initialization code
        // Initialize your CameraManager, GLRenderer, etc.
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketServer != null) {
            webSocketServer.stopServer();
        }
        // Clean up your camera and OpenGL resources
    }
}