import com.whatever.caro.configureFirebaseMessaging

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    id("caro.kmp.test")
    alias(libs.plugins.kmp.spm)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.configureFirebaseMessaging()
    }

    android {
        namespace = "com.whatever.caro.core.messaging"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)

            implementation(project.dependencies.platform(libs.firebase.bom.android))
            implementation(libs.firebase.fcm)
        }
    }
}
