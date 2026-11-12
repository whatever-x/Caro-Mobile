import app.cash.turbine.test
import com.whatever.caro.core.data.repository.banner.HomeBannerRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.repository.streak.StreakRepository
import com.whatever.caro.core.model.banner.HomeBanner
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.model.streak.StreakStatus
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeStreakState
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        fun viewModelWith(
            deckRepository: DeckRepository =
                mock { everySuspend { getDecks() } returns emptyList() },
            streakRepository: StreakRepository =
                mock {
                    everySuspend { getStreak() } returns
                        Streak(status = StreakStatus.NOT_STARTED, currentDays = 0)
                },
            profileRepository: ProfileRepository =
                mock { everySuspend { getMyNickname() } returns "캐로" },
            exceptionFilter: ExceptionFilter = ExceptionFilter.None,
            bannerRepository: HomeBannerRepository = mock { everySuspend { list() } returns emptyList() },
        ) = HomeViewModel(
            deckRepository,
            streakRepository,
            profileRepository,
            bannerRepository,
            exceptionFilter,
        )

        test("배너 응답이 늦어도 홈 콘텐츠와 라이브러리 이동을 먼저 제공한다") {
            runTest(testDispatcher) {
                val pending = CompletableDeferred<List<HomeBanner>>()
                val repository = mock<HomeBannerRepository> { everySuspend { list() } calls { pending.await() } }
                val viewModel = viewModelWith(bannerRepository = repository)
                viewModel.intent(HomeIntent.Initialize)
                runCurrent()
                viewModel.state.value.isLoading shouldBe false
                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickLibrary)
                    runCurrent()
                    awaitItem() shouldBe HomeSideEffect.NavigateToLibrary
                }
                pending.complete(emptyList())
                advanceUntilIdle()
            }
        }

        test("배너 조회 실패는 홈 오류로 처리하지 않는다") {
            runTest(testDispatcher) {
                val repository = mock<HomeBannerRepository> { everySuspend { list() } throws IllegalStateException("banner unavailable") }
                val viewModel = viewModelWith(bannerRepository = repository)
                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()
                viewModel.state.value.hasLoadError shouldBe false
                viewModel.state.value.banners
                    .isEmpty() shouldBe true
            }
        }

        test("유효한 배너만 라이브러리 목록이나 제공 덱 미리보기로 이동한다") {
            runTest(testDispatcher) {
                val banner = HomeBanner("banner", 1, "https://caro.test/banner.png", "라이브러리", 42, Instant.parse("2099-01-01T00:00:00Z"))
                val expired = banner.copy(id = "expired", expiresAt = Instant.parse("2000-01-01T00:00:00Z"))
                val listing = banner.copy(id = "list", libraryDeckId = null)
                val repository = mock<HomeBannerRepository> { everySuspend { list() } returns listOf(banner, expired, listing) }
                val viewModel = viewModelWith(bannerRepository = repository)
                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()
                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickBanner("banner", 1))
                    advanceUntilIdle()
                    awaitItem() shouldBe HomeSideEffect.NavigateToLibraryPreview(42)
                    viewModel.intent(HomeIntent.ClickBanner("list", 1))
                    advanceUntilIdle()
                    awaitItem() shouldBe HomeSideEffect.NavigateToLibrary
                    viewModel.intent(HomeIntent.ClickBanner("expired", 1))
                    viewModel.intent(HomeIntent.ClickBanner("banner", 2))
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("늦게 도착한 이전 배너 응답은 최신 목록을 덮어쓰지 않는다") {
            runTest(testDispatcher) {
                val oldRequest = CompletableDeferred<List<HomeBanner>>()
                val latest = HomeBanner("latest", 1, "https://caro.test/banner.png", "라이브러리", null, Instant.parse("2099-01-01T00:00:00Z"))
                var calls = 0
                val repository =
                    mock<HomeBannerRepository> {
                        everySuspend { list() } calls { if (++calls == 1) oldRequest.await() else listOf(latest) }
                    }
                val viewModel = viewModelWith(bannerRepository = repository)
                viewModel.intent(HomeIntent.Initialize)
                runCurrent()
                viewModel.intent(HomeIntent.Initialize)
                runCurrent()
                oldRequest.complete(listOf(latest.copy(id = "old")))
                advanceUntilIdle()
                viewModel.state.value.banners
                    .map { it.id } shouldBe listOf("latest")
            }
        }

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("ClickSettingButton 은 NavigateToSetting 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickSettingButton)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToSetting
                }
            }
        }

        test("ClickCreateDeck 은 NavigateToCreateDeck 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToCreateDeck
                }
            }
        }

        test("ClickCreateDeckButton 은 NavigateToCreateDeck 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickCreateDeckButton)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToCreateDeck
                }
            }
        }

        test("덱 클릭 시 덱 상세 이동 side effect에 홈 덱 데이터를 그대로 포함한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()
                val deck =
                    Deck(
                        id = 1L,
                        title = "테스트 덱",
                        description = "테스트 설명",
                        cardTotalCount = 24,
                        todayLearningCount = 12,
                        todayCompleteCount = 6,
                        state = DeckState.LEARNING,
                    )

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickDeckButton(deck = deck))

                    val sideEffect = awaitItem()
                    sideEffect shouldBe HomeSideEffect.NavigateToDeckDetail(deck = deck)

                    val payloadDeck = (sideEffect as HomeSideEffect.NavigateToDeckDetail).deck
                    payloadDeck.title shouldBe "테스트 덱"
                    payloadDeck.description shouldBe "테스트 설명"
                    payloadDeck.cardTotalCount shouldBe 24
                    payloadDeck.todayLearningCount shouldBe 12
                    payloadDeck.todayCompleteCount shouldBe 6
                    payloadDeck.todayProgress shouldBe 50
                    payloadDeck.state shouldBe DeckState.LEARNING
                }
            }
        }

        test("Initialize 는 덱을 불러와 state.decks 를 채우고 로딩을 종료한다") {
            runTest(testDispatcher) {
                val decks =
                    listOf(
                        Deck(
                            id = 1L,
                            title = "덱1",
                            description = "설명1",
                            cardTotalCount = 10,
                            todayLearningCount = 4,
                            todayCompleteCount = 2,
                            state = DeckState.LEARNING,
                        ),
                    )
                val deckRepository =
                    mock<DeckRepository> { everySuspend { getDecks() } returns decks }
                val viewModel = viewModelWith(deckRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.isDeckEmpty shouldBe false
                viewModel.state.value.decks
                    .toList() shouldBe decks
                verifySuspend { deckRepository.getDecks() }
            }
        }

        test("Initialize 는 ACTIVE streak 일수를 Home 상태에 반영한다") {
            runTest(testDispatcher) {
                val streakRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } returns
                            Streak(status = StreakStatus.ACTIVE, currentDays = 6)
                    }
                val viewModel = viewModelWith(streakRepository = streakRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.streakState shouldBe HomeStreakState.Active(days = 6)
                verifySuspend { streakRepository.getStreak() }
            }
        }

        test("Initialize 는 현재 사용자 닉네임을 Home 상태에 반영한다") {
            runTest(testDispatcher) {
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyNickname() } returns "승우"
                    }
                val viewModel = viewModelWith(profileRepository = profileRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.nickname shouldBe "승우"
                verifySuspend { profileRepository.getMyNickname() }
            }
        }

        test("Initialize 는 NOT_STARTED 와 BROKEN streak 상태를 구분한다") {
            runTest(testDispatcher) {
                val notStartedRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } returns
                            Streak(status = StreakStatus.NOT_STARTED, currentDays = 0)
                    }
                val brokenRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } returns
                            Streak(status = StreakStatus.BROKEN, currentDays = 0)
                    }

                val notStarted = viewModelWith(streakRepository = notStartedRepository)
                notStarted.intent(HomeIntent.Initialize)
                advanceUntilIdle()
                notStarted.state.value.streakState shouldBe HomeStreakState.NotStarted

                val broken = viewModelWith(streakRepository = brokenRepository)
                broken.intent(HomeIntent.Initialize)
                advanceUntilIdle()
                broken.state.value.streakState shouldBe HomeStreakState.Broken
            }
        }

        test("덱 조회가 실패해도 streak 성공 결과를 유지한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } throws RuntimeException("deck error")
                    }
                val streakRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } returns
                            Streak(status = StreakStatus.ACTIVE, currentDays = 3)
                    }
                val viewModel = viewModelWith(deckRepository, streakRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.streakState shouldBe HomeStreakState.Active(days = 3)
                viewModel.state.value.hasLoadError shouldBe true
            }
        }

        test("덱 조회가 실패하면 ShowDeckLoadError 를 방출한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } throws RuntimeException("deck error")
                    }
                val viewModel = viewModelWith(deckRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.Initialize)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.ShowDeckLoadError
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("streak 조회가 실패해도 덱 성공 결과를 유지한다") {
            runTest(testDispatcher) {
                val decks =
                    listOf(
                        Deck(
                            id = 1L,
                            title = "덱",
                            description = "설명",
                            cardTotalCount = 10,
                            todayLearningCount = 2,
                            todayCompleteCount = 1,
                            state = DeckState.LEARNING,
                        ),
                    )
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } returns decks
                    }
                val streakRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } throws RuntimeException("streak error")
                    }
                val viewModel = viewModelWith(deckRepository, streakRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.decks
                    .toList() shouldBe decks
                viewModel.state.value.streakState shouldBe HomeStreakState.Loading
                viewModel.state.value.hasLoadError shouldBe true
            }
        }

        test("닉네임 조회가 실패해도 덱과 streak 성공 결과를 유지한다") {
            runTest(testDispatcher) {
                val decks =
                    listOf(
                        Deck(
                            id = 1L,
                            title = "덱",
                            description = "설명",
                            cardTotalCount = 10,
                            todayLearningCount = 2,
                            todayCompleteCount = 1,
                            state = DeckState.LEARNING,
                        ),
                    )
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } returns decks
                    }
                val streakRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } returns
                            Streak(status = StreakStatus.ACTIVE, currentDays = 5)
                    }
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyNickname() } throws RuntimeException("nickname error")
                    }
                val viewModel = viewModelWith(deckRepository, streakRepository, profileRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.nickname shouldBe ""
                viewModel.state.value.decks
                    .toList() shouldBe decks
                viewModel.state.value.streakState shouldBe HomeStreakState.Active(days = 5)
                viewModel.state.value.hasLoadError shouldBe true
            }
        }

        test("ClickRetry 는 실패한 홈 데이터를 다시 불러오고 에러 상태를 해제한다") {
            runTest(testDispatcher) {
                var requestCount = 0
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } calls {
                            requestCount++
                            if (requestCount == 1) {
                                throw RuntimeException("offline")
                            }
                            emptyList()
                        }
                    }
                val viewModel = viewModelWith(deckRepository = deckRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()
                viewModel.state.value.hasLoadError shouldBe true

                viewModel.intent(HomeIntent.ClickRetry)
                advanceUntilIdle()

                viewModel.state.value.hasLoadError shouldBe false
                viewModel.state.value.isLoading shouldBe false
                requestCount shouldBe 2
            }
        }

        test("연속 Initialize 에서는 느린 이전 요청이 최신 상태를 덮어쓰지 않는다") {
            runTest(testDispatcher) {
                val firstDecks = CompletableDeferred<List<Deck>>()
                var deckRequestCount = 0
                var nicknameRequestCount = 0
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } calls {
                            deckRequestCount++
                            if (deckRequestCount == 1) {
                                firstDecks.await()
                            } else {
                                emptyList()
                            }
                        }
                    }
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyNickname() } calls {
                            nicknameRequestCount++
                            if (nicknameRequestCount == 1) "이전 닉네임" else "최신 닉네임"
                        }
                    }
                val viewModel =
                    viewModelWith(
                        deckRepository = deckRepository,
                        profileRepository = profileRepository,
                    )

                viewModel.intent(HomeIntent.Initialize)
                runCurrent()
                viewModel.intent(HomeIntent.Initialize)
                runCurrent()

                viewModel.state.value.nickname shouldBe "최신 닉네임"

                firstDecks.complete(emptyList())
                advanceUntilIdle()

                viewModel.state.value.nickname shouldBe "최신 닉네임"
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("동시 조회 실패는 decks 예외를 대표로 두고 나머지 원인을 suppressed 로 보존한다") {
            runTest(testDispatcher) {
                val decksError = IllegalStateException("decks error")
                val streakError = IllegalArgumentException("streak error")
                val nicknameError = RuntimeException("nickname error")
                val capturedErrors = mutableListOf<Throwable>()
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } throws decksError
                    }
                val streakRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } throws streakError
                    }
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyNickname() } throws nicknameError
                    }
                val viewModel =
                    viewModelWith(
                        deckRepository = deckRepository,
                        streakRepository = streakRepository,
                        profileRepository = profileRepository,
                        exceptionFilter =
                            ExceptionFilter { throwable ->
                                capturedErrors += throwable
                                true
                            },
                    )

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                capturedErrors.size shouldBe 1
                val primaryError = capturedErrors.single().cause ?: capturedErrors.single()
                (primaryError === decksError) shouldBe true
                verifySuspend { deckRepository.getDecks() }
                verifySuspend { streakRepository.getStreak() }
                verifySuspend { profileRepository.getMyNickname() }
                primaryError.suppressedExceptions
                    .toList()
                    .shouldContainExactly(streakError, nicknameError)
            }
        }
    }
}
