plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.cmp")
    id("caro.kmp.test")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.designsystem"
    }

    sourceSets {
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.compose.ui.test.junit4)
                runtimeOnly(libs.compose.ui.test.manifest)
                runtimeOnly(libs.junit.vintage.engine)
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = auto
}
