package com.whatever.caro.core.data.repository

import io.github.aakira.napier.Napier

// FIXME: 서버 API 연동 필요. 현재는 stub.
internal class FcmTokenRepositoryImpl : FcmTokenRepository {
    override suspend fun syncToken(token: String) {
        Napier.d { "FcmTokenRepository -> syncToken(token=$token)" }
    }
}
