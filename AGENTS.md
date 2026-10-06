# AGENTS.md

Guidance for coding agents working in this repository.

## What this is

Koord is a Kotlin Multiplatform AR library for Android and iOS: one common API
over ARCore and ARKit, with [filament-kmp](https://github.com/Erkko68/filament-kmp)
for rendering. It is early. Read these before changing the API:

- [docs/api.md](docs/api.md): the common API and its conventions
- [docs/scope.md](docs/scope.md): what the API covers, what it leaves out, and why
- [docs/platform-apis.md](docs/platform-apis.md): the ARCore vs ARKit inventory behind those decisions

## Layout

| Path | What |
| :--- | :--- |
| `koord/` | The library. `commonMain` holds the API, `androidMain` the ARCore side, `iosMain` the ARKit side. |
| `build-logic/` | Convention plugin `koord-kmp-module`: targets, SDK levels, compiler flags. |
| `samples/` | A separate Gradle build with its own wrapper. It consumes `koord` by Maven coordinates, substituted with the local project. |
| `docs/` | API, scope and platform notes. |

## Build and verify

There are no tests yet. A change is verified by building:

```sh
./gradlew :koord:assemble                      # library, all targets
cd samples
./gradlew :androidApp:assembleDebug            # Android sample
./gradlew :shared:compileKotlinIosArm64        # iOS side of the sample (macOS only)
```

`samples/` needs `ANDROID_HOME` or its own `local.properties` with `sdk.dir`.

AR only runs on real devices, not on the emulator or the simulator. A green
build does not show that AR code works: say plainly what was built and what
was not run on a device.

CI runs two jobs on every pull request: `android` (library and Android sample,
on Linux) and `ios` (library and the Kotlin side of the iOS sample, on macOS).
Neither runs the Xcode build of `samples/iosApp`.

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
- **Math types come from `filament-utils`** (`Mat4`, `Float3`, `Float2`, `Ray`).
  Do not add new vector or matrix types.
- **World space** is right-handed, Y-up, in metres. Screen points are viewport
  pixels with the origin at the top-left.
- **Keep it small.** No abstractions, options or dependencies that nothing
  uses yet. Versions go in `gradle/libs.versions.toml` (and
  `samples/gradle/libs.versions.toml` for the samples).
- **Materials** are compiled at build time by a Gradle task in `build-logic/`
  that runs filamat, and embedded as generated Kotlin under `koord/build/`.
  The material is defined in that task
  (`GenerateCameraBackgroundMaterial.kt`); nothing compiled is committed.
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
