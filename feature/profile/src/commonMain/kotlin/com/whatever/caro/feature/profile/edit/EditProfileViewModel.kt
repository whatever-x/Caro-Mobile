package com.whatever.caro.feature.profile.edit

import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.edit.mvi.EditProfileIntent
import com.whatever.caro.feature.profile.edit.mvi.EditProfileSideEffect
import com.whatever.caro.feature.profile.edit.mvi.EditProfileState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class EditProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val nicknameValidator: NicknameValidator,
    nickname: String,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<EditProfileState, EditProfileIntent, EditProfileSideEffect>(
        initialState = EditProfileState(nickname = nickname),
        exceptionFilter = exceptionFilter,
    ) {
    private var nicknameValidationJob: Job? = null
    private var randomNicknameJob: Job? = null

    override suspend fun handleIntent(intent: EditProfileIntent) {
        if (currentState.isLoading) return

        when (intent) {
            is EditProfileIntent.UpdateNickname -> handleUpdateNickname(intent.nickname)
            is EditProfileIntent.ClickRefresh -> fetchRandomNickname()
            is EditProfileIntent.ClickConfirm -> handleConfirm()
            is EditProfileIntent.ClickBack -> postSideEffect(EditProfileSideEffect.NavigateBack)
        }
    }

    private fun handleUpdateNickname(nickname: String) {
        randomNicknameJob?.cancel()
        val filtered = nicknameValidator.filterInput(nickname)
        val validation = nicknameValidator.validate(filtered)

        if (validation.isValid.not()) {
            nicknameValidationJob?.cancel()
            reduce {
                copy(
                    nickname = filtered,
                    validationResult = validation,
                    isRandomNicknameLoading = false,
                )
            }
            return
        }

        reduce {
            copy(
                nickname = filtered,
                validationResult = NicknameValidationResult.Checking,
                isRandomNicknameLoading = false,
            )
        }

        nicknameValidationJob?.cancel()
        nicknameValidationJob =
            launch {
                delay(DEBOUNCE_MS)
                val result =
                    suspendRunCatching {
                        if (profileRepository.isNicknameAvailable(filtered)) {
                            NicknameValidationResult.Valid
                        } else {
                            NicknameValidationResult.Duplicate
                        }
                    }.getOrElse { throwable ->
                        Napier.e(throwable = throwable) { "checkNicknameAvailability failed" }
                        postErrorSideEffect(
                            throwable = throwable,
                            fallback = EditProfileSideEffect.ShowNicknameCheckError,
                        )
                        // 일시적인 확인 실패로 입력을 막지 않는다. 실제 중복은 변경 요청에서 서버가 거른다.
                        NicknameValidationResult.Valid
                    }
                reduce { copy(validationResult = result) }
            }
    }

    private fun fetchRandomNickname() {
        nicknameValidationJob?.cancel()
        randomNicknameJob?.cancel()
        reduce { copy(isRandomNicknameLoading = true) }
        randomNicknameJob =
            launch {
                suspendRunCatching { profileRepository.getRandomNickname() }
                    .onSuccess { nickname ->
                        reduce {
                            copy(
                                nickname = nickname,
                                validationResult = NicknameValidationResult.Valid,
                            )
                        }
                    }.onFailure { throwable ->
                        Napier.e(throwable = throwable) { "getRandomNickname failed" }
                        postErrorSideEffect(
                            throwable = throwable,
                            fallback = EditProfileSideEffect.ShowRandomNicknameError,
                        )
                    }
                reduce { copy(isRandomNicknameLoading = false) }
            }
    }

    private fun handleConfirm() {
        if (currentState.isConfirmEnabled.not()) return
        val nickname = currentState.nickname
        nicknameValidationJob?.cancel()
        randomNicknameJob?.cancel()
        reduce { copy(isLoading = true, isRandomNicknameLoading = false) }
        launch {
            suspendRunCatching {
                profileRepository.updateNickname(nickname = nickname)
            }.onSuccess {
                postSideEffect(EditProfileSideEffect.NavigateBack)
            }.onFailure { throwable ->
                Napier.e(throwable = throwable) { "handleConfirm failed" }
                reduce { copy(isLoading = false) }
                postErrorSideEffect(
                    throwable = throwable,
                    fallback = EditProfileSideEffect.ShowUpdateNicknameError,
                )
            }
        }
    }

    private fun postErrorSideEffect(
        throwable: Throwable,
        fallback: EditProfileSideEffect,
    ) {
        val sideEffect =
            if (throwable is NetworkException) {
                EditProfileSideEffect.ShowNetworkError
            } else {
                fallback
            }
        postSideEffect(sideEffect)
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
    }
}
