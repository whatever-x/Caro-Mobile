import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.internal.utils.getLocalProperty

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
}

kotlin {
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
            getLocalProperty("GOOGLE_WEB_CLIENT_ID") ?: error("CLIENT_ID를 찾을 수 없습니다"),
        )
    }
}
