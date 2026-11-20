package com.whatever.caro.feature.importer.route

import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ImportDeckRouteTest : FunSpec() {
    init {
        test("뒤로 가면 뒤로 가기만 보내고 선택기는 열지 않는다") {
            val rig = RouteRig()

            rig.handle(ImportDeckSideEffect.NavigateBack)

            listOf(rig.commands, rig.pickerOpenCount) shouldBe listOf(listOf(NavCommand.Back), 0)
        }

        test("파일 선택기 열기 효과는 선택기를 한 번 열고 이동은 보내지 않는다") {
            val rig = RouteRig()

            rig.handle(ImportDeckSideEffect.OpenFilePicker)

            listOf(rig.pickerOpenCount, rig.commands.size) shouldBe listOf(1, 0)
        }
    }
}

private class RouteRig {
    val commands = mutableListOf<NavCommand>()
    var pickerOpenCount = 0
        private set

    private val navDispatcher =
        object : NavigationDispatcher {
            override val commands: Flow<NavCommand> = emptyFlow()

            override suspend fun emit(command: NavCommand) {
                this@RouteRig.commands += command
            }
        }

    suspend fun handle(sideEffect: ImportDeckSideEffect) {
        handleImportSideEffect(
            sideEffect = sideEffect,
            navDispatcher = navDispatcher,
            openFilePicker = { pickerOpenCount += 1 },
        )
    }
}
