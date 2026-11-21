package com.whatever.caro.feature.importer.route

import androidx.compose.material3.SnackbarDuration
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_snackbar_completed
import caromobile.core.designsystem.generated.resources.import_snackbar_deck_invalid_input
import caromobile.core.designsystem.generated.resources.import_snackbar_empty_deck
import caromobile.core.designsystem.generated.resources.import_snackbar_partial
import caromobile.core.designsystem.generated.resources.import_snackbar_unconfirmed
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.support.SAVE_SCENARIO_DECK
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.StringResource

class ImportDeckRouteTest : FunSpec() {
    init {
        context("저장 결과") {
            val unconfirmed = SnackBarMessage(message = Res.string.import_snackbar_unconfirmed.key, duration = SnackbarDuration.Long)
            listOf(
                Triple(
                    "완료하면 덱 상세로 교체한 뒤 짧은 스낵바로 가져온 장수를 알린다",
                    result(saved = 1_512, total = 1_512, completed = true),
                    SnackBarMessage(message = "${Res.string.import_snackbar_completed.key}|1,512", duration = SnackbarDuration.Short),
                ),
                Triple(
                    "멈춰서 일부만 저장했으면 덱 상세로 교체한 뒤 전체와 저장한 장수를 긴 스낵바로 알린다",
                    result(saved = 1_000, total = 1_512),
                    SnackBarMessage(message = "${Res.string.import_snackbar_partial.key}|1,512|1,000", duration = SnackbarDuration.Long),
                ),
                Triple(
                    "한 장도 저장하지 못하고 멈췄으면 덱 상세로 교체한 뒤 빈 덱 문구를 긴 스낵바로 알린다",
                    result(saved = 0, total = 40),
                    SnackBarMessage(message = Res.string.import_snackbar_empty_deck.key, duration = SnackbarDuration.Long),
                ),
                Triple(
                    "결과를 확인하지 못했으면 저장 장수가 0이어도 빈 덱 문구 대신 확인하지 못했다는 문구를 긴 스낵바로 알린다",
                    result(saved = 0, total = 40, isSaveUnconfirmed = true),
                    unconfirmed,
                ),
                Triple(
                    "결과를 확인하지 못했으면 일부 저장 장수 문구 대신 확인하지 못했다는 문구를 알린다",
                    result(saved = 1_000, total = 1_512, isSaveUnconfirmed = true),
                    unconfirmed,
                ),
            ).forEach { (name, effect, message) ->
                test(name) {
                    val rig = RouteRig()

                    rig.handle(effect)

                    rig.events shouldBe listOf(NavCommand.Replace(DeckDetailEntry(SAVE_SCENARIO_DECK)), message)
                }
            }

            test("가져오기 화면이 구성에서 빠져도 스낵바 문구를 잃지 않게 문구를 먼저 읽은 뒤 덱 화면으로 바꾼다") {
                val rig = RouteRig()

                rig.handle(result(saved = 1_000, total = 1_512))

                rig.calls shouldBe listOf(RouteCall.ResolveText, RouteCall.Navigate, RouteCall.ShowSnackbar)
            }
        }

        context("이동과 오류") {
            test("홈으로 가면 홈으로 초기화하고 스낵바는 띄우지 않는다") {
                val rig = RouteRig()

                rig.handle(ImportDeckSideEffect.NavigateHome)

                rig.events shouldBe listOf(NavCommand.ResetTo(HomeEntry))
            }

            test("뒤로 가면 뒤로 가기만 보내고 선택기는 열지 않는다") {
                val rig = RouteRig()

                rig.handle(ImportDeckSideEffect.NavigateBack)

                listOf(rig.events, rig.pickerOpenCount) shouldBe listOf(listOf(NavCommand.Back), 0)
            }

            test("입력 값 오류로 덱 생성이 거절되면 전역이 아니라 가져오기 화면 안에 이름과 설명을 확인하라는 오류 스낵바만 띄운다") {
                val rig = RouteRig()

                rig.handle(ImportDeckSideEffect.ShowDeckInputError)

                rig.events shouldBe emptyList()
                rig.screenMessages shouldBe
                    listOf(SnackBarMessage(message = Res.string.import_snackbar_deck_invalid_input.key, style = CaroSnackbarStyle.Error))
            }

            test("덱 화면으로 가면 결과 스낵바는 가져오기 화면을 떠난 뒤라 전역으로 띄운다") {
                val rig = RouteRig()

                rig.handle(result(saved = 1_512, total = 1_512, completed = true))

                rig.screenMessages shouldBe emptyList()
            }

            test("파일 선택기 열기 효과는 선택기를 한 번 열고 이동이나 스낵바는 보내지 않는다") {
                val rig = RouteRig()

                rig.handle(ImportDeckSideEffect.OpenFilePicker)

                listOf(rig.pickerOpenCount, rig.events.size) shouldBe listOf(1, 0)
            }
        }
    }

    private fun result(
        saved: Int,
        total: Int,
        completed: Boolean = false,
        isSaveUnconfirmed: Boolean = false,
    ) = ImportDeckSideEffect.NavigateToDeckDetail(SAVE_SCENARIO_DECK, saved, total, completed, isSaveUnconfirmed)
}

private enum class RouteCall {
    ResolveText,
    Navigate,
    ShowSnackbar,
}

/**
 * 이동 명령과 전역 스낵바가 보낸 순서를 한 목록에 남기고, 화면 안 스낵바는 따로 남긴다. 문구는 리소스 키와 인자를 이어 붙인 값으로 대신 읽는다.
 * [calls]는 문구 읽기까지 포함한 호출 순서다.
 */
private class RouteRig {
    val events = mutableListOf<Any>()
    val calls = mutableListOf<RouteCall>()
    val screenMessages = mutableListOf<SnackBarMessage>()
    var pickerOpenCount = 0
        private set

    private val navDispatcher =
        object : NavigationDispatcher {
            override val commands: Flow<NavCommand> = emptyFlow()

            override suspend fun emit(command: NavCommand) {
                events += command
                calls += RouteCall.Navigate
            }
        }

    private val snackbarController =
        object : SnackbarController {
            override val messages: Flow<SnackBarMessage> = emptyFlow()

            override suspend fun show(message: SnackBarMessage) {
                events += message
                calls += RouteCall.ShowSnackbar
            }
        }

    suspend fun handle(sideEffect: ImportDeckSideEffect) {
        handleImportSideEffect(
            sideEffect = sideEffect,
            navDispatcher = navDispatcher,
            snackbarController = snackbarController,
            showScreenSnackbar = { message -> screenMessages += message },
            openFilePicker = { pickerOpenCount += 1 },
            resolveText = ::keyAndArgs,
        )
    }

    private suspend fun keyAndArgs(
        resource: StringResource,
        args: List<Any>,
    ): String {
        calls += RouteCall.ResolveText
        return (listOf(resource.key) + args).joinToString("|")
    }
}
