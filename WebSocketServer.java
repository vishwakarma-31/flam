package com.yourname.edgedetection;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EdgeDetectionWebSocketServer extends WebSocketServer {
    private static final String TAG = "WebSocketServer";
    private static final int PORT = 8765;
    
    private Set<WebSocket> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private ServerCallback callback;
    
    public interface ServerCallback {
        void onServerStarted();
        void onServerStopped();
        void onClientConnected(int clientCount);
        void onClientDisconnected(int clientCount);
        void onError(String error);
    }
    
    public EdgeDetectionWebSocketServer(ServerCallback callback) {
        super(new InetSocketAddress(PORT));
        this.callback = callback;
        setReuseAddr(true);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        Log.d(TAG, "New client connected: " + conn.getRemoteSocketAddress());
        
        if (callback != null) {
            callback.onClientConnected(clients.size());
        }
        
        // Send welcome message
        try {
            JSONObject message = new JSONObject();
            message.put("type", "welcome");
            message.put("message", "Connected to Android Edge Detection App");
            conn.send(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating welcome message", e);
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        Log.d(TAG, "Client disconnected: " + conn.getRemoteSocketAddress());
        
        if (callback != null) {
            callback.onClientDisconnected(clients.size());
        }
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, "Received message: " + message);
        // Handle incoming messages if needed
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "WebSocket error", ex);
        
        if (callback != null) {
            callback.onError(ex.getMessage());
        }
    }
    
    @Override
    public void onStart() {
        Log.d(TAG, "WebSocket server started on port " + PORT);
        
        if (callback != null) {
            callback.onServerStarted();
        }
    }
    
    /**
     * Broadcast a frame to all connected clients
     */
    public void broadcastFrame(Bitmap bitmap) {
        if (clients.isEmpty()) {
            return;
        }
        
        try {
            // Convert bitmap to base64
            String base64Image = bitmapToBase64(bitmap);
            
            // Create JSON message
            JSONObject message = new JSONObject();
            message.put("type", "frame");
            message.put("data", base64Image);
            
            // Broadcast to all clients
            String jsonString = message.toString();
            for (WebSocket client : clients) {
                if (client.isOpen()) {
                    client.send(jsonString);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting frame", e);
        }
    }
    
    /**
     * Send frame statistics to all connected clients
     */
    public void broadcastStats(int width, int height, double fps, double processingTime, int frameCount) {
        if (clients.isEmpty()) {
            return;
        }
        
        try {
            JSONObject stats = new JSONObject();
            stats.put("fps", fps);
            stats.put("width", width);
            stats.put("height", height);
            stats.put("processingTime", processingTime);
            stats.put("frameCount", frameCount);
            
            JSONObject message = new JSONObject();
            message.put("type", "stats");
            message.put("stats", stats);
            
            String jsonString = message.toString();
            for (WebSocket client : clients) {
                if (client.isOpen()) {
                    client.send(jsonString);
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error broadcasting stats", e);
        }
    }
    
    /**
     * Convert bitmap to base64 string
     */
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
    
    /**
     * Get number of connected clients
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Check if any clients are connected
     */
    public boolean hasConnectedClients() {
        return !clients.isEmpty();
    }
    
    /**
     * Stop the server gracefully
     */
    public void stopServer() {
        try {
            stop();
            if (callback != null) {
                callback.onServerStopped();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping server", e);
        }
    }
}