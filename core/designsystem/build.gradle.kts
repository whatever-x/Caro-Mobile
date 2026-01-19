import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.cmp")
}

kotlin {
    android {
        namespace = "com.whatever.caro.designsystem"
    }


    sourceSets {
        androidMain.dependencies {

        }
        commonMain.dependencies {

        }
    }

}

compose.resources {
    publicResClass = true
    generateResClass = auto
}