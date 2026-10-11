plugins {
    id("caro.kmp")
    id("caro.kmp.ios")
    id("caro.kmp.android")
    id("caro.cmp")
    id("caro.feature")
    id("caro.koin")
    id("caro.kmp.test")
    id("caro.kover")
}

// 연결 후 처리 테스트는 파서 모듈의 fixture를 함께 쓴다. iOS 시뮬레이터 테스트의 작업 디렉터리가 모듈 경로가 아니므로 절대 경로 상수로 넘긴다.
val generateFixtureRoot =
    tasks.register("generateFixtureRoot") {
        val fixtureRoot =
            rootProject.layout.projectDirectory
                .dir("core/importparser/src/commonTest/resources/fixtures")
                .asFile.absolutePath
        val outputDir = layout.buildDirectory.dir("generated/fixtureRoot/kotlin")
        inputs.property("fixtureRoot", fixtureRoot)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("com/whatever/caro/feature/importer/FixtureRoot.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                "package com.whatever.caro.feature.importer\n\n" +
                    "internal const val FIXTURE_ROOT = \"$fixtureRoot\"\n",
            )
        }
    }

// 공통 규칙(50%)은 그대로 두고 importer의 LINE 기준만 더 높게 잡는다. 보고서에는 caro.kover가 고른 ViewModel 클래스만 들어간다.
kover {
    reports {
        verify {
            rule("ImportDeckViewModel") {
                minBound(80)
            }
        }
    }
}

kotlin {
    android {
        namespace = "com.whatever.caro.feature.importer"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.importparser)
        }
        commonTest {
            kotlin.srcDir(generateFixtureRoot)
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
