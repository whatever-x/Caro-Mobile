plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.kotlin.serialization")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.model"
    }

    sourceSets {
        commonMain.dependencies {
            compileOnly(libs.compose.stable.marker)
        }
        nativeMain.dependencies {
            api(libs.compose.stable.marker)
        }
    }

}
