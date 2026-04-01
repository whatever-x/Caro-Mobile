package com.whatever.caro.feature.profile

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.profile.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.mvi.CreateProfileSideEffect
import com.whatever.caro.feature.profile.mvi.CreateProfileState
import com.whatever.caro.feature.profile.usecase.CreateProfileResult
import com.whatever.caro.feature.profile.usecase.CreateProfileUseCase
import com.whatever.caro.feature.profile.usecase.ValidateNicknameUseCase
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CreateProfileViewModel(
    private val validateNicknameUseCase: ValidateNicknameUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
) : BaseViewModel<CreateProfileState, CreateProfileIntent, CreateProfileSideEffect>(
        initialState = CreateProfileState(),
    ) {
    override suspend fun handleIntent(intent: CreateProfileIntent) {
        when (intent) {
            is CreateProfileIntent.UpdateNickname -> {
                val filtered = validateNicknameUseCase.filterInput(intent.nickname)
                val validation = validateNicknameUseCase.validate(filtered)
                reduce { copy(nickname = filtered, validationResult = validation) }
            }

            is CreateProfileIntent.ClickRefresh -> {
                // TODO: 자동 닉네임 생성 API 연동
                reduce {
                    val validation = validateNicknameUseCase.validate("")
                    copy(nickname = "", validationResult = validation)
                }
            }

            is CreateProfileIntent.ClickConfirm -> {
                if (!currentState.isValid) return
                reduce { copy(isLoading = true) }
                launch {
                    when (createProfileUseCase(currentState.nickname)) {
                        is CreateProfileResult.Success -> {
                            postSideEffect(CreateProfileSideEffect.NavigateBack)
                        }

                        is CreateProfileResult.InvalidNickname -> {
                            reduce { copy(isLoading = false) }
                        }
                    }
                }
            }

            is CreateProfileIntent.ClickBack -> {
                postSideEffect(CreateProfileSideEffect.NavigateBack)
            }
        }
    }
}
