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
    }
}