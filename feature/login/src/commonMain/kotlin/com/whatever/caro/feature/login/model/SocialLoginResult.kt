package com.whatever.caro.feature.login.model

sealed interface SocialLoginResult<out T> {
    data class Success<T>(val authResult: T) : SocialLoginResult<T>
    data object UserCancelled : SocialLoginResult<Nothing>
    data object Failed : SocialLoginResult<Nothing>
}