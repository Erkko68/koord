plugins {
    id("koord-kmp-module")
}

// The material compiler, run on the build machine by generateCameraBackgroundMaterial.
val filamat by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}
dependencies {
    filamat(libs.filamat.jvm)
}

val generateCameraBackgroundMaterial by tasks.registering(GenerateCameraBackgroundMaterial::class) {
    compilerClasspath.from(filamat)
    outputDir = layout.buildDirectory.dir("generated/cameraBackgroundMaterial")
}

kotlin {
    android {
        namespace = "io.github.erkko68.koord"
    }

    sourceSets {
        androidMain {
            kotlin.srcDir(generateCameraBackgroundMaterial)
        }
        commonMain.dependencies {
            api(libs.filament)
            api(libs.filament.utils)
        }
        androidMain.dependencies {
            implementation(libs.arcore)
        }
    }
}
