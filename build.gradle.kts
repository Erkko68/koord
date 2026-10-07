plugins {
    id("koord-docs")
}

// One API reference for every module, published to GitHub Pages by .github/workflows/pages.yml: ./gradlew :dokkaGenerate
dokka {
    moduleName = "koord"
}

dependencies {
    dokka(project(":koord"))
    dokka(project(":koord-filament"))
}
