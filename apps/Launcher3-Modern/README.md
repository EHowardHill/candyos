# Launcher3 (KitKat)

A modern fork of the Android 4.4 (KitKat) build of Launcher3, the stock home screen.

## Building

Requirements:

- JDK 17
- Android SDK with platform 34 (`sdkmanager "platforms;android-34"`)

Build an APK:

```sh
./gradlew assembleDebug        # outputs build/outputs/apk/debug/Launcher3-debug.apk
```