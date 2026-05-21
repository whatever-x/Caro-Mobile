plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.cmp")
    id("caro.koin")
}

kotlin {
    android {
        namespace = "com.whatever.caro.composeApp"
    }

    sourceSets {
        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
            implementation(projects.core.viewmodel)
            implementation(projects.core.navigator)
            implementation(projects.core.model)
            implementation(projects.core.remote)
            implementation(projects.core.analytics)
            implementation(projects.core.crashlytics)
            api(projects.core.messaging)

            implementation(projects.feature.home)
            implementation(projects.feature.login)
            implementation(projects.feature.splash)
            implementation(projects.feature.profile)

            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.nav3)
        }
    }
}
