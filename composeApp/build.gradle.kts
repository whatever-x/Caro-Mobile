plugins {
    id("caro.kmp")
    id("caro.kmp.android")
    id("caro.kmp.ios")
    id("caro.cmp")
    id("caro.koin")
    id("caro.kmp.test")
}

kotlin {
    android {
        namespace = "com.whatever.caro.composeApp"
    }

    sourceSets {
        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.datastore)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
            implementation(projects.core.viewmodel)
            implementation(projects.core.navigator)
            implementation(projects.core.model)
            implementation(projects.core.remote)
            api(projects.core.analytics)
            api(projects.core.crashlytics)
            api(projects.core.messaging)

            implementation(projects.feature.home)
            api(projects.feature.login)
            implementation(projects.feature.splash)
            implementation(projects.feature.profile)
            implementation(projects.feature.deck)
            implementation(projects.feature.card)
            implementation(projects.feature.setting)
            implementation(projects.feature.deckDetail)
            implementation(projects.feature.learning)

            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.nav3)
        }
    }
}

// koin-compiler 1.0.0의 전역 그래프 검증(KOIN-D001)이 plain DSL 람다/named()/parametersOf로
// 등록된 정의를 provider로 인식하지 못해 기존 등록 5건을 누락으로 오판하여 비활성화한다.
// 플러그인이 해당 패턴(@Provides/@Assisted 격)을 지원하면 재활성화한다.
koinCompiler {
    compileSafety = false
}
