import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.whatever.caro.configureGoogleSignIn
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
    alias(libs.plugins.kmp.spm)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.configureGoogleSignIn()
    }

    android {
        namespace = "com.whatever.caro.feature.login"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.credentials.credentials)
            implementation(libs.androidx.credentials.auth)
            implementation(libs.android.google.id)
            implementation(project.dependencies.platform(libs.firebase.bom.android))
            implementation(libs.firebase.auth)
        }
        commonMain.dependencies {
            implementation(projects.core.model)
        }
    }
}

buildkonfig {
    packageName = "com.whatever.caro.feature.login.generated"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "GOOGLE_WEB_CLIENT_ID", "")
    }

    listOf(
        "devDebug" to "DEV",
        "devRelease" to "DEV",
        "qaDebug" to "QA",
        "qaRelease" to "QA",
        "prodDebug" to "PROD",
        "prodRelease" to "PROD",
    ).forEach { (variant, flavor) ->
        targetConfigs(variant) {
            create("android") {
                val flavorProperty = "GOOGLE_WEB_CLIENT_ID_$flavor"
                buildConfigField(
                    FieldSpec.Type.STRING,
                    "GOOGLE_WEB_CLIENT_ID",
                    getLocalProperty(flavorProperty)
                        ?: getLocalProperty("GOOGLE_WEB_CLIENT_ID")
                        ?: error("${flavorProperty}를 찾을 수 없습니다."),
                )
            }
        }
    }
}
