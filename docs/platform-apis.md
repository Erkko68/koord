# Platform AR APIs: ARCore vs ARKit

Inventory of what the two platforms offer, which the common API was designed
from. The Koord API itself is described in [api.md](api.md); what it takes from
this inventory and what it leaves out is in [scope.md](scope.md).

Sources: the public classes of `com.google.ar:core` 1.56.0 and the ARKit
headers in the iOS 26.2 SDK. Names are the platform's own. ARKit names are the
Objective-C ones, which is how Kotlin/Native exposes them in `platform.ARKit`.

The **Common** column is an estimate of how well the two sides line up:

- **yes** — both platforms offer it in a comparable shape
- **partial** — both offer it, but the model differs enough to need a decision
- **ARCore** / **ARKit** — only that platform has it

## 1. Availability and install

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Is AR supported on this device | `ArCoreApk.checkAvailability()`, `checkAvailabilityAsync()` | `ARConfiguration.isSupported` (per configuration class) | yes |
| Install or update the AR runtime | `ArCoreApk.requestInstall()` | — (part of the OS) | ARCore |
| Camera permission | Android runtime permission | `NSCameraUsageDescription`, system prompt on first run | partial |

## 2. Session lifecycle

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Create | `Session(context)`, `Session(context, features)` | `ARSession()` | yes |
| Start / resume | `resume()` | `runWithConfiguration(_:options:)` | yes |
| Pause | `pause()` | `pause()` | yes |
| Destroy | `close()` | released by ARC | partial |
| Reconfigure while running | `configure(config)` | `runWithConfiguration` again, with reset options | yes |
| Reset tracking / remove anchors | recreate the session | run options `ResetTracking`, `RemoveExistingAnchors`, `ResetSceneReconstruction` | partial |
| Get the next frame | `update()` — **pull**, blocking or latest, set by `Config.UpdateMode` | `ARSessionDelegate.session(_:didUpdateFrame:)` — **push**; `currentFrame` for pull | partial |
| Interruption callbacks | — (driven by `pause` / `resume`) | `sessionWasInterrupted`, `sessionInterruptionEnded`, `sessionShouldAttemptRelocalization` | ARKit |
| Errors | exceptions (`CameraNotAvailableException`, `Unavailable…Exception`, …) | `session(_:didFailWithError:)`, `ARError` codes | partial |
| Move the world origin | — | `setWorldOrigin(_:)` | ARKit |
| Display rotation and viewport | `setDisplayGeometry(rotation, w, h)` | passed per call (`…ForOrientation:viewportSize:`) | partial |

## 3. Configuration

ARCore has one `Config` with a mode per feature. ARKit has one configuration
class per tracking type, each with its own properties.

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| World tracking (6DoF) | default `Session` | `ARWorldTrackingConfiguration` | yes |
| Orientation only (3DoF) | — | `AROrientationTrackingConfiguration` | ARKit |
| Position only, no camera image | — | `ARPositionalTrackingConfiguration` | ARKit |
| Front camera / face tracking | `Session.Feature.FRONT_CAMERA` + `AugmentedFaceMode` | `ARFaceTrackingConfiguration` | partial |
| Image tracking only | — (images tracked inside world tracking) | `ARImageTrackingConfiguration` | partial |
| Body tracking | — | `ARBodyTrackingConfiguration` | ARKit |
| Object scanning | — | `ARObjectScanningConfiguration` | ARKit |
| Geospatial | `Config.GeospatialMode` | `ARGeoTrackingConfiguration` | partial |
| Plane detection | `PlaneFindingMode` (horizontal, vertical, both, off) | `planeDetection` (horizontal, vertical) | yes |
| Light estimation | `LightEstimationMode` (ambient intensity, environmental HDR, off) | `lightEstimationEnabled`, `environmentTexturing` | partial |
| Depth | `DepthMode` (automatic, raw depth only) | `frameSemantics` (`sceneDepth`, `smoothedSceneDepth`) | partial |
| Focus | `FocusMode` (fixed, auto) | `autoFocusEnabled` | yes |
| Camera resolution / frame rate | `CameraConfig`, `CameraConfigFilter`, `getSupportedCameraConfigs()` | `ARVideoFormat`, `supportedVideoFormats`, `videoFormat` | yes |
| World alignment | gravity only | `worldAlignment` (gravity, gravity and heading, camera) | partial |
| Flash / torch | `FlashMode` | via `configurableCaptureDeviceForPrimaryCamera` | partial |
| Image stabilisation (EIS) | `ImageStabilizationMode` | — | ARCore |
| Instant placement | `InstantPlacementMode` | — (raycast against estimated planes covers it) | partial |
| Semantic segmentation of the scene | `SemanticMode` | — | ARCore |
| People segmentation | — | `frameSemantics` (`personSegmentation`, `personSegmentationWithDepth`) | ARKit |
| Scene reconstruction (mesh) | — | `sceneReconstruction` (LiDAR devices) | ARKit |
| Feature support queries | `isDepthModeSupported()`, `isGeospatialModeSupported()`, … | `supportsFrameSemantics`, `supportsSceneReconstruction`, … | yes |

