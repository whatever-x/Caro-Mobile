import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.cmp")
    id("caro.koin.annotation")
}

kotlin {
    android {
        namespace = "com.whatever.caro.composeApp"
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family.isAppleFamily) {
            binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
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

            implementation(projects.feature.home)
            implementation(projects.feature.login)
        }
    }

}
