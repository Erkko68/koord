# Scope of the common API

What Koord takes from the two platforms, what it leaves out, and why. Feature
names follow [platform-apis.md](platform-apis.md); the API itself is described
in [api.md](api.md).

Decided on 2026-10-05. The split of rendering into `koord-filament` was decided
on 2026-10-06. Depth was added on 2026-10-07.

## In the API

| Area | Koord | ARCore | ARKit |
| :--- | :--- | :--- | :--- |
| Session lifecycle | `ArSession`: `configure`, `resume`, `pause`, `close` | `Session` | `ARSession` + `ARWorldTrackingConfiguration` |
| Display geometry | `ArSession.setDisplayGeometry` | `setDisplayGeometry` | passed per call on the ARKit side |
| Frames, pulled | `ArSession.update()`, `ArFrame` | `Session.update()` | latest frame from `ARSessionDelegate`, cached |
| Configuration | `ArConfig`: plane detection, light estimation, autofocus, depth | `Config` | configuration properties |
| Camera | `ArCamera`: pose, view and projection matrices, intrinsics | `Camera` | `ARCamera` |
| Tracking state | `TrackingState`, `TrackingFailureReason` | `TrackingState`, `TrackingFailureReason` | `trackingState`, `trackingStateReason` |
| Anchors | `Anchor`, `ArSession.createAnchor`, `ArSession.anchors` | `Anchor` | `ARAnchor` |
| Planes | `Plane`, `ArSession.planes` | `Plane` | `ARPlaneAnchor` |
| Hit testing | `ArFrame.hitTest` (screen point or `Ray`), `HitTarget`, `HitResult` | `Frame.hitTest` | `ARRaycastQuery`, `ARSession.raycast` |
| Light estimate | `LightEstimate`: intensity, colour | `LightEstimate` pixel intensity, colour correction | `ARLightEstimate` ambient intensity, colour temperature |
| Depth | `ArConfig.depth`, `ArFrame.depthImage`, `DepthImage` | `DepthMode.AUTOMATIC`, `Frame.acquireDepthImage16Bits` | `frameSemantics` `smoothedSceneDepth`, `ARDepthData.depthMap` |
| Camera image, raw | `ArFrame.cameraImageUv`, plus per platform: `ArSession.eglContext`, `ArSession.cameraTextureNames` and `ArFrame.cameraTextureName` on Android, `ArFrame.cameraImage` on iOS | external OES textures in a GL context, `Frame.transformCoordinates2d` | `ARFrame.capturedImage`, `ARFrame.displayTransform` |
| Camera image, drawn (`koord-filament`) | `ArSession.createEngine`, `CameraBackground` | the OES texture, in a GL context shared with Filament | the two planes of the image as Metal textures imported into Filament |
| Errors | `ArException` | exceptions | `session(_:didFailWithError:)` |
| Availability | `ArAvailability`, platform `checkArAvailability` | `ArCoreApk.checkAvailability` | `ARConfiguration.isSupported` |

## Decisions where the platforms disagree

