package com.whatever.caro.feature.importer.save

import com.whatever.caro.feature.importer.support.CONFIRMED_FAILURE_SAMPLES
import com.whatever.caro.feature.importer.support.UNCONFIRMED_FAILURE_SAMPLES
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChunkFailureTest : FunSpec() {
    init {
        UNCONFIRMED_FAILURE_SAMPLES.forEach { failure ->
            test("$failure 뒤에는 멈춘 묶음이 서버에 저장됐는지 확인하지 못한 것이다") {
                failure.isSaveUnconfirmed shouldBe true
            }
        }

        CONFIRMED_FAILURE_SAMPLES.forEach { failure ->
            test("$failure 뒤에는 멈춘 묶음의 저장 결과가 확정된 것이다") {
                failure.isSaveUnconfirmed shouldBe false
            }
        }
    }
}
