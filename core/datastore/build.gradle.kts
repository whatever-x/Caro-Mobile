plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    id("caro.kmp.test")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.datastore"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.datastore)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
