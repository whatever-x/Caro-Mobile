package com.whatever.caro.core.test.dto

import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationV11Request
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSerializationApi::class)
class CompleteRegistrationV11RequestTest : FunSpec() {
    init {
        test("생년월일을 ISO 날짜 형식으로 직렬화한다") {
            val request = CompleteRegistrationV11Request("닉네임", LocalDate(2000, 1, 2), true)
            Json.encodeToString(request) shouldBe
                """{"nickname":"닉네임","birthDate":"2000-01-02","isTermsAgreed":true}"""
        }

        test("생년월일이 없으면 역직렬화에 실패한다") {
            assertFailsWith<MissingFieldException> {
                Json.decodeFromString<CompleteRegistrationV11Request>("""{"nickname":"닉네임","isTermsAgreed":true}""")
            }
        }
    }
}
