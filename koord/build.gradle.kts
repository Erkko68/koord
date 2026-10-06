import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("koord-kmp-module")
}

kotlin {
    android {
        namespace = "io.github.erkko68.koord"
    }

    // Objective-C helpers that keep ARKit frames out of Kotlin's hands: src/nativeInterop/cinterop/arkit.def.
    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main").cinterops.create("arkit")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.arcore)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
