package com.whatever.caro.core.data.repository.profile

import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource
import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import com.whatever.caro.core.remote.dto.user.request.UpdateNicknameRequest
import com.whatever.caro.core.remote.dto.user.response.MyNicknameResponse
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse
import com.whatever.caro.core.remote.dto.user.response.UpdateNicknameResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class ProfileRepositoryImplTest : FunSpec() {
    init {
        test("getRandomNickname은 응답의 nickname을 반환한다") {
            runTest {
                val profileDataSource =
                    mock<ProfileDataSource> {
                        everySuspend { getRandomNickname() } returns NicknameResponse(nickname = "캐로")
                    }
                val repository = ProfileRepositoryImpl(profileDataSource)

                repository.getRandomNickname() shouldBe "캐로"
            }
        }

        test("getMyNickname은 현재 사용자 닉네임을 반환한다") {
            runTest {
                val profileDataSource =
                    mock<ProfileDataSource> {
                        everySuspend { getMyNickname() } returns MyNicknameResponse(nickname = "캐로")
                    }
                val repository = ProfileRepositoryImpl(profileDataSource)

                repository.getMyNickname() shouldBe "캐로"
                verifySuspend { profileDataSource.getMyNickname() }
            }
        }

        test("getMyNickname은 nullable 닉네임을 빈 문자열로 정규화한다") {
            runTest {
                val profileDataSource =
                    mock<ProfileDataSource> {
                        everySuspend { getMyNickname() } returns MyNicknameResponse(nickname = null)
                    }
                val repository = ProfileRepositoryImpl(profileDataSource)

                repository.getMyNickname() shouldBe ""
            }
        }

        test("isNicknameAvailable은 응답의 available 값을 반환한다") {
            runTest {
                val profileDataSource =
                    mock<ProfileDataSource> {
                        everySuspend { checkNicknameAvailability("캐로") } returns
                            NicknameCheckResponse(nickname = "캐로", available = true)
                    }
                val repository = ProfileRepositoryImpl(profileDataSource)

                repository.isNicknameAvailable("캐로") shouldBe true

                verifySuspend {
                    profileDataSource.checkNicknameAvailability("캐로")
                }
            }
        }

        test("updateNickname은 닉네임 변경 요청을 전달한다") {
            runTest {
                val profileDataSource =
                    mock<ProfileDataSource> {
                        everySuspend { changeNickname(any()) } returns
                            UpdateNicknameResponse(userId = 1L, nickname = "캐로")
                    }
                val repository = ProfileRepositoryImpl(profileDataSource)

                repository.updateNickname("캐로")

                verifySuspend {
                    profileDataSource.changeNickname(UpdateNicknameRequest(nickname = "캐로"))
                }
            }
        }
    }
}
