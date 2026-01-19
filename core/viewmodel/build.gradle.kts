plugins {
    id("caro.kmp")
    id("caro.kmp.android")
}

kotlin {
    android {
        namespace = "com.whatever.caro.viewmodel"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel)
        }
    }
}
