plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin")
}

kotlin {
    android {
        namespace = "com.whatever.caro.feature.home"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
        }
    }

}
