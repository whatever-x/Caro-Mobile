plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.koin")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.viewmodel"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel)
        }
    }
}
