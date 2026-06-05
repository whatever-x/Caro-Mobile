import io.github.frankois944.spmForKmp.swiftPackageConfig
import java.net.URI

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    alias(libs.plugins.kmp.spm)
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.swiftPackageConfig {
            minIos = "15.0"

            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/firebase/firebase-ios-sdk"),
                    version = "12.14.0",
                    products = {
                        add("FirebaseCrashlytics", exportToKotlin = true)
                    },
                )
            }
        }
    }

    android {
        namespace = "com.whatever.caro.core.crashlytics"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)

            implementation(project.dependencies.platform(libs.firebase.bom.android))
            implementation(libs.firebase.crashlytics)
        }
    }
}
