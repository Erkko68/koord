// Not "koord": with the same group and name as the :koord module, Gradle takes a dependency
// on that module for the root project itself and drops it, which left it out of the API reference.
rootProject.name = "koord-root"

pluginManagement {
    // Convention plugins live in the build-logic included build (not buildSrc), so
    // editing them doesn't invalidate the whole main build's task graph.
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":koord")
include(":koord-filament")
