package com.whatever.caro.feature.login.provider

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.whatever.caro.feature.login.model.SocialLoginResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GoogleCredentialErrorTest :
    FunSpec({
        test("로그인 정보를 찾지 못한 오류는 기타 실패와 구분한다") {
            NoCredentialException().toSocialLoginResult() shouldBe SocialLoginResult.NoCredential
        }

        test("계정 선택 취소는 재시도 중에도 사용자 취소로 구분한다") {
            GetCredentialCancellationException().toSocialLoginResult() shouldBe SocialLoginResult.UserCancelled
        }

        test("기타 Credential Manager 오류는 일반 실패로 유지한다") {
            GetCredentialUnknownException().toSocialLoginResult() shouldBe SocialLoginResult.Failed
        }
    })
