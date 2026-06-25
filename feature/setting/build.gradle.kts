import com.codingfeline.buildkonfig.compiler.FieldSpec

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
        namespace = "com.whatever.caro.feature.setting"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
        }
    }
}

buildkonfig {
    packageName = "com.whatever.caro.feature.setting.generated"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "VERSION_NAME",
            libs.versions.version.name
                .get(),
        )
    }
}
