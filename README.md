# Flam - Real-time Edge Detection with WebSocket Streaming

Flam is an Android application that performs real-time edge detection on camera input and streams the processed frames to a web viewer via WebSocket connection. The project consists of two main components: an Android app for edge detection and a web-based viewer for remote monitoring.

## Features

- Real-time edge detection using Android camera
- WebSocket server on Android device for streaming processed frames
- Web-based viewer to remotely monitor edge detection results
- Live statistics display (FPS, processing time, frame count)
- Cross-platform viewing via web browser

## Project Structure

```
flam/
├── app/                 # Android application (Kotlin)
├── web/                 # Web viewer (TypeScript/Vite)
├── MainActivity.java    # Main Android activity
├── WebSocketServer.java # WebSocket server implementation
└── ...
```

## Android App

The Android component captures camera input, processes frames for edge detection, and broadcasts the results via WebSocket.

### Key Components

- **MainActivity.java**: Main application activity handling UI and camera permissions
- **WebSocketServer.java**: WebSocket server implementation for streaming frames to web viewers
- **Edge Detection**: Real-time processing of camera frames (implementation details in native code)

### Permissions

The app requires the following permissions:
- CAMERA: For accessing the device camera
- INTERNET: For WebSocket communication
- ACCESS_NETWORK_STATE: For network status checking
- ACCESS_WIFI_STATE: For WiFi network information

## Web Viewer

The web component provides a real-time view of the edge detection results streamed from the Android device.

### Technologies Used

- TypeScript
- Vite (build tool)
- WebSocket client for receiving frames

### Setup

1. Navigate to the `web` directory:
   ```bash
   cd web
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

### Usage

1. Start the Android app on your device
2. Note the IP address displayed in the app
3. Start the web viewer on a computer connected to the same network
4. Connect to the WebSocket server using the provided IP address and port 8765

## How It Works

1. The Android app initializes the camera and starts edge detection processing
2. A WebSocket server runs on port 8765 on the Android device
3. Processed frames are converted to base64 and broadcast to connected clients
4. The web viewer connects to the WebSocket server and displays the incoming frames
5. Real-time statistics are sent alongside frames for performance monitoring

## Building the Android App

1. Open the project in Android Studio
2. Build and run on a physical device (camera access required)

## Building the Web Viewer

1. Navigate to the web directory:
   ```bash
   cd web
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Build for production:
   ```bash
   npm run build
   ```

## Connecting the Components

1. Ensure both Android device and web viewer computer are on the same network
2. Start the Android app and note the displayed IP address
3. Start the web viewer and connect to `ws://[IP_ADDRESS]:8765`
4. Processed frames will appear in real-time in the web viewer

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Uses Java-WebSocket library for WebSocket communication
- Built with Android SDK and Vite.js