package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse

internal fun MyInfoResponse.toMyNickname(): String = nickname.orEmpty()
