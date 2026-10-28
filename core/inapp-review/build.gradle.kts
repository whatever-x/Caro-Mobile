plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.cmp")
    id("caro.koin")
    id("caro.kmp.test")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.inappreview"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(libs.koin.compose)
        }
        androidMain.dependencies {
            implementation(libs.play.review)
            implementation(libs.androidx.activity.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.android)
        }
        androidHostTest.dependencies {
            implementation(libs.robolectric)
            implementation(libs.junit.vintage.engine)
        }
    }
}
