# Koord

Augmented reality for Kotlin Multiplatform. Koord puts one Kotlin API over
ARCore on Android and ARKit on iOS, so that tracking, planes, anchors and hit
testing can be written once in common code. Rendering is meant to be done with
[filament-kmp](https://github.com/Erkko68/filament-kmp), whose math types the
API uses.

```kotlin
session.configure(ArConfig(planeDetection = PlaneDetection.HORIZONTAL))
session.resume()

val engine = session.createEngine()
val background = CameraBackground(engine, session)   // the camera image, as a renderable
scene.addEntity(background.entity)

// Once per rendered frame:
val frame = session.update() ?: return
background.update(frame)
if (frame.camera.trackingState == TrackingState.TRACKING) {
    val anchor = frame.hitTest(tapX, tapY).firstOrNull()?.createAnchor()
}
```

## Status

Koord is in early development and has not been published yet.

| | Android (ARCore) | iOS (ARKit) |
| :--- | :--- | :--- |
| Session lifecycle and configuration | implemented | implemented |
| Camera pose, matrices, intrinsics | implemented | implemented |
| Plane detection | implemented | implemented |
| Anchors | implemented | implemented |
| Hit testing | implemented | implemented |
| Light estimation | implemented | implemented |
| Camera image behind a Filament scene | implemented | not yet |

The iOS side compiles but has not been run on a device yet. Its
`CameraBackground` draws nothing: ARKit delivers the camera image as a
`CVPixelBuffer`, and filament-kmp does not yet bind the Filament call that
takes one (`Texture::setExternalImage`).

## Documentation

- [docs/api.md](docs/api.md): the API, its conventions, platform setup and a
  first-use walkthrough
- [docs/scope.md](docs/scope.md): what the API covers, what it leaves out, and
  why
- [docs/platform-apis.md](docs/platform-apis.md): the ARCore and ARKit features
  side by side, which the API was designed from

## Requirements

- JDK 21 (the Gradle daemon toolchain downloads it if missing)
- Android: an [ARCore-supported device](https://developers.google.com/ar/devices), minSdk 24
- iOS: Xcode, an ARKit-capable iPhone on iOS 18.2+

AR needs real devices; the emulator and simulator won't do.

## Repository layout

| Path | What |
| :--- | :--- |
| [koord](koord) | The library (`:koord`, `io.github.erkko68.koord:koord`) |
| [build-logic](build-logic) | Convention plugins; `koord-kmp-module` sets targets, SDK levels and JVM target |
| [samples](samples) | A separate Gradle build that consumes `koord` by its Maven coordinates |
| [docs](docs) | API, scope and platform notes |

`samples/` works like a normal consumer of the library. Its
[settings.gradle.kts](samples/settings.gradle.kts) includes the root build and
substitutes `io.github.erkko68.koord:koord` with the local `:koord` project, so
the samples always compile against the sources in this checkout.

## Building and running

Library only, from the repo root:

```sh
./gradlew :koord:assemble
```

The samples have their own Gradle wrapper. Run these from `samples/`, which
needs its own `local.properties` with `sdk.dir` (or `ANDROID_HOME` set).

Android:

```sh
./gradlew :androidApp:installDebug
```

The sample asks for the camera permission, then shows the camera with the
tracking state on top. Detected surfaces are tinted blue; tap one to place a
cube on it.

iOS: Gradle builds the Kotlin framework, Xcode builds and launches the app.
Open [samples/iosApp/iosApp.xcodeproj](samples/iosApp/iosApp.xcodeproj), set
`TEAM_ID` in [Config.xcconfig](samples/iosApp/Configuration/Config.xcconfig),
and run on a device. The Xcode build calls `./gradlew
:shared:embedAndSignAppleFrameworkForXcode` itself. To build from the command
line:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS' build
```
