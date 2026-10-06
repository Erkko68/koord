plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.android.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    // Only to compile against: the task runs it in a worker process, on the classpath its project gives it.
    compileOnly(libs.filamat.jvm)
}
