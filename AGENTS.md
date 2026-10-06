# AGENTS.md

Guidance for coding agents working in this repository.

## What this is

Koord is a Kotlin Multiplatform AR library for Android and iOS: one common API
over ARCore and ARKit. The core does not render and depends on no renderer;
`koord-filament` draws with [filament-kmp](https://github.com/Erkko68/filament-kmp).
It is early. Read these before changing the API:

- [docs/api.md](docs/api.md): the common API and its conventions
- [docs/scope.md](docs/scope.md): what the API covers, what it leaves out, and why
- [docs/platform-apis.md](docs/platform-apis.md): the ARCore vs ARKit inventory behind those decisions

## Layout

| Path | What |
| :--- | :--- |
| `koord/` | The library. `commonMain` holds the API, `androidMain` the ARCore side, `iosMain` the ARKit side. No Filament. |
| `koord-filament/` | The Filament integration: the engine for a session, `CameraBackground`, and the conversion to `filament-utils`. Depends on `koord`. |
| `build-logic/` | Convention plugin `koord-kmp-module`: targets, SDK levels, compiler flags. |
| `samples/` | A separate Gradle build with its own wrapper. It consumes `koord` and `koord-filament` by Maven coordinates, substituted with the local projects. |
| `docs/` | API, scope and platform notes. |

## Build and verify

The only tests cover the math in `koord` (`koord/src/commonTest`), and run on
the iOS simulator. Everything else is verified by building:

```sh
./gradlew assemble                             # both libraries, all targets
./gradlew :koord:iosSimulatorArm64Test         # the math tests (macOS only)
cd samples
./gradlew :androidApp:assembleDebug            # Android sample
./gradlew :shared:compileKotlinIosArm64        # iOS side of the sample (macOS only)
```

`samples/` needs `ANDROID_HOME` or its own `local.properties` with `sdk.dir`.
On Linux the build also needs `libc++-dev`, `libc++abi-dev` and `libgl-dev`,
for the material compiler it runs.

AR only runs on real devices, not on the emulator or the simulator. A green
build does not show that AR code works: say plainly what was built and what
was not run on a device.

CI runs two jobs on every pull request: `android` (libraries and Android
sample, on Linux) and `ios` (libraries, tests and the Kotlin side of the iOS
sample, on macOS).
Neither runs the Xcode build of `samples/iosApp`.

A third workflow, `Pages`, generates the API reference with Dokka
(`./gradlew :dokkaGenerate`, both modules) and publishes it to
<https://erkko68.github.io/koord/> on every push to `main`.

## Code conventions

- **Common API first.** Public API is declared in `commonMain`, as an `expect`
  class when it wraps a platform object and as plain common code otherwise.
  Platform files mirror the common path and are named `Name.android.kt` and
  `Name.ios.kt`.
- **KDoc on every public declaration**, naming the ARCore and ARKit
  counterparts and any behaviour that differs between them.
- **Same behaviour on both platforms.** When the platforms disagree, pick one
  behaviour, implement it on both, and record the decision in `docs/scope.md`.
  Do not expose a feature only one platform has.
- **No renderer in `koord`.** It must not depend on Filament or any other
  renderer; what is specific to one goes in its own module, as `koord-filament`.
- **Math types are Koord's own** (`Mat4`, `Float3`, `Float2`, `Ray` in
  `io.github.erkko68.koord.math`) and only carry values. Do not grow them into
  a math library: public operations belong to the renderer's types, reached
  through a conversion such as `Mat4.toFilament()`.
- **World space** is right-handed, Y-up, in metres. Screen points are viewport
  pixels with the origin at the top-left.
- **Keep it small.** No abstractions, options or dependencies that nothing
  uses yet. Versions go in `gradle/libs.versions.toml` (and
  `samples/gradle/libs.versions.toml` for the samples).
- **Materials** are compiled at build time by a Gradle task in `build-logic/`
  that runs filamat, and embedded as generated Kotlin under
  `koord-filament/build/`.
  The material is defined in that task
  (`GenerateCameraBackgroundMaterial.kt`), once per platform; nothing compiled
  is committed.
- **Never hold an `ARFrame` in Kotlin.** Kotlin/Native keeps Objective-C
  objects alive until its garbage collector runs, and ARKit stops delivering
  camera images when too many frames are held. Read a frame through the
  helpers in `koord/src/nativeInterop/cinterop/arkit.def`, which take the
  pointer `ArSession` retains for exactly one frame. The same goes for a Metal
  texture made from the camera image: see `cameraImage.def` in `koord-filament`.
- **Keep the docs true.** An API change updates `docs/api.md`; a scope decision
  updates `docs/scope.md`.

## Git and pull requests

- `main` is protected. Never push to it; every change goes through a pull
  request, and merged branches are deleted automatically.
- Branch names: `<type>/<short-description>`, for example `feat/android-arcore`
  or `fix/plane-polygon-order`.
- Commit messages and PR titles follow
  [Conventional Commits](https://www.conventionalcommits.org):
  `<type>(<optional scope>): <summary>`, summary in the imperative and lower
  case, no trailing period. Scopes in use: `android`, `ios`, `samples`.
- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`.
- One logical change per commit; each commit should build.
- Do not add AI co-author trailers to commits.
- Do not commit `local.properties`, build output or IDE files.
