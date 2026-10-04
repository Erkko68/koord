plugins {
    id("koord-kmp-module")
}

kotlin {
    android {
        namespace = "io.github.erkko68.koord"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.filament)
        }
        androidMain.dependencies {
            implementation(libs.arcore)
        }
    }
}
