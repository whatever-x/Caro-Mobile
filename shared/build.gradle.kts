import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.cmp")
}

kotlin {
    android {
        namespace = "com.whatever.caro.shared"
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family.isAppleFamily) {
            binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        androidMain.dependencies {

        }
        commonMain.dependencies {

        }
    }

}