## 4. Frame

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Timestamp | `Frame.getTimestamp()` (ns) | `ARFrame.timestamp` (s) | yes |
| Camera for this frame | `Frame.getCamera()` | `ARFrame.camera` | yes |
| Camera image, GPU | OES texture: `Session.setCameraTextureName(s)`, `Frame.getCameraTextureName()`; or `Frame.getHardwareBuffer()` | `ARFrame.capturedImage` (`CVPixelBuffer`, bi-planar YCbCr) | partial |
| Camera image, CPU | `Frame.acquireCameraImage()` (YUV_420_888) | `ARFrame.capturedImage` (same buffer) | partial |
| Image-to-screen transform | `Frame.transformCoordinates2d()`, `hasDisplayGeometryChanged()` | `ARFrame.displayTransformForOrientation(_:viewportSize:)` | yes |
| Anchors updated this frame | `Frame.getUpdatedAnchors()` | delegate `didAddAnchors`, `didUpdateAnchors`, `didRemoveAnchors`; `ARFrame.anchors` | partial |
| Trackables updated this frame | `Frame.getUpdatedTrackables(type)` | same anchor callbacks | partial |
| Light estimate | `Frame.getLightEstimate()` | `ARFrame.lightEstimate` | yes |
| Feature points | `Frame.acquirePointCloud()` | `ARFrame.rawFeaturePoints` | yes |
| Depth image | `acquireDepthImage16Bits()`, `acquireRawDepthImage16Bits()`, `acquireRawDepthConfidenceImage()` | `sceneDepth`, `smoothedSceneDepth` (`ARDepthData`: `depthMap`, `confidenceMap`) | partial |
| Hit test from this frame | `Frame.hitTest(x, y)`, `hitTest(origin, direction)` | `ARFrame.raycastQueryFromPoint(_:allowingTarget:alignment:)` | yes |
| Camera metadata | `getImageMetadata()`, `getAndroidCameraTimestamp()` | `exifData`, `ARCamera.exposureDuration`, `exposureOffset` | partial |
| Mapping quality | `Session.estimateFeatureMapQualityForHosting()` | `ARFrame.worldMappingStatus` | partial |
| Camera grain | — | `cameraGrainTexture`, `cameraGrainIntensity` | ARKit |
| High-resolution still | — | `ARSession.captureHighResolutionFrame…` | ARKit |

## 5. Camera

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Pose in world space | `Camera.getPose()`, `getDisplayOrientedPose()` | `ARCamera.transform` | yes |
| View matrix | `getViewMatrix()` | `viewMatrixForOrientation(_:)` | yes |
| Projection matrix | `getProjectionMatrix(near, far)` | `projectionMatrix`, `projectionMatrixForOrientation(_:viewportSize:zNear:zFar:)` | yes |
| Intrinsics | `getImageIntrinsics()`, `getTextureIntrinsics()` (`CameraIntrinsics`: focal length, principal point, dimensions) | `intrinsics` (3×3), `imageResolution` | yes |
| Tracking state | `getTrackingState()` (tracking, paused, stopped) | `trackingState` (normal, limited, not available) | yes |
| Tracking failure reason | `getTrackingFailureReason()` (bad state, insufficient light, excessive motion, insufficient features, camera unavailable) | `trackingStateReason` (initializing, excessive motion, insufficient features, relocalizing) | yes |
| World-to-screen | — (use the matrices) | `projectPoint(_:orientation:viewportSize:)` | partial |
| Screen-to-world on a plane | — (use hit test) | `unprojectPoint(_:ontoPlaneWithTransform:…)` | partial |

