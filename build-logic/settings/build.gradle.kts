plugins {
    `kotlin-dsl`
}

group = "com.whatever.caro.buildlogic"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("buildKonfig") {
            id = "caro.build.konfig.setting"
            implementationClass = "BuildKonfigSettingPlugin"
        }
    }
}
