package com.whatever.caro.feature.profile

import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.profile.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.mvi.CreateProfileSideEffect
import com.whatever.caro.feature.profile.mvi.CreateProfileState
import com.whatever.caro.feature.profile.usecase.CheckNicknameUseCase
import com.whatever.caro.feature.profile.usecase.CreateProfileUseCase
import com.whatever.caro.feature.profile.usecase.GetRandomNicknameUseCase
import com.whatever.caro.feature.profile.usecase.NicknameValidationResult
import com.whatever.caro.feature.profile.usecase.ValidateNicknameUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class CreateProfileViewModel(
    private val validateNicknameUseCase: ValidateNicknameUseCase,
    private val checkNicknameUseCase: CheckNicknameUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val getRandomNicknameUseCase: GetRandomNicknameUseCase,
) : BaseViewModel<CreateProfileState, CreateProfileIntent, CreateProfileSideEffect>(
        initialState = CreateProfileState(),
    ) {
    private var nicknameValidationJob: Job? = null

    init {
        fetchRandomNickname()
    }

    override suspend fun handleIntent(intent: CreateProfileIntent) {
        when (intent) {
            is CreateProfileIntent.UpdateNickname -> handleUpdateNickname(intent.nickname)
            is CreateProfileIntent.ClickRefresh -> fetchRandomNickname()
            is CreateProfileIntent.ClickConfirm -> handleConfirm()
            is CreateProfileIntent.ClickBack -> postSideEffect(CreateProfileSideEffect.NavigateBack)
        }
    }

    private fun handleUpdateNickname(nickname: String) {
        val filtered = validateNicknameUseCase.filterInput(nickname)
        val validation = validateNicknameUseCase.validate(filtered)

        if (validation.isValid.not()) {
            nicknameValidationJob?.cancel()
            reduce { copy(nickname = filtered, validationResult = validation) }
            return
        }

        reduce { copy(nickname = filtered, validationResult = NicknameValidationResult.Checking) }

        nicknameValidationJob?.cancel()
        nicknameValidationJob =
            launch {
                delay(DEBOUNCE_MS)
                val result =
                    suspendRunCatching { checkNicknameUseCase(filtered) }
                        .getOrElse { throwable ->
                            Napier.e(throwable = throwable) { "checkNicknameAvailability failed" }
                            // TODO: 서버 에러 처리 UI 확정 시 폴백 제거
                            NicknameValidationResult.Valid
                        }
                reduce { copy(validationResult = result) }
            }
    }

    private fun fetchRandomNickname() {
        reduce { copy(isRandomNicknameLoading = true) }
        launch {
            val nickname = getRandomNicknameUseCase()
            reduce {
                copy(
                    nickname = nickname,
                    validationResult = NicknameValidationResult.Valid,
                    isRandomNicknameLoading = false,
                )
            }
        }
    }

    private fun handleConfirm() {
        if (currentState.isConfirmEnabled.not()) return
        reduce { copy(isLoading = true) }
        launch {
            createProfileUseCase(currentState.nickname)
            postSideEffect(CreateProfileSideEffect.NavigateBack)
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
    }
}
