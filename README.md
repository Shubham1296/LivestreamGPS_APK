# LiveStreamGPS

A real-time GPS location and camera streaming Android application built with Kotlin and Jetpack Compose. The app captures live camera feed, GPS coordinates, and streams them to a remote server via WebSocket connection.

## Features

### Core Functionality
- **Real-time Camera Streaming**: Captures and streams live camera feed at configurable FPS
- **GPS Location Tracking**: Continuously tracks device location with accuracy monitoring
- **WebSocket Communication**: Reliable bidirectional communication with automatic reconnection
- **User Authentication**: Secure login and registration system
- **Landscape Mode**: Optimized for landscape orientation viewing

### User Interface
- **Live Camera Preview**: Full-screen camera preview with overlay controls
- **Status Bar**: Real-time display of GPS accuracy, FPS, and connection status
- **Connection Indicator**: Visual indicator (green/red) showing WebSocket connection status
- **Logs Viewer**: In-app log viewer to monitor connection and streaming activity
- **Menu System**: Easy access to settings and logout functionality

### Technical Features
- **Permission Handling**: Automatic request and management of camera and location permissions
- **Configurable Server URL**: Editable WebSocket server URL (supports ngrok and custom servers)
- **Token-based Authentication**: Secure authentication with JWT token support
- **Automatic Reconnection**: Exponential backoff reconnection strategy (up to 8 attempts)
- **Error Handling**: Comprehensive error handling with user-friendly messages

## Screenshots

The app interface includes:
- Login screen with registration option
- Main screen with camera preview
- GPS and FPS status display
- Connection status indicator
- Logs viewer for debugging

## Requirements

### Development Environment
- **Android Studio**: Hedgehog (2023.1.1) or later
- **VS Code**: Latest version with Kotlin extension
- **JDK**: Java 17 or higher
- **Android SDK**: API Level 34 (Android 14)
- **Minimum Android Version**: API Level 26 (Android 8.0)

### Device Requirements
- Android device with camera
- GPS/Location services enabled
- Internet connection

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Camera**: CameraX API
- **Location**: Google Play Services Location API
- **Networking**: OkHttp3 WebSocket
- **Architecture**: MVVM with StateFlow
- **Build System**: Gradle (Kotlin DSL)

## Setup Instructions

### Option 1: Android Studio Setup