## 6. Pose and math

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Rigid transform | `Pose` (translation + quaternion; `compose`, `inverse`, `transformPoint`, `toMatrix`, `makeInterpolated`) | `simd_float4x4` | partial |
| Conventions | right-handed, Y-up, metres, column-major | right-handed, Y-up, metres, column-major | yes |

## 7. Anchors

In ARCore, anchors are separate objects attached to trackables. In ARKit, every
detected thing *is* an anchor (`ARPlaneAnchor`, `ARImageAnchor`, …).

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Create at a pose | `Session.createAnchor(pose)` | `ARSession.addAnchor(ARAnchor(transform:))` | yes |
| Create on a hit | `HitResult.createAnchor()` | build an `ARAnchor` from `ARRaycastResult.worldTransform` | yes |
| Attach to a trackable | `Trackable.createAnchor(pose)` | — | ARCore |
| Remove | `Anchor.detach()` | `ARSession.removeAnchor(_:)` | yes |
| Pose | `Anchor.getPose()` | `ARAnchor.transform` | yes |
| Tracking state | `Anchor.getTrackingState()` | `ARTrackable.isTracked` (some anchor types only) | partial |
| Identity | object identity | `ARAnchor.identifier` (UUID), `name` | partial |
| List all | `Session.getAllAnchors()` | `ARFrame.anchors` | yes |

## 8. Trackables

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Planes | `Plane`: `getCenterPose`, `getExtentX/Z`, `getPolygon`, `getType`, `getSubsumedBy`, `isPoseInPolygon` | `ARPlaneAnchor`: `center`, `planeExtent`, `geometry` (`ARPlaneGeometry`), `alignment` | yes |
| Plane classification | — | `ARPlaneAnchor.classification` (wall, floor, ceiling, table, seat, door, window) | ARKit |
| Feature points | `Point`, `PointCloud` (`getPoints`, `getIds`) | `ARPointCloud` (`points`, `identifiers`) | yes |
| Images | `AugmentedImageDatabase`, `AugmentedImage` (`getCenterPose`, `getExtentX/Z`, `getName`, `getTrackingMethod`) | `ARReferenceImage`, `ARImageAnchor` (`referenceImage`, `estimatedScaleFactor`) | yes |
| Faces | `AugmentedFace` (mesh, region poses) | `ARFaceAnchor` (`geometry`, `blendShapes`, eye transforms, `lookAtPoint`) | partial |
| Depth points | `DepthPoint` | — | ARCore |
| Instant placement points | `InstantPlacementPoint` | — | ARCore |
| Scene mesh | — | `ARMeshAnchor`, `ARMeshGeometry` (LiDAR) | ARKit |
| 3D objects | — | `ARReferenceObject`, `ARObjectAnchor` | ARKit |
| Bodies | — | `ARBodyAnchor`, `ARSkeleton3D`, `ARBody2D` | ARKit |
| Environment probes | — (cube map comes from the light estimate) | `AREnvironmentProbeAnchor` (`environmentTexture`, `extent`) | partial |
| App Clip Codes | — | `ARAppClipCodeAnchor` | ARKit |
| List all | `Session.getAllTrackables(type)` | `ARFrame.anchors`, filtered by class | yes |

## 9. Hit testing

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| From a screen point | `Frame.hitTest(x, y)` | `ARFrame.raycastQueryFromPoint` + `ARSession.raycast(_:)` | yes |
| From a world-space ray | `Frame.hitTest(origin, direction)` | `ARRaycastQuery(origin:direction:allowingTarget:alignment:)` | yes |
| What can be hit | planes, points, depth points, instant placement points, images | `ARRaycastTarget`: existing plane geometry, existing plane infinite, estimated plane | partial |
| Result | `HitResult`: `getHitPose`, `getDistance`, `getTrackable` | `ARRaycastResult`: `worldTransform`, `target`, `targetAlignment`, `anchor` | yes |
| Result that keeps refining | — | `ARSession.trackedRaycast(_:updateHandler:)`, `ARTrackedRaycast` | ARKit |
| Without detected surfaces | `Frame.hitTestInstantPlacement(x, y, distance)` | raycast with `estimatedPlane` target | partial |

