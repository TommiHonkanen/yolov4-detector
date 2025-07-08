# YOLOv4 Detector

Android app for running live object detection with YOLOv4 models on your phone's camera feed. Import your own models trained with Darknet using the Model Manager. Detection is done with OpenCV's DNN module. No need to convert the weights to another format.

## Features

- **Real-time Object Detection**: Use YOLOv4-Tiny for efficient mobile inference
- **Model Management**: Import and manage custom YOLO models
- **Adjustable Parameters**: Fine-tune confidence and NMS thresholds in real-time
- **Performance Metrics**: Live FPS and inference time display
- **Camera Controls**: Flash toggle, camera switching (front/back)
- **Material Design UI**: Modern, intuitive interface with smooth animations

## Screenshots

*TBD - Screenshots coming soon*

## Requirements

- Android 7.0 (API level 24) or higher
- Device with camera

## Installation

### Option 1: Download APK

Download the latest APK from the [Releases](https://github.com/TommiHonkanen/yolov4-detector/releases) page.

### Option 2: Build from Source

#### Prerequisites
- Latest version of Android Studio
- JDK 17
- Android SDK with API level 33

#### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/TommiHonkanen/yolov4-detector.git
   cd yolov4-detector
   ```

2. Open the project in Android Studio

3. Build the project:
   ```bash
   # Debug build
   ./gradlew assembleDebug
   
   # Release build
   ./gradlew assembleRelease
   
   # Clean build
   ./gradlew clean
   
   # Run tests
   ./gradlew test
   ```

4. Install on device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   
## Usage

### Basic Operation

1. **Grant Camera Permission** when prompted on first launch
2. **Point camera** at objects to detect
3. **View detections** with bounding boxes and confidence scores

### Controls

- **Main FAB (Floating Action Button)**:
  - Tap: Pause/Resume detection
  - Long press: Open quick actions menu
- **Flash**: Toggle camera flash on/off
- **Flip Camera**: Switch between front and back cameras
- **Settings**: Adjust detection thresholds
- **Models**: Access model manager

### Threshold Settings

- **Confidence Threshold** (default: 25%): Minimum confidence for detections
- **NMS Threshold** (default: 45%): Non-maximum suppression for overlapping boxes

### Model Management

The app includes YOLOv4-Tiny trained on COCO dataset (80 classes) by default. You can import custom models:

1. Tap the **Models** button
2. Tap the **+** button to import
3. Select three files in any order:
   - `.weights` - Neural network weights
   - `.cfg` - Network architecture configuration  
   - `.names` - Class labels (one per line)

## Supported Models

The app supports Darknet YOLO format models. Tested configurations:

- YOLOv4
- YOLOv4-Tiny
- YOLOv4-Tiny-3l

### Model Requirements

- Input size must be square (e.g., 416x416, 608x608)
- Config file must contain proper [net] and [yolo] sections
- Names file must have one class label per line

## Architecture

### Core Components

- **MainActivity**: Camera management and UI
- **YoloDetector**: YOLO inference engine using OpenCV DNN
- **ModelManager**: Model storage and configuration
- **DetectionOverlayView**: Real-time visualization

### Technical Stack

- **Language**: Kotlin
- **Camera**: CameraX API
- **ML Framework**: OpenCV DNN module
- **UI**: Material Design 3
- **Architecture**: MVVM pattern with Coroutines

## Performance

Typical performance on modern devices:
- YOLOv4-Tiny: 10-20 FPS
- YOLOv4: 1-5 FPS

Performance varies based on:
- Device processing power
- Network Dimensions

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Joseph Redmon (pjreddie)](https://pjreddie.com/) - Original YOLO author and Darknet framework creator
- [Alexey Bochkovskiy (AlexeyAB)](https://github.com/AlexeyAB/darknet) - YOLOv4 author and Darknet maintainer
- [Stéphane Charette](https://www.ccoderun.ca/darknet/) - Darknet/YOLO maintainer and creator of DarkHelp and DarkMark
- [OpenCV](https://opencv.org/) - Computer vision library