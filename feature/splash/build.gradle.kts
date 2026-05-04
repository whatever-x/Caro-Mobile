plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin")
    id("caro.kmp.test")
    id("caro.kover")
}

kotlin {
    android {
        namespace = "com.whatever.caro.feature.splash"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.messaging)
            implementation(libs.bundles.moko)
        }
    }
}
