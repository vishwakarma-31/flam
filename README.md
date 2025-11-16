# Flam - Real-Time Edge Detection Viewer

This project is a complete implementation of the Android + OpenCV-C++ + OpenGL Assessment for RnD Intern position. It captures camera frames, processes them using OpenCV in C++ via JNI, renders output with OpenGL ES, and includes a TypeScript-based web viewer.

## ✅ Features Implemented

### 📱 Android Application
- **Camera Feed Integration**: Uses CameraX for real-time camera frame capture
- **OpenCV C++ Processing**: Native C++ implementation of Canny Edge Detection and grayscale filtering
- **OpenGL ES Rendering**: Smooth real-time rendering of processed frames using OpenGL ES 2.0
- **JNI Communication**: Efficient Java ↔ C++ communication for frame processing
- **Toggle Functionality**: Switch between raw camera feed and edge-detected output
- **Performance Metrics**: FPS counter and frame processing time display

### 🌐 Web Viewer (TypeScript)
- **Frame Display**: Shows processed frames streamed from Android app
- **Statistics Overlay**: Displays real-time FPS, resolution, and processing time
- **WebSocket Connection**: Receives frames in real-time from Android device
- **Responsive Design**: Works on various screen sizes

## 📸 Screenshots

![Main Screen](images/Main.jpg)
*Main application screen with controls*

![Edge Detection](images/Edge.jpg)
*Real-time edge detection view*

![Professional View](images/Pro.jpg)
*Professional view with statistics*

## ⚙️ Architecture

```
flam/
├── app/                 # Android application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/    # Java/Kotlin source code
│   │   │   └── res/     # Resources and layouts
├── jni/                 # Native C++ code with OpenCV
├── gl/                  # OpenGL renderer classes
├── web/                 # TypeScript web viewer
├── images/              # Screenshot images
└── ...
```

### Component Flow

1. **Camera Capture**: Android CameraX captures frames and sends to native processor
2. **JNI Interface**: Java code communicates with C++ via JNI for frame processing
3. **Native Processing**: OpenCV C++ applies Canny Edge Detection or grayscale filter
4. **OpenGL Rendering**: Processed frames rendered in real-time using OpenGL ES 2.0
5. **WebSocket Streaming**: Processed frames broadcast to web viewer via WebSocket
6. **Web Display**: TypeScript web page receives and displays frames with statistics

## 🧩 Technical Implementation

### 1. 📸 Camera Feed Integration
- Uses Android CameraX with ImageAnalysis for efficient camera access
- Implements SurfaceTexture for direct OpenGL integration
- Handles camera permissions and lifecycle management

### 2. 🔁 OpenCV C++ Processing
- **EdgeProcessor Class**: Core C++ implementation of Canny edge detection
- **Native Methods**: JNI interface for Java ↔ C++ communication
- **Bitmap Conversion**: Efficient conversion between Android Bitmap and OpenCV Mat
- **Performance Optimization**: Real-time processing with minimal latency

Key C++ files:
- `EdgeProcessor.h/cpp`: Implements Canny edge detection algorithm
- `native-lib.cpp`: JNI interface and bitmap conversion utilities

### 3. 🎨 OpenGL ES Rendering
- Custom `GLRenderer` implementation for efficient frame rendering
- Real-time texture updates with OpenGL ES 2.0
- Vertex and fragment shaders for image display
- Direct RGBA byte array rendering for optimal performance

### 4. 🌐 Web Viewer (TypeScript)
- WebSocket client for receiving frames from Android app
- Real-time display of processed frames
- Statistics overlay (FPS, resolution, processing time)
- Responsive design using modern CSS

## 🚀 Setup Instructions

### Prerequisites
- Android Studio with NDK support (version 25.0 or higher)
- OpenCV Android SDK (version 4.x)
- Node.js (version 16.0 or higher) and npm for web viewer
- Physical Android device (API level 24 or higher) for testing

### OpenCV SDK Setup

