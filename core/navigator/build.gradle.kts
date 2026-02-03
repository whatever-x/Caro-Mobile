plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.kotlin.serialization")
    id("caro.koin")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.navigator"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.androidx.navigation3.ui)
        }
    }
}
