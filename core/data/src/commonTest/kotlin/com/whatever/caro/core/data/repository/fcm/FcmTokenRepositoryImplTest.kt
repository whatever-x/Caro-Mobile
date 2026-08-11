package com.whatever.caro.core.data.repository.fcm

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.test.runTest

class FcmTokenRepositoryImplTest : FunSpec() {
    init {
        test("syncToken은 예외 없이 토큰을 처리한다") {
            runTest {
                val repository = FcmTokenRepositoryImpl()

                repository.syncToken("fcm-token")
            }
        }
    }
}
