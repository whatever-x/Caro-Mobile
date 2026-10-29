import com.whatever.caro.configureGoogleMobileAds

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    id("caro.cmp")
    id("caro.kmp.test")
    alias(libs.plugins.kmp.spm)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.configureGoogleMobileAds() }
    android { namespace = "com.whatever.caro.core.ads" }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.date.time)
            implementation(libs.koin.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.google.mobile.ads)
            implementation(libs.google.ump)
        }
    }
}
