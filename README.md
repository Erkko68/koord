# Koord

Augmented reality for Kotlin Multiplatform. Koord puts one Kotlin API over
ARCore on Android and ARKit on iOS, so that tracking, planes, anchors and hit
testing can be written once in common code. Koord itself does not render and
depends on no renderer; the `koord-filament` module draws the camera image
with [filament-kmp](https://github.com/Erkko68/filament-kmp).

```kotlin
session.configure(ArConfig(planeDetection = PlaneDetection.HORIZONTAL))
session.resume()

// With koord-filament:
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
| Hit testing, inside a plane or on its extension | implemented | implemented |
| Light estimation | implemented | implemented |
| Camera image behind a Filament scene (`koord-filament`) | implemented | implemented |

The iOS side is new and not yet confirmed working on a device.

## Documentation

- [API reference](https://erkko68.github.io/koord/): every public declaration,
  generated from the KDoc on each push to `main`
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
| [koord-filament](koord-filament) | Rendering with Filament (`:koord-filament`, `io.github.erkko68.koord:koord-filament`) |
| [build-logic](build-logic) | Convention plugins; `koord-kmp-module` sets targets, SDK levels and JVM target |
| [samples](samples) | A separate Gradle build that consumes both by their Maven coordinates |
| [docs](docs) | API, scope and platform notes |

`samples/` works like a normal consumer of the library. Its
[settings.gradle.kts](samples/settings.gradle.kts) includes the root build and
substitutes `io.github.erkko68.koord:koord` and `koord-filament` with the local
projects, so the samples always compile against the sources in this checkout.

## Building and running

Libraries only, from the repo root:

```sh
./gradlew assemble
./gradlew :dokkaGenerate    # API reference, into build/dokka/html
```

The samples have their own Gradle wrapper. Run these from `samples/`, which
needs its own `local.properties` with `sdk.dir` (or `ANDROID_HOME` set).

Android:

```sh
./gradlew :androidApp:installDebug
```

The sample asks for the camera permission, then shows the camera with the
tracking state on top. Detected surfaces are tinted, floors and tables blue
and walls orange; tap one to stand a cube on it. A tap beside a detected
surface places the cube on its extension, which is how a plain wall becomes
usable from a small patch. The cubes are lit by the session's light estimate,
and a button removes them.

Its code, in [samples/shared](samples/shared/src/commonMain/kotlin/io/github/erkko68/koord/sample),
is split by what it shows: `App.kt` is the screen, `ArSessionEffect.kt` is
where Koord meets Filament each frame, `ArContent.kt` draws the lights, planes
and cubes from the result, and `PlaneMesh.kt` turns a plane into triangles.

iOS: Gradle builds the Kotlin framework, Xcode builds and launches the app.
Open [samples/iosApp/iosApp.xcodeproj](samples/iosApp/iosApp.xcodeproj), pick
your own team under Signing & Capabilities, and run on a device. The Xcode build calls `./gradlew
:shared:embedAndSignAppleFrameworkForXcode` itself. To build from the command
line:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS' build
```
