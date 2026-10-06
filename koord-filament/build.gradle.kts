import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("koord-kmp-module")
}

// The material compiler, run on the build machine by the generate…CameraBackgroundMaterial tasks.
val filamat by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}
dependencies {
    filamat(libs.filamat.jvm)
}

val generateAndroidCameraBackgroundMaterial by tasks.registering(GenerateCameraBackgroundMaterial::class) {
    compilerClasspath.from(filamat)
    yCbCr = false
    outputDir = layout.buildDirectory.dir("generated/cameraBackgroundMaterial/android")
}
val generateIosCameraBackgroundMaterial by tasks.registering(GenerateCameraBackgroundMaterial::class) {
    compilerClasspath.from(filamat)
    yCbCr = true
    outputDir = layout.buildDirectory.dir("generated/cameraBackgroundMaterial/ios")
}

kotlin {
    android {
        namespace = "io.github.erkko68.koord.filament"
    }

    // Objective-C helpers that turn the camera image into Metal textures: src/nativeInterop/cinterop/cameraImage.def.
    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main").cinterops.create("cameraImage")
    }

    sourceSets {
        androidMain {
            kotlin.srcDir(generateAndroidCameraBackgroundMaterial)
        }
        iosMain {
            kotlin.srcDir(generateIosCameraBackgroundMaterial)
        }
        commonMain.dependencies {
            api(project(":koord"))
            api(libs.filament)
            api(libs.filament.utils)
        }
    }
}
