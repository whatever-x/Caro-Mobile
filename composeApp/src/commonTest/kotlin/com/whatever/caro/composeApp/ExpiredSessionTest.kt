package com.whatever.caro.composeApp

import com.whatever.caro.core.datastore.datasource.AuthTokens
import com.whatever.caro.core.model.auth.AuthSessionEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExpiredSessionTest :
    FunSpec({
        test("이전 세션 만료 이벤트는 새 로그인에 적용하지 않는다") {
            val expired = AuthSessionEvent.Expired(2)
            isCurrentExpiredSession(expired, AuthTokens(null, null, 2)) shouldBe true
            isCurrentExpiredSession(expired, AuthTokens("new", "refresh", 3)) shouldBe false
            isCurrentExpiredSession(expired, AuthTokens("new", "refresh", 2)) shouldBe false
        }
    })
