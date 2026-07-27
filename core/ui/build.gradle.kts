plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.mp)
            implementation(libs.compottie.lite)
        }
    }
}
