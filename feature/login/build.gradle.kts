plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin.annotation")
}

kotlin {
    android {
        namespace = "com.whatever.caro.feature.login"
    }

    sourceSets {
        commonMain.dependencies {

        }
    }

}
