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
    alias(libs.plugins.ksp)
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

            implementation(libs.ktorfit.lib)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspIosX64", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
}

kotlin.sourceSets.named("commonMain").configure {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

tasks
    .matching {
        (it.name.startsWith("compile") || it.name.startsWith("ksp")) &&
            it.name != "kspCommonMainKotlinMetadata" &&
            it.name != "compileCommonMainKotlinMetadata"
    }.configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
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
