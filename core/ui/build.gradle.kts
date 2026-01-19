plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.cmp")
}

kotlin {
    android {
        namespace = "com.whatever.caro.ui"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
        }
    }

}