1. **Install Android Studio**
   - Download from [https://developer.android.com/studio](https://developer.android.com/studio)
   - Install Android SDK and required build tools

2. **Clone or Open Project**
   ```bash
   git clone https://github.com/Shubham1296/LivestreamGPS_APK.git
   cd LiveStreamGPS
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `LiveStreamGPS` directory
   - Wait for Gradle sync to complete

4. **Configure SDK**
   - Go to `File > Project Structure > SDK Location`
   - Ensure Android SDK is properly configured
   - Set JDK to version 17

5. **Sync Gradle**
   - Click "Sync Project with Gradle Files" if not done automatically
   - Wait for all dependencies to download

6. **Connect Device or Emulator**
   - Enable USB debugging on your Android device
   - Connect via USB, or
   - Create an Android Virtual Device (AVD) in Device Manager

7. **Run the App**
   - Click the "Run" button (green play icon) or press `Shift+F10`
   - Select your target device
   - Wait for build and installation

### Option 2: VS Code Setup

1. **Install Required Extensions**
   - Open VS Code
   - Install these extensions:
     - "Kotlin" by mathiasfrohlich
     - "Gradle for Java" by Microsoft
     - "Android iOS Emulator" by DiemasMichiels (optional)

2. **Install Android SDK**
   - Download Android command line tools from [https://developer.android.com/studio#command-tools](https://developer.android.com/studio#command-tools)
   - Extract and set up SDK:
     ```bash
     export ANDROID_HOME=$HOME/Android/Sdk
     export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
     export PATH=$PATH:$ANDROID_HOME/platform-tools
     ```
   - Install required packages:
     ```bash
     sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
     ```

3. **Install JDK 17**
   ```bash
   # macOS (using Homebrew)
   brew install openjdk@17

   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk

   # Windows
   # Download from https://adoptium.net/
   ```

4. **Clone and Open Project**
   ```bash
   git clone https://github.com/Shubham1296/LivestreamGPS_APK.git
   cd LiveStreamGPS
   code .
   ```

5. **Configure local.properties**
   - Create or edit `local.properties` in the project root:
     ```properties
     sdk.dir=/path/to/your/Android/Sdk
     ```

6. **Build the Project**
   - Open integrated terminal in VS Code
   - Run Gradle build:
     ```bash
     # macOS/Linux
     ./gradlew build

     # Windows
     gradlew.bat build
     ```

7. **Install on Device**
   - Connect your Android device with USB debugging enabled
   - Verify device connection:
     ```bash
     adb devices
     ```
   - Build and install debug APK:
     ```bash
     ./gradlew assembleDebug
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```

## Building the App

### Debug Build
```bash
# Using Gradle wrapper
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Build unsigned release APK
./gradlew assembleRelease

# For signed release, you'll need to configure signing in build.gradle.kts
```

## Installing on Device

### Method 1: Using ADB
```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Reinstall (keeps app data)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uninstall first
adb uninstall com.example.livestreamgps
```

### Method 2: Using Android Studio
- Click "Run" button
- Select target device
- App will build, install, and launch automatically

## Configuration

### Server Setup

The app requires a WebSocket server to connect to. You can configure the server URL in the app:

1. **On Login Screen**:
   - Tap "Edit Server URL"
   - Enter your WebSocket URL (e.g., `wss://your-server.com/ws`)
   - Tap "Save"

2. **On Main Screen**:
   - Tap the link icon in the top-right corner
   - Or use the menu and select "Edit Server URL"

### WebSocket URL Format
```
wss://your-ngrok-url/ws
wss://your-domain.com/ws
ws://192.168.1.100:8080/ws  (local development)
```

**Note**: The app automatically appends the authentication token as a query parameter:
```
wss://your-server.com/ws?token=YOUR_AUTH_TOKEN
```

## Usage

1. **First Launch**
   - Grant camera and location permissions when prompted
   - You'll see the login screen

2. **Registration**
   - Tap "Register Instead"
   - Enter email and password
   - Tap "Register"
   - You'll be automatically logged in

3. **Login**
   - Enter your registered email and password
   - Tap "Login"

4. **Configure Server**
   - Before streaming, set your WebSocket server URL
   - Tap "Edit Server URL" and enter your server address

5. **Start Streaming**
   - On the main screen, tap the green record button
   - The app will connect to the server and start streaming
   - Monitor connection status via the indicator (green = connected)

6. **View Logs**
   - Tap the logs icon (document icon) in the top-right
   - View real-time connection and streaming logs

7. **Stop Streaming**
   - Tap the red stop button
   - Camera and WebSocket connection will close

## Permissions

The app requires the following permissions:

- **CAMERA**: To capture video feed
- **ACCESS_FINE_LOCATION**: For precise GPS coordinates
- **ACCESS_COARSE_LOCATION**: For approximate location
- **INTERNET**: For WebSocket communication
- **ACCESS_NETWORK_STATE**: To check network connectivity

## Troubleshooting

### Common Issues

**1. App crashes on launch**
- Ensure you've granted all required permissions
- Check if your device meets minimum API level (26)

**2. Camera not showing**
- Grant camera permission in device settings
- Restart the app after granting permission

**3. GPS not working**
- Enable location services on your device
- Ensure location permission is granted
- Try using the app outdoors for better GPS signal

**4. WebSocket connection fails**
- Verify your server URL is correct
- Check if your server is running and accessible
- Ensure your device has internet connectivity
- For local development, use your computer's IP address
- Check server logs for authentication issues

**5. "Unauthorized" connection error**
- Ensure you're logged in
- The authentication token might have expired - try logging in again
- Verify your server accepts the token format

**6. Build fails in VS Code**
- Ensure ANDROID_HOME is set correctly
- Verify JDK 17 is installed and configured
- Run `./gradlew clean build` to clean and rebuild

## Development

### Project Structure
```
LiveStreamGPS/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/example/livestreamgps/
│   │       │   ├── MainActivity.kt
│   │       │   ├── LoginScreen.kt
│   │       │   ├── MainScreen.kt
│   │       │   ├── service/
│   │       │   │   ├── AuthService.kt
│   │       │   │   ├── CameraService.kt
│   │       │   │   ├── LocationService.kt
│   │       │   │   └── WebSocketClient.kt
│   │       │   ├── ui/theme/
│   │       │   │   ├── Color.kt
│   │       │   │   └── Theme.kt
│   │       │   └── viewmodel/
│   │       │       └── AuthViewModel.kt
│   │       └── res/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Key Components

- **MainActivity.kt**: Entry point, handles navigation between login and main screen
- **LoginScreen.kt**: Authentication UI with login/register functionality
- **MainScreen.kt**: Main interface with camera preview, controls, and status
- **CameraService.kt**: Manages camera capture and frame processing
- **LocationService.kt**: Handles GPS location updates
- **WebSocketClient.kt**: Manages WebSocket connection and data transmission
- **AuthViewModel.kt**: Handles authentication state and API calls

## Backend Requirements

This app requires a backend server with the following endpoints:

### Authentication Endpoints
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login and receive JWT token

### WebSocket Endpoint
- `WebSocket /ws?token=JWT_TOKEN` - Real-time streaming connection

The server should accept:
- Binary frames containing camera data
- GPS coordinates (latitude, longitude, accuracy)
- Metadata about the stream

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes (`git commit -m 'Add some feature'`)
4. Push to the branch (`git push origin feature/YourFeature`)
5. Open a Pull Request

## License

This project is open source and available for educational purposes.

## Support

For issues, questions, or contributions:
- Open an issue on GitHub: [https://github.com/Shubham1296/LivestreamGPS_APK/issues](https://github.com/Shubham1296/LivestreamGPS_APK/issues)

## Acknowledgments

Built with:
- Jetpack Compose
- CameraX
- OkHttp
- Google Play Services Location
- Material Design 3

---

**Note**: This app is designed for authorized use only. Ensure you have proper permissions before streaming camera and location data.
