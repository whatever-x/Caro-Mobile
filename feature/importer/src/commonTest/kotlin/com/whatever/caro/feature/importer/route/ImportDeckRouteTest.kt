package com.whatever.caro.feature.importer.route

import androidx.compose.material3.SnackbarDuration
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_snackbar_completed
import caromobile.core.designsystem.generated.resources.import_snackbar_empty_deck
import caromobile.core.designsystem.generated.resources.import_snackbar_partial
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
        test("완료하면 덱 상세로 교체한 뒤 짧은 스낵바로 가져온 장수를 알린다") {
            val rig = RouteRig()
            val deck = SAVE_SCENARIO_DECK.copy(cardTotalCount = 12)

            rig.handle(ImportDeckSideEffect.NavigateToDeckDetail(deck = deck, saved = 12, total = 12, completed = true))

            rig.events shouldBe
                listOf(
                    NavCommand.Replace(DeckDetailEntry(deck)),
                    SnackBarMessage(message = "${Res.string.import_snackbar_completed.key}|12", duration = SnackbarDuration.Short),
                )
        }

        test("멈춰서 일부만 저장했으면 덱 상세로 교체한 뒤 전체와 저장한 장수를 긴 스낵바로 알린다") {
            val rig = RouteRig()
            val deck = SAVE_SCENARIO_DECK.copy(cardTotalCount = 12)

            rig.handle(ImportDeckSideEffect.NavigateToDeckDetail(deck = deck, saved = 12, total = 40, completed = false))

            rig.events shouldBe
                listOf(
                    NavCommand.Replace(DeckDetailEntry(deck)),
                    SnackBarMessage(message = "${Res.string.import_snackbar_partial.key}|40|12", duration = SnackbarDuration.Long),
                )
        }

        test("한 장도 저장하지 못하고 멈췄으면 덱 상세로 교체한 뒤 빈 덱 문구를 긴 스낵바로 알린다") {
            val rig = RouteRig()
            val deck = SAVE_SCENARIO_DECK.copy(cardTotalCount = 0)

            rig.handle(ImportDeckSideEffect.NavigateToDeckDetail(deck = deck, saved = 0, total = 40, completed = false))

            rig.events shouldBe
                listOf(
                    NavCommand.Replace(DeckDetailEntry(deck)),
                    SnackBarMessage(message = Res.string.import_snackbar_empty_deck.key, duration = SnackbarDuration.Long),
                )
        }

        test("홈으로 가면 홈으로 초기화하고 스낵바는 띄우지 않는다") {
            val rig = RouteRig()

            rig.handle(ImportDeckSideEffect.NavigateHome)

            rig.events shouldBe listOf(NavCommand.ResetTo(HomeEntry))
        }

        test("뒤로 가면 뒤로 가기만 보낸다") {
            val rig = RouteRig()

            rig.handle(ImportDeckSideEffect.NavigateBack)

            rig.events shouldBe listOf(NavCommand.Back)
        }
    }
}

/** 이동 명령과 스낵바가 보낸 순서를 한 목록에 남긴다. 문구는 리소스 키와 인자를 이어 붙인 값으로 대신 읽는다. */
private class RouteRig {
    val events = mutableListOf<Any>()

    private val navDispatcher =
        object : NavigationDispatcher {
            override val commands: Flow<NavCommand> = emptyFlow()

            override suspend fun emit(command: NavCommand) {
                events += command
            }
        }

    private val snackbarController =
        object : SnackbarController {
            override val messages: Flow<SnackBarMessage> = emptyFlow()

            override suspend fun show(message: SnackBarMessage) {
                events += message
            }
        }

    suspend fun handle(sideEffect: ImportDeckSideEffect) {
        handleImportSideEffect(
            sideEffect = sideEffect,
            navDispatcher = navDispatcher,
            snackbarController = snackbarController,
            resolveText = ::keyAndArgs,
        )
    }

    private suspend fun keyAndArgs(
        resource: StringResource,
        args: List<Any>,
    ): String = (listOf(resource.key) + args).joinToString("|")
}
