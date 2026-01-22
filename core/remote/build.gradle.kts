plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin.annotation")
    id("caro.kotlin.serialization")
}

kotlin {
    android {
        namespace = "com.whatever.caro.core.remote"
    }

    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.bundles.ktor.client.plugin)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
