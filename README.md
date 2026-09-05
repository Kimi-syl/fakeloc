# FakeLoc

Android app that fakes your device's GPS location the same way `wloc` does on iOS/macOS, using Android's developer-options mock-location API and an OpenStreetMap (osmdroid) picker.

## How it works

Android exposes `LocationManager.setTestProviderLocation()` for letting test apps feed fake locations into the system. To use it, the device has to be told "this app is allowed to be a mock provider". The app then pushes whatever coordinates the user picks on the map to both the GPS and Network providers, and every other app on the phone sees the fake location.

## Setup (one-time)

1. Install the APK
2. Enable Developer Options: **Settings → About phone → tap "Build number" 7 times**
3. Pick FakeLoc as the mock location app: **Developer Options → Select mock location app → FakeLoc**
4. Open FakeLoc, tap a spot on the map, hit **Start spoofing**

## Build

```bash
JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk \
  gradle --no-daemon :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

The build host (aarch64 Termux) requires overriding aapt2 to the arm64 build:
```properties
# gradle.properties
android.aapt2FromMavenOverride=/opt/aapt2arm/build-tools/aapt2
```

## Project layout

```
app/src/main/
├── AndroidManifest.xml              # ACCESS_MOCK_LOCATION + map activity
├── java/com/example/fakeloc/
│   ├── MainActivity.kt               # Compose host
│   ├── location/MockLocationManager.kt   # test-provider push/clear
│   ├── data/SavedLocationsStore.kt   # SharedPreferences-backed favorites
│   └── ui/FakeLocScreen.kt           # map + Compose UI
└── res/                              # strings, themes, launcher icons
```

## Requirements

- Android 7.0+ (minSdk 24)
- Target SDK 34
- Kotlin 1.9.24, AGP 8.5.2, Compose BOM 2024.09
- osmdroid 6.1.18 (OpenStreetMap, no API key)

## License

MIT
