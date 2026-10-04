# Spike: camera passthrough behind a Filament scene

Status: **not started** — the repo is set up, the spike code is not written yet.

## Goal

One spike. Throwaway code: no library API, no abstractions. The code is written
in the `koord` module and the sample apps import it like any other library. Prove that camera passthrough renders behind a Filament scene on
both platforms.

**Deliverable:** a camera feed with a spinning cube on top of it, running on a
real Android device and a real iPhone. Nothing else.

## Stack

- `io.github.erkko68.filament:filament` 0.7.1 in `koord`; `filament-compose`
  0.7.1 in the samples (Maven Central).
- Android: ARCore (`com.google.ar:core` 1.56.0).
- iOS: ARKit. It ships in Kotlin/Native's bundled platform frameworks
  (`platform.ARKit`), so no cinterop is needed.

## Known landmines

Don't rediscover these.

### 1. Android backend — decide this first

ARCore hands you a `GL_TEXTURE_EXTERNAL_OES` texture. That pins the AR path to
Filament's **OpenGL ES** backend; Vulkan won't take it. Resolve the backend
choice before anything else, because it constrains everything downstream.

### 2. iOS camera image is YCbCr, not RGB

`ARFrame.capturedImage` is a bi-planar YCbCr `CVPixelBuffer`. It needs a `matc`
material that samples both planes and converts to RGB. There is no Android
counterpart to copy.

### 3. Frame model: pull on both platforms

ARCore is pull (`session.update()` on the render thread). ARKit is push
(`ARSessionDelegate`). On iOS, cache the latest `ARFrame` and read it as pull,
so both platforms fit the Filament render loop the same way.

### 4. Camera

Per frame: `camera.setCustomProjection()` from the device intrinsics and
`camera.setModelMatrix()` from the tracked pose. Both platforms are Y-up,
right-handed, column-major, so this part is plumbing.

### 5. Transparent clear

The Filament view must clear to transparent, or the passthrough won't show.

## Checked against filament-kmp 0.7.1

Read from the published sources on 2026-10-04. Nothing here has been run yet.

| Need | In 0.7.1 |
| :--- | :--- |
| Force the GL backend | `Engine.create(backend, sharedContext)`; in Compose, `rememberFilamentEngine(backend)` |
| Share an EGL context with ARCore | `Engine.create(sharedContext = …)` — but the Compose engine helper takes no shared context |
| Wrap ARCore's GL texture | `Texture.Builder.import(id)` with an external sampler |
| Stream-based external textures | `Stream`, `Texture.setExternalStream()` |
| Camera | `Camera.setCustomProjection()`, `Camera.setModelMatrix()` |
| Transparent clear | `FilamentView(transparent = true)` sets the blend mode, an alpha-0 clear and a transparent swap chain |
| `Texture.setExternalImage()` | **Not bound** |

Two things follow:

- **Android:** `filament-compose` creates its own engine with no shared
  context. If ARCore's texture has to live in a context shared with Filament,
  the Android half of the spike has to drive `Engine` / `SwapChain` directly
  instead of going through `FilamentView`.
- **iOS:** upstream Filament does have an ARKit sample,
  [`ios/samples/hello-ar`](https://github.com/google/filament/tree/main/ios/samples/hello-ar).
  It passes the `CVPixelBuffer` to a `SAMPLER_EXTERNAL` texture through
  `Texture::setExternalImage()` and lets the Metal backend handle the YCbCr
  planes. That call isn't bound in filament-kmp 0.7.1, so the choice is between
  binding it upstream and writing the two-plane material from landmine 2.

## Out of scope

Anchors, raycasting, plane detection, lighting estimation, occlusion, a public
API, Compose wrappers, publishing. Later.
