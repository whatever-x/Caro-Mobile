package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.model.profile.request.CreateProfileRequest
import com.whatever.caro.core.remote.model.profile.response.CreateProfileResponse
import com.whatever.caro.core.remote.model.profile.response.RandomNicknameResponse
import com.whatever.caro.core.remote.model.profile.response.ValidateNicknameResponse
import org.koin.core.annotation.Single

@Single(binds = [ProfileDataSource::class])
internal class ProfileDataSourceImpl : ProfileDataSource {
    private val mockNicknames = listOf(
        "행복한고양이",
        "빠른여우",
        "용감한사자",
        "귀여운판다",
        "멋진독수리",
    )

    override suspend fun getRandomNickname(): RandomNicknameResponse {
//        return authClient.get("$BASE_URL/nickname/random")
//            .body<RandomNicknameResponse>()

        return RandomNicknameResponse(
            nickname = mockNicknames.random(),
        )
    }

    override suspend fun validateNickname(nickname: String): ValidateNicknameResponse {
//        return authClient.get("$BASE_URL/nickname/validate") {
//            parameter("nickname", nickname)
//        }.body<ValidateNicknameResponse>()

        return ValidateNicknameResponse(
            isValid = nickname.length in 2..10,
            reason = if (nickname.length < 2) "TOO_SHORT" else null,
        )
    }

    override suspend fun createProfile(request: CreateProfileRequest): CreateProfileResponse {
//        return authClient.post("$BASE_URL/profile") {
//            setBody(request)
//        }.body<CreateProfileResponse>()

        return CreateProfileResponse(
            userId = 1L,
            nickname = request.nickname,
        )
    }

    companion object {
        private const val BASE_URL = "/v1/profile"
    }
}
