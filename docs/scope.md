# Scope of the common API

What Koord takes from the two platforms, what it leaves out, and why. Feature
names follow [platform-apis.md](platform-apis.md); the API itself is described
in [api.md](api.md).

Decided on 2026-10-05.

## In the API

| Area | Koord | ARCore | ARKit |
| :--- | :--- | :--- | :--- |
| Session lifecycle | `ArSession`: `configure`, `resume`, `pause`, `close` | `Session` | `ARSession` + `ARWorldTrackingConfiguration` |
| Display geometry | `ArSession.setDisplayGeometry` | `setDisplayGeometry` | passed per call on the ARKit side |
| Frames, pulled | `ArSession.update()`, `ArFrame` | `Session.update()` | latest frame from `ARSessionDelegate`, cached |
| Configuration | `ArConfig`: plane detection, light estimation, autofocus | `Config` | configuration properties |
| Camera | `ArCamera`: pose, view and projection matrices, intrinsics | `Camera` | `ARCamera` |
| Tracking state | `TrackingState`, `TrackingFailureReason` | `TrackingState`, `TrackingFailureReason` | `trackingState`, `trackingStateReason` |
| Anchors | `Anchor`, `ArSession.createAnchor`, `ArSession.anchors` | `Anchor` | `ARAnchor` |
| Planes | `Plane`, `ArSession.planes` | `Plane` | `ARPlaneAnchor` |
| Hit testing | `ArFrame.hitTest` (screen point or `Ray`), `HitTarget`, `HitResult` | `Frame.hitTest` | `ARRaycastQuery`, `ARSession.raycast` |
| Light estimate | `LightEstimate`: intensity, colour | `LightEstimate` pixel intensity, colour correction | `ARLightEstimate` ambient intensity, colour temperature |
| Camera image | `ArSession.createEngine`, `CameraBackground` | external OES texture in a GL context shared with Filament | the two planes of `ARFrame.capturedImage` as Metal textures imported into Filament |
| Errors | `ArException` | exceptions | `session(_:didFailWithError:)` |
| Availability | `ArAvailability`, platform `checkArAvailability` | `ArCoreApk.checkAvailability` | `ARConfiguration.isSupported` |

## Decisions where the platforms disagree

| Topic | Decision | Why |
| :--- | :--- | :--- |
| Transforms | `Mat4` from `filament-utils`, not a pose type | Filament consumes matrices; ARKit already uses them; ARCore's `Pose` converts with `toMatrix`. |
| Frame delivery | Pull, on both | It matches a render loop. ARKit's pushed frames are cached and returned by `update()`. |
| Session constructor and availability check | Platform-specific, no common declaration | Android needs a `Context`, iOS needs nothing. |
| Errors | One sealed `ArException`; on iOS thrown from the next `update()` | ARCore throws, ARKit calls a delegate. Throwing gives common code one path. |
| Hit targets | Detected planes only: inside their polygon, or extended without limit (`HitTarget`) | Both platforms hit a detected plane beyond its polygon natively (ARKit's `existingPlaneInfinite`, ARCore's unfiltered plane hits), and it is how a plain wall becomes usable from a small patch. ARKit's estimated planes and ARCore's instant placement, point and depth hits have no counterpart on the other side. |
| Merged planes | Hidden from `ArSession.planes` | ARKit removes them; ARCore keeps them with `getSubsumedBy` set. |
| Layered planes | On Android, a plane within 10 cm of a larger parallel plane that covers its centre is hidden from `ArSession.planes` and from hit tests | ARCore starts a plane on any flat patch and merges late, so one floor shows as stacked planes and low objects as planes of their own. ARKit holds these back. ARCore has no setting for it; filtering is what ARCore apps do. |
| Light | Normalised intensity and an RGB tint only | These are the only values both platforms report during world tracking. |
| Camera image | Koord draws it; the app does not get the texture | Android gives an OES texture tied to a GL context, iOS a YCbCr pixel buffer. A renderable hides both. The price is that the Filament engine must be created by the session. |
| Installing ARCore | Reported as `ArAvailability.NEEDS_INSTALL`; the app triggers it | It needs an `Activity` and has no iOS counterpart. |

## Left out

### Waiting on rendering

| Feature | Why it waits |
| :--- | :--- |
| Depth and occlusion | Needs the camera image drawn on both platforms first. Also LiDAR-only on iOS. |
| Raw camera image and image-to-screen transform | `CameraBackground` covers drawing it. Direct access to the pixels has no shared shape: an OES texture on Android, a YCbCr pixel buffer on iOS. |

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
