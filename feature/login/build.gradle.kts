@file:OptIn(ExperimentalSpmForKmpFeature::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec
import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import org.jetbrains.compose.internal.utils.getLocalProperty
import java.net.URI

plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin")
    id("caro.kmp.test")
    id("caro.kover")
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.kmp.spm)
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.swiftPackageConfig(cinteropName = "GoogleLoginBridge") {
            customPackageSourcePath = "../../iosApp"
            minIos = "15.0"

            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/google/GoogleSignIn-iOS"),
                    version = "9.1.0",
                    products = { add("GoogleSignIn", "GoogleSignInSwift") }
                )
            }
        }
    }

    android {
        namespace = "com.whatever.caro.feature.login"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.credentials.credentials)
            implementation(libs.androidx.credentials.auth)
            implementation(libs.android.google.id)
        }
        commonMain.dependencies {
            implementation(projects.core.model)
        }
    }
}

buildkonfig {
    packageName = "com.whatever.caro.feature.login.generated"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "GOOGLE_WEB_CLIENT_ID",
            getLocalProperty("GOOGLE_WEB_CLIENT_ID") ?: error("GOOGLE_WEB_CLIENT_ID를 찾을 수 없습니다"),
        )
    }
}