1. **Download OpenCV**:
   - Download OpenCV Android SDK from [OpenCV releases page](https://opencv.org/releases/)
   - Extract to a known location (e.g., `C:\opencv-android` or `/opt/opencv-android`)

2. **Configure CMakeLists.txt**:
   - Open [jni/CMakeLists.txt](file:///d:/Projects/flam/jni/CMakeLists.txt)
   - Update line 11 with the correct path to your OpenCV SDK:
     ```cmake
     set(OpenCV_DIR "YOUR_OPENCV_PATH/sdk/native/jni")
     ```
   - Example for Windows:
     ```cmake
     set(OpenCV_DIR "C:/opencv-android/sdk/native/jni")
     ```
   - Example for macOS/Linux:
     ```cmake
     set(OpenCV_DIR "/opt/opencv-android/sdk/native/jni")
     ```

3. **Verify OpenCV Integration**:
   - Android Studio should automatically detect the OpenCV libraries
   - If you encounter issues, ensure the path in CMakeLists.txt is correct

### Android App Build and Run

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd flam
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the project directory and open it

3. **Sync Project**:
   - Android Studio will automatically sync the project
   - Wait for Gradle sync to complete

4. **Build the project**:
   ```bash
   # Using Gradle wrapper (from project root)
   ./gradlew build
   
   # Or on Windows
   gradlew.bat build
   ```

5. **Run on device**:
   - Connect a physical Android device with USB debugging enabled
   - In Android Studio, select the device from the device dropdown
   - Click "Run" or use:
   ```bash
   # Using Gradle wrapper
   ./gradlew installDebug
   
   # Or on Windows
   gradlew.bat installDebug
   ```

6. **Alternative run method**:
   - In Android Studio, click the green "Run" button
   - Or press Shift+F10 (Windows/Linux) or Ctrl+R (macOS)

### Web Viewer Build and Run

1. **Navigate to web directory**:
   ```bash
   cd web
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Start development server**:
   ```bash
   npm run dev
   ```
   - The web viewer will be available at `http://localhost:5173`
   - The server includes hot-reloading for development

4. **Build for production**:
   ```bash
   npm run build
   ```
   - Production build will be output to the `dist` folder

5. **Preview production build**:
   ```bash
   npm run preview
   ```
   - Serves the production build locally for testing

6. **Alternative build commands**:
   ```bash
   # Using Vite directly (if installed globally)
   vite build
   
   # Serve the built files
   vite preview
   ```

## 🧠 Architecture Explanation

### JNI Communication
The project uses JNI for efficient communication between Java and C++:
- Java code captures camera frames and passes them to native code
- C++ code processes frames using OpenCV and returns results
- Direct memory access minimizes copying overhead

### Frame Flow
1. CameraX captures frame → Java Bitmap
2. Java passes Bitmap to C++ via JNI
3. C++ converts Bitmap to OpenCV Mat
4. OpenCV processes frame (edge detection or grayscale)
5. C++ converts processed Mat back to Bitmap
6. Java receives processed Bitmap
7. OpenGL renders Bitmap as texture
8. WebSocket broadcasts frame to web viewer

### TypeScript Implementation
- Uses modern TypeScript with ES6 modules
- WebSocket client for real-time frame reception
- Canvas API for efficient image rendering
- DOM manipulation for statistics display

## ⭐️ Bonus Features Implemented

- [x] Toggle button between raw camera feed and edge-detected output
- [x] FPS counter and frame processing time display
- [x] WebSocket streaming to web viewer
- [x] Real-time statistics overlay on web viewer
- [x] Professional UI with intuitive controls

## 📦 Project Structure Details

```
/app/src/main/java/
├── com.yourname.edgedetection/
│   ├── MainActivity.java   # Main application activity
│   ├── NativeProcessor.java # JNI wrapper class
│   └── CameraFrameProcessor.java # Camera frame processing
/gl/
├── GLRenderer.java     # OpenGL ES renderer
/jni/
├── CMakeLists.txt          # Build configuration for native code
├── EdgeProcessor.h/cpp     # OpenCV edge detection implementation
├── native-lib.cpp          # JNI interface and bitmap conversion
/web/
├── src/
│   ├── app.ts              # Main TypeScript application
│   ├── index.html          # HTML structure
│   └── styles.css          # Styling
├── package.json            # Dependencies and scripts
├── tsconfig.json           # TypeScript configuration
└── vite.config.js          # Vite build configuration
```

## 🧪 Testing & Validation

The implementation has been tested for:
- Real-time performance (maintains 15+ FPS on mid-range devices)
- Memory efficiency (no memory leaks in native code)
- Cross-platform compatibility (web viewer works on Chrome, Firefox, Safari)
- Network resilience (WebSocket reconnection handling)

## 📤 Submission

This project is properly committed and pushed to a public repository with a clear commit history demonstrating the development process.

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- OpenCV library for computer vision algorithms
- Java-WebSocket library for WebSocket communication
- Android SDK and NDK for native development
- Vite.js for web development tooling