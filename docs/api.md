# The Koord common API

Koord puts one Kotlin API over ARCore (Android) and ARKit (iOS). This page
describes that API. [scope.md](scope.md) lists what it covers and what it
leaves out; [platform-apis.md](platform-apis.md) is the platform inventory it
was designed from.

> [!NOTE]
> **Android is implemented, iOS is not.** On Android the API runs on ARCore,
> including drawing the camera image. On iOS every `actual` is still a `TODO()`
> stub and throws `NotImplementedError`.

## Types

Everything lives under `io.github.erkko68.koord`.

| Type | Package | Kind | What it is |
| :--- | :--- | :--- | :--- |
| `ArSession` | root | `expect class` | The tracking session. Entry point for everything else. |
| `ArConfig`, `PlaneDetection` | root | data class, enum | What the session should track. |
| `ArFrame` | root | `expect class` | The session's state at one camera image. |
| `TrackingState`, `TrackingFailureReason` | root | enums | How well something is tracked, and why not. |
| `ArException` | root | sealed class | Platform failures. |
| `ArAvailability` | root | enum | Whether the device can run AR. |
| `DisplayRotation` | root | enum | Screen rotation, for `setDisplayGeometry`. |
| `CameraBackground` | `camera` | `expect class` | Draws the camera image behind a Filament scene. |
| `ArCamera`, `CameraIntrinsics` | `camera` | `expect class`, data class | Camera pose, matrices and intrinsics for a frame. |
| `Anchor` | `trackable` | `expect class` | A world pose the session keeps correcting. |
| `Plane` | `trackable` | `expect class` | A detected flat surface. |
| `HitResult` | `hit` | `expect class` | One intersection of a hit-test ray with a plane. |
| `LightEstimate` | `light` | data class | Ambient light of the real scene. |

The `expect` classes wrap a platform object; the rest is plain common code.
Each declaration carries KDoc with the details and the ARCore and ARKit
counterparts.

## Conventions

- **World space** is right-handed, Y-up, in metres, on both platforms.
- **Math types** come from `filament-utils` (`Mat4`, `Float3`, `Float2`, `Ray`),
  which `koord` exposes as an `api` dependency. Pass a `Mat4` to Filament with
  `toFloatArrayColumn()`.
- **Screen points** are in viewport pixels, origin top-left.
- **Frames are pulled.** Call `ArSession.update()` once per rendered frame,
  always from the same thread, which must also be the one that calls
  `createEngine()` and `close()`.
  It returns `null` while the session is paused and until the first frame
  exists. A frame is only valid until the next `update()`.
- **`DisplayRotation`** has the meaning of Android's `Surface.ROTATION_*`.
- **Live objects.** `Anchor` and `Plane` change as tracking improves; re-read
  their properties every frame instead of caching them.

## What is platform-specific

Two things have no common declaration, because their arguments differ:

| | Android | iOS |
| :--- | :--- | :--- |
| Create a session | `ArSession(context)` | `ArSession()` |
| Check availability | `checkArAvailability(context)` | `checkArAvailability()` |

Create the session in platform code and pass it to common code. The camera
permission is also the app's job on both platforms: the Android runtime
permission, and `NSCameraUsageDescription` on iOS.

On Android the constructor never throws: the ARCore session is created on the
first `resume()`, which is where a missing permission or a missing ARCore
install is reported. `checkArAvailability(context)` answers `SUPPORTED` while
ARCore is still checking with Google Play.

## Android setup

The app's manifest needs the camera permission and the ARCore marker, and the
app must hold the runtime permission before calling `resume()`:

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-feature android:name="android.hardware.camera.ar" android:required="true"/>

<application>
    <meta-data android:name="com.google.ar.core" android:value="required"/>
</application>
```

`pause()` the session when the app leaves the foreground; it holds the camera.

## First use

The session is created per platform:

```kotlin
// Android
val session = ArSession(context)

// iOS
val session = ArSession()
```

Everything after that is common:

```kotlin
session.configure(ArConfig(planeDetection = PlaneDetection.HORIZONTAL))
session.setDisplayGeometry(DisplayRotation.ROTATION_0, widthPx, heightPx)
session.resume()

// Once per rendered frame:
val frame = session.update() ?: return
if (frame.camera.trackingState == TrackingState.TRACKING) {
    // Drive the Filament camera.
    filamentCamera.setModelMatrix(frame.camera.transform.toFloatArrayColumn())

    // Place an anchor where the user tapped a detected plane.
    val anchor = frame.hitTest(tapX, tapY).firstOrNull()?.createAnchor()
}

// When leaving the screen, and when done for good:
session.pause()
session.close()
```

[samples/shared/…/App.kt](../samples/shared/src/commonMain/kotlin/io/github/erkko68/koord/sample/App.kt)
is the same flow as a Compose screen.

## Rendering with Filament

Koord does not own the render loop; it gives Filament what it needs to draw an
AR scene. Three pieces, all common code:

```kotlin
// 1. The engine must come from the session.
val engine = session.createEngine()

// 2. The camera image, as a renderable behind everything else.
val background = CameraBackground(engine, session)
scene.addEntity(background.entity)

// 3. Once per rendered frame:
val frame = session.update() ?: return
background.update(frame)
val projection = frame.camera.projectionMatrix(near, far).toFloatArrayColumn()
filamentCamera.setCustomProjection(DoubleArray(16) { projection[it].toDouble() }, near.toDouble(), far.toDouble())
filamentCamera.setModelMatrix(frame.camera.transform.toFloatArrayColumn())
```

- **Use `session.createEngine()`, not `Engine.create()`.** ARCore writes the
  camera image to an OpenGL texture, so on Android the engine has to use the
  OpenGL backend and share the session's GL context. Call it from the thread
  that calls `update()`. The caller destroys the engine.
- **`CameraBackground`** fills the viewport set with `setDisplayGeometry`,
  cropping the camera image to its shape. The view does not need to be
  transparent. Destroy it before the engine.
- **Colours:** the camera image is passed through the inverse of Filament's
  tone mapping, so it survives the view's post-processing roughly unchanged.

The sample does this with `filament-compose`: an engine passed to
`rememberFilamentScene`, a `FilamentEffect` for the per-frame work, and the
view's camera reached through `rememberFilamentViewState`.

## Errors

`ArSession.resume()` and `ArSession.update()` throw `ArException`:

| Subclass | Meaning |
| :--- | :--- |
| `CameraUnavailable` | The camera is in use elsewhere or could not be opened. |
| `CameraPermissionDenied` | The app does not hold the camera permission. |
| `Unsupported` | The device cannot run AR as it is, or ARCore is missing or too old. |
| `Unknown` | Anything else; the platform's detail is in `message` or `cause`. |

ARCore throws where the error happens. ARKit reports errors later through a
delegate, so on iOS a failure surfaces on the next `update()`.

## Behaviour that is deliberately the same on both platforms

- **Hit tests only hit detected planes**, inside their polygon. Nothing can be
  hit until plane detection has found a surface. `HitResult.plane` is therefore
  never null.
- **`ArSession.planes` never contains merged-away planes.** When two planes turn
  out to be one surface, only the survivor is listed.
- **`LightEstimate.intensity` is normalised**: `1.0` is a neutrally lit scene on
  both platforms.

## Behaviour that still differs

- `Plane.polygon` is convex on Android and may be concave on iOS.
- `Anchor.trackingState` is per anchor on Android. ARKit does not track plain
  anchors individually, so on iOS it follows the camera's state.
- Some `TrackingFailureReason` values are only reported by one platform; the
  KDoc on each value says which.