## 10. Light estimation

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Ambient intensity | `LightEstimate.getPixelIntensity()` | `ARLightEstimate.ambientIntensity` (lumens) | partial |
| Colour | `getColorCorrection()` (RGB gains) | `ambientColorTemperature` (kelvin) | partial |
| Main directional light | `getEnvironmentalHdrMainLightDirection()`, `…MainLightIntensity()` | `ARDirectionalLightEstimate.primaryLightDirection`, `primaryLightIntensity` (face tracking only) | partial |
| Spherical harmonics | `getEnvironmentalHdrAmbientSphericalHarmonics()` | `ARDirectionalLightEstimate.sphericalHarmonicsCoefficients` (face tracking only) | partial |
| Reflection cube map | `acquireEnvironmentalHdrCubeMap()` | `AREnvironmentProbeAnchor.environmentTexture` | partial |
| Validity | `getState()` | nullable `lightEstimate` | yes |

## 11. Depth and occlusion

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Depth map | `Frame.acquireDepthImage16Bits()` (works without a depth sensor) | `ARFrame.sceneDepth` (LiDAR only) | partial |
| Smoothed depth | default depth image is already smoothed | `smoothedSceneDepth` | partial |
| Raw depth and confidence | `acquireRawDepthImage16Bits()`, `acquireRawDepthConfidenceImage()` | `ARDepthData.confidenceMap` | partial |
| People occlusion | — | `segmentationBuffer`, `estimatedDepthData`, `ARMatteGenerator` | ARKit |
| Front camera depth | — | `capturedDepthData` (TrueDepth) | ARKit |

## 12. Persistence and sharing

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Save and reload a map locally | — | `ARWorldMap`, `getCurrentWorldMapWithCompletionHandler`, `initialWorldMap` | ARKit |
| Cloud-hosted anchors | `hostCloudAnchorAsync()`, `resolveCloudAnchorAsync()` | — (ARCore Cloud Anchors has a separate iOS SDK) | ARCore |
| Live multi-user | — | `collaborationEnabled`, `ARCollaborationData`, `ARParticipantAnchor` | ARKit |
| Record and replay a session | `startRecording()`, `setPlaybackDatasetUri()` | — (Xcode / Reality Composer only) | ARCore |

## 13. Geospatial

| Capability | ARCore | ARKit | Common |
| :--- | :--- | :--- | :--- |
| Availability at a location | `checkVpsAvailabilityAsync()` | `ARGeoTrackingConfiguration.checkAvailabilityAtCoordinate` | yes |
| Device geo pose | `Earth.getCameraGeospatialPose()` | `ARSession.getGeoLocationForPoint` | partial |
| Anchor at lat/long/altitude | `Earth.createAnchor()` | `ARGeoAnchor(coordinate:altitude:)` | yes |
| Terrain and rooftop anchors | `resolveAnchorOnTerrainAsync()`, `resolveAnchorOnRooftopAsync()` | — | ARCore |
| Building and terrain geometry | `StreetscapeGeometry` | — | ARCore |
| State | `Earth.getEarthState()`, `getTrackingState()` | `ARGeoTrackingStatus` | yes |

## 14. Platform-only extras

| Capability | ARCore | ARKit |
| :--- | :--- | :--- |
| Share the camera with Camera2 | `Session.createForSharedCamera()`, `SharedCamera` | — |
| Onboarding UI | — | `ARCoachingOverlayView` |
| Built-in renderers | — | `ARSCNView`, `ARSKView`, RealityKit `ARView` |
| Custom per-frame data in recordings | `Frame.recordTrackData()`, `getUpdatedTrackData()` | — |
| Audio capture | — | `providesAudioData` |
