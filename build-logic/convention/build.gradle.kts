import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.whatever.caro.buildlogic"

repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.bundles.build.logic.plugins)
    compileOnly(libs.kmp.spm.gradleplugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

gradlePlugin {
    plugins {
        register("cmp") {
            id = "caro.cmp"
            implementationClass = "CmpPlugin"
        }
        register("kmp") {
            id = "caro.kmp"
            implementationClass = "KmpPlugin"
        }
        register("kmpAndroid") {
            id = "caro.kmp.android"
            implementationClass = "KmpAndroidPlugin"
        }
        register("kmpIos") {
            id = "caro.kmp.ios"
            implementationClass = "KmpIosPlugin"
        }
        register("kmpIosSpm") {
            id = "caro.kmp.ios.spm"
            implementationClass = "KmpIosSpmPlugin"
        }
        register("kotlinSerialization") {
            id = "caro.kotlin.serialization"
            implementationClass = "KotlinSerializationPlugin"
        }
        register("androidApplication") {
            id = "caro.android.application"
            implementationClass = "AndroidApplicationPlugin"
        }
        register("kmpTest") {
            id = "caro.kmp.test"
            implementationClass = "KmpTestPlugin"
        }
        register("feature") {
            id = "caro.feature"
            implementationClass = "FeaturePlugin"
        }
        register("koinPlugin") {
            id = "caro.koin"
            implementationClass = "KoinPlugin"
        }
        register("koverPlugin") {
            id = "caro.kover"
            implementationClass = "KoverPlugin"
        }
    }
}