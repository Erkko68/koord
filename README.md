# Koord

Kotlin Multiplatform AR for Android and iOS, built on
[filament-kmp](https://github.com/Erkko68/filament-kmp). ARCore on Android,
ARKit on iOS, Filament for rendering on both.

> [!WARNING]
> **Provisional.** There is no library yet. The repo currently holds the setup
> for one spike: camera passthrough behind a Filament scene. See
> [docs/spike-camera-passthrough.md](docs/spike-camera-passthrough.md).

## Layout

| Path | What |
| :--- | :--- |
| [koord](koord) | The library (`:koord`, `io.github.erkko68.koord:koord`) — the spike is developed here |
| [build-logic](build-logic) | Convention plugins; `koord-kmp-module` sets targets, SDK levels and JVM target |
| [samples](samples) | A separate Gradle build that consumes `koord` by its Maven coordinates |
| [docs](docs) | Notes |

`samples/` works like a normal consumer of the library. Its
[settings.gradle.kts](samples/settings.gradle.kts) includes the root build and
substitutes `io.github.erkko68.koord:koord` with the local `:koord` project, so
the samples always compile against the sources in this checkout.

## Requirements

- JDK 21 (the Gradle daemon toolchain downloads it if missing)
- Android: an [ARCore-supported device](https://developers.google.com/ar/devices), minSdk 24
- iOS: Xcode, an ARKit-capable iPhone on iOS 18.2+

AR needs real devices; the emulator and simulator won't do.

## Running

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