| Topic | Decision | Why |
| :--- | :--- | :--- |
| Transforms | A `Mat4`, not a pose type | Renderers consume matrices; ARKit already uses them; ARCore's `Pose` converts with `toMatrix`. |
| Rendering | None in `koord`; Filament support is a separate module, `koord-filament` | An app that renders with something else should not pull in Filament. `filament-utils` depends on the whole engine, so even its math types could not stay. |
| Math types | Koord's own `Mat4`, `Float3`, `Float2`, `Ray`, carrying values only | The price of not depending on a renderer. They have no public operations, so they do not compete with the renderer's types: `Mat4.toFloatArray()` goes to any renderer, `Mat4.toFilament()` to `filament-utils`. |
| Frame delivery | Pull, on both | It matches a render loop. ARKit's pushed frames are cached and returned by `update()`. |
| Session constructor and availability check | Platform-specific, no common declaration | Android needs a `Context`, iOS needs nothing. |
| Errors | One sealed `ArException`; on iOS thrown from the next `update()` | ARCore throws, ARKit calls a delegate. Throwing gives common code one path. |
| Hit targets | Detected planes only: inside their polygon, or extended without limit (`HitTarget`) | Both platforms hit a detected plane beyond its polygon natively (ARKit's `existingPlaneInfinite`, ARCore's unfiltered plane hits), and it is how a plain wall becomes usable from a small patch. ARKit's estimated planes and ARCore's instant placement, point and depth hits have no counterpart on the other side. |
| Merged planes | Hidden from `ArSession.planes` | ARKit removes them; ARCore keeps them with `getSubsumedBy` set. |
| Layered planes | On Android, a plane within 10 cm of a larger parallel plane that covers its centre is hidden from `ArSession.planes` and from hit tests | ARCore starts a plane on any flat patch and merges late, so one floor shows as stacked planes and low objects as planes of their own. ARKit holds these back. ARCore has no setting for it; filtering is what ARCore apps do. |
| Light | Normalised intensity and an RGB tint only | These are the only values both platforms report during world tracking. |
| Camera image | Handed out per platform, with no common type; only where the viewport falls in it (`ArFrame.cameraImageUv`) is common | Android gives an OES texture tied to a GL context, iOS a YCbCr pixel buffer: there is no shape to share. `koord-filament`'s `CameraBackground` hides both behind a renderable; the price there is that the Filament engine must be created for the session. |
| Depth | A common `DepthImage`: metres as floats, copied to the CPU, smoothed on both. `null` on a device without depth | Unlike the camera image, both platforms deliver depth as a small CPU buffer, so one type fits and the copy is cheap. ARCore's default depth is already smoothed over time, so ARKit's `smoothedSceneDepth` is its counterpart, not `sceneDepth`. On iOS only devices with a LiDAR scanner have it; that is a hardware limit, as ARCore's list of depth devices is, so the feature is in and the limit is documented. |
| Installing ARCore | Reported as `ArAvailability.NEEDS_INSTALL`; the app triggers it | It needs an `Activity` and has no iOS counterpart. |

## Left out

### Waiting on rendering

| Feature | Why it waits |
| :--- | :--- |
| Occlusion | The depth image is in the API; drawing with it in `koord-filament` is not done. |
| The camera image's pixels on the CPU | The image is handed out as each platform delivers it, which on Android is a GL texture. Nothing needs to read it yet. |

### Common on both platforms, planned for later

| Feature | Note |
| :--- | :--- |
| Image tracking | Needs a reference-image database type and a new trackable. The largest common feature not yet covered. |
| Feature point cloud | Lines up on both; mostly useful for debug visualisation. |
| Camera resolution and frame rate selection | Both have it; no need yet. |
| Geospatial anchors | Both have them, with different models and availability rules. |

### Skipped until something needs them

| Feature | Why |
| :--- | :--- |
| Per-frame lists of updated anchors and planes | Polling `ArSession.planes` and `ArSession.anchors` covers it. |
| Raw depth and depth confidence | Both platforms have a confidence map, with different scales. |
| A query for whether the device has depth | `ArFrame.depthImage` is `null` without it. On Android the answer needs the ARCore session, which only exists after the first `resume()`. |
| Session reset | ARKit has run options; ARCore needs the session recreated. |
| Interruption callbacks | ARKit only. They already show up as `TrackingState.LIMITED` with `RELOCALIZING`. |
| Main light direction, spherical harmonics, reflection cube map | ARKit only reports the first two in face tracking, and models the third as a separate anchor. |
| Hits without a detected plane | See "Hit targets" above. |
| Anchors attached to a trackable | ARCore only. |

### Out of scope

Features that exist on one platform only, or differ too much to share a model:

- **ARCore only:** cloud anchors, recording and playback, scene semantics,
  image stabilisation, shared camera, terrain and rooftop anchors, streetscape
  geometry.
- **ARKit only:** world maps, collaboration, scene mesh, plane classification,
  people segmentation, body tracking, object scanning and detection, 3DoF and
  position-only configurations, world origin, tracked raycasts, coaching
  overlay, App Clip Codes, high-resolution stills.
- **Different models:** face tracking.
