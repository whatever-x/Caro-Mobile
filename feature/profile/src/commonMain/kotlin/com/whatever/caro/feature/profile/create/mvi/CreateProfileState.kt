package com.whatever.caro.feature.profile.create.mvi

import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class CreateProfileState(
    val step: CreateProfileStep = CreateProfileStep.Nickname,
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val registrationFailed: Boolean = false,
    val reloginFailed: Boolean = false,
    val isReloginDialogVisible: Boolean = false,
    val nickname: String = "",
    val validationResult: NicknameValidationResult = NicknameValidationResult.Empty,
    val isLoading: Boolean = false,
    val isRandomNicknameLoading: Boolean = false,
    val isCancelDialogVisible: Boolean = false,
) : UiState {
    val characterCount: String
        get() = "${nickname.length}/${NicknameValidator.MAX_LENGTH}"

    val isConfirmEnabled: Boolean
        get() =
            !isLoading &&
                when (step) {
                    CreateProfileStep.Nickname -> validationResult.isValid && !isRandomNicknameLoading
                    CreateProfileStep.BirthDate -> birthDate != null
                }

    val birthDate: String?
        get() =
            birthDateOn(
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date,
            )

    internal fun birthDateOn(today: LocalDate): String? {
        if (year.length != 4 || month.length != 2 || day.length != 2) return null
        val date = runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull() ?: return null
        return date.takeIf { it.year > 0 && it <= today }?.toString()
    }

    val isBirthDateInvalid: Boolean
        get() = year.length == 4 && month.length == 2 && day.length == 2 && birthDate == null
}

enum class CreateProfileStep { Nickname, BirthDate }
