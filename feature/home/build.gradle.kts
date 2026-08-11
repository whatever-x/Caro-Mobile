plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin")
    id("caro.kmp.test")
    id("caro.kover")
}

kotlin {
    android {
        namespace = "com.whatever.caro.feature.home"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.util)
            implementation(libs.compottie)
            implementation(libs.compottie.lite)
            implementation(libs.compottie.resources)
        }
    }
}
