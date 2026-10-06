rootProject.name = "Samples"

// Includes the main `koord` build, so the samples compile against the local
// library sources instead of fetching them from a repository.
//
// To run the samples standalone, remove this block: the dependencies then
// resolve from Maven Central at the version in `gradle/libs.versions.toml`.
includeBuild("../") {
    name = "koord"
    dependencySubstitution {
        substitute(module("io.github.erkko68.koord:koord")).using(project(":koord"))
        substitute(module("io.github.erkko68.koord:koord-filament")).using(project(":koord-filament"))
    }
}

pluginManagement {
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

include(":shared")
include(":androidApp")
