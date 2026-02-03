package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.User
import com.whatever.caro.core.remote.model.demo.response.DemoResponse

fun DemoResponse.toUser(): User =
    User(
        id = this.user.id,
        name = this.user.name
    )