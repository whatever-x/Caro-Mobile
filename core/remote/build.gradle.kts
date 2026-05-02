import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.internal.utils.getLocalProperty

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.koin")
    id("caro.kotlin.serialization")
    id("caro.kmp.test")
    alias(libs.plugins.build.konfig)
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
            implementation(projects.core.model)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.bundles.ktor.client.plugin)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

buildkonfig {
    packageName = "com.whatever.caro.core.remote.generated"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "SERVER_BASE_URL", "")
        buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
    }

    targetConfigs("qa") {
        create("android") {
            buildConfigField(
                FieldSpec.Type.STRING,
                "SERVER_BASE_URL",
                getLocalProperty("CARO_QA_SERVER") ?: error("CARO_BASE_URL을 찾을 수 없습니다."),
            )
            buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
        }
    }

    targetConfigs("dev") {
        create("android") {
            buildConfigField(
                FieldSpec.Type.STRING,
                "SERVER_BASE_URL",
                getLocalProperty("CARO_DEV_SERVER") ?: error("CARO_BASE_URL을 찾을 수 없습니다."),
            )
            buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
        }
    }

    targetConfigs("prod") {
        create("android") {
            buildConfigField(
                FieldSpec.Type.STRING,
                "SERVER_BASE_URL",
                getLocalProperty("CARO_PROD_SERVER") ?: error("CARO_BASE_URL을 찾을 수 없습니다."),
            )
            buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "false")
        }
    }
}
