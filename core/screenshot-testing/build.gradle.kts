plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.kmp.test")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.screenshot.testing"
    }

    // Test infrastructure needs library variants, without the app framework exports.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.ui.test)
            api(libs.jetbrains.compose.resources)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            api(libs.compose.ui.test.junit4)
            api(libs.robolectric)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
        }
        androidHostTest.dependencies {
            runtimeOnly(libs.compose.ui.test.manifest)
            runtimeOnly(libs.junit.vintage.engine)
        }
        iosMain.dependencies {
            implementation(libs.roborazzi.compose.ios)
        }
    }
}
