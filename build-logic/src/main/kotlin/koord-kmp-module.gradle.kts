import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.dokka")
}

val libs = the<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

// API reference, published to GitHub Pages by .github/workflows/pages.yml.
dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory = rootDir
            remoteUrl("https://github.com/Erkko68/koord/tree/main")
            remoteLineSuffix = "#L"
        }
    }
}

kotlin {
    // SDK levels single-sourced from gradle/libs.versions.toml; each module sets its own namespace.
    android {
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk     = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // The common API is expect classes, which are still Beta.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
