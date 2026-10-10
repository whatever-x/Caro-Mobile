import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kotlin.serialization")
    id("caro.kmp.test")
}

// iOS 시뮬레이터 테스트는 작업 디렉터리가 모듈 경로가 아니므로 fixture 위치를 절대 경로 상수로 넘긴다.
val generateFixtureRoot =
    tasks.register("generateFixtureRoot") {
        val fixtureRoot =
            layout.projectDirectory
                .dir("src/commonTest/resources/fixtures")
                .asFile.absolutePath
        val outputDir = layout.buildDirectory.dir("generated/fixtureRoot/kotlin")
        inputs.property("fixtureRoot", fixtureRoot)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("com/whatever/caro/core/importparser/FixtureRoot.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                "package com.whatever.caro.core.importparser\n\n" +
                    "internal const val FIXTURE_ROOT = \"$fixtureRoot\"\n",
            )
        }
    }

kotlin {
    android {
        namespace = "com.whatever.caro.core.importparser"
    }

    // 후속 서버 파싱이 같은 파서를 jvm 아티팩트로 재사용하므로 jvm 타깃을 둔다. 바이트코드는 앱 Android 타깃과 같은 17로 고정한다.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // caro.kmp.ios는 앱 프레임워크와 export 설정을 함께 붙이므로 Caro 모듈에 의존하지 않는 이 모듈은 타깃만 직접 선언한다.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.okio)
            api(libs.androidx.sqlite)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.zstd.kmp.okio)
        }
        commonTest {
            kotlin.srcDir(generateFixtureRoot)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
