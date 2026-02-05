plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    id("caro.kmp.test")
    id("caro.kover")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.remote)
            implementation(projects.core.model)

            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
