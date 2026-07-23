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
    android { namespace = "com.whatever.caro.feature.learning" }
    sourceSets.commonMain.dependencies {
        implementation(projects.core.model)
        implementation(libs.jetbrains.androidx.navigation3.event)
    }
}
