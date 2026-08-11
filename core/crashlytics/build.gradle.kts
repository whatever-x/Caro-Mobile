import com.whatever.caro.configureFirebaseCrashlytics

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
        iosTarget.configureFirebaseCrashlytics()
    }

    android {
        namespace = "com.whatever.caro.core.crashlytics"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)

            implementation(project.dependencies.platform(libs.firebase.bom.android))
            implementation(libs.firebase.crashlytics)
        }
    }
}
