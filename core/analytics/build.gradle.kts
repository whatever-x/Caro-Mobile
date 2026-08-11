import com.whatever.caro.configureFirebaseAnalytics

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    alias(libs.plugins.kmp.spm)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.configureFirebaseAnalytics()
    }

    android {
        namespace = "com.whatever.caro.core.analytics"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)

            implementation(project.dependencies.platform(libs.firebase.bom.android))
            implementation(libs.firebase.analytics)
        }
    }
}
