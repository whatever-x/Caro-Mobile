package com.whatever.caro.feature.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Size
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.banner.HomeBanner
import com.whatever.caro.feature.home.banner.HomeBannerSection
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class HomeBannerLayoutAndroidTest {
    @get:Rule val composeRule = createComposeRule()
    private lateinit var loader: ImageLoader
    private var slowImage: CompletableDeferred<Unit>? = null
    private val requestedSizes = mutableListOf<Size>()
    private val completedImages = mutableListOf<String>()
    private val banner =
        HomeBanner(
            "first",
            1,
            "https://caro.test/first.png",
            "첫 배너",
            null,
            Instant.parse("2099-01-01T00:00:00Z"),
        )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        loader =
            ImageLoader
                .Builder(context)
                .components {
                    add(
                        object : Interceptor {
                            override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                                requestedSizes += chain.request.sizeResolver.size()
                                val url = chain.request.data.toString()
                                if (url.contains("slow")) slowImage?.await()
                                return if (url.contains("broken")) {
                                    ErrorResult(null, chain.request, IllegalStateException("image unavailable"))
                                } else {
                                    val bitmap = Bitmap.createBitmap(354, 118, Bitmap.Config.ARGB_8888)
                                    bitmap.eraseColor(android.graphics.Color.rgb(74, 108, 247))
                                    completedImages += url
                                    SuccessResult(bitmap.asImage(), chain.request)
                                }
                            }
                        },
                    )
                }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
        loader.shutdown()
    }

    @Test
    @Config(qualifiers = "ko-rKR-w320dp-h852dp-xxhdpi")
    fun emptyDeckKeepsLibraryEntry() {
        val intents = mutableListOf<HomeIntent>()
        composeRule.setContent {
            CaroTheme {
                HomeScreen(HomeState(nickname = "건형", streakState = HomeStreakState.NotStarted, isLoading = false), intents::add)
            }
        }
        composeRule.onNodeWithText("내 덱").assertIsDisplayed()
        composeRule.onNodeWithText("덱 라이브러리").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("덱 만들기").assertIsDisplayed().performClick()
        assertEquals(listOf<HomeIntent>(HomeIntent.ClickLibrary, HomeIntent.ClickCreateDeckButton), intents)
    }

    @Test
    fun imageFailureKeepsLibraryEntry() {
        var clicked = false
        composeRule.setContent {
            CaroTheme {
                HomeBannerSection(
                    persistentListOf(banner.copy(imageUrl = "https://caro.test/broken.png")),
                    true,
                    true,
                    null,
                    {},
                    {},
                    {},
                    { clicked = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("덱 라이브러리").assertIsDisplayed().performClick()
        assertTrue(clicked)
        assertTrue(composeRule.onAllNodesWithContentDescription("첫 배너").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun imageRequestMatchesTheMeasuredBannerSlot() {
        composeRule.setContent {
            CaroTheme {
                Box(Modifier.width(320.dp)) {
                    HomeBannerSection(
                        banners = persistentListOf(banner),
                        isVisibleAndIdle = true,
                        autoAdvanceStopped = true,
                        selectedBannerKey = null,
                        onStopAutoAdvance = {},
                        onSelectedBanner = {},
                        onBannerClick = {},
                        onLibraryClick = {},
                    )
                }
            }
        }
        composeRule.waitUntil(5_000) { requestedSizes.isNotEmpty() }
        val density =
            ApplicationProvider
                .getApplicationContext<Context>()
                .resources.displayMetrics.density
        val expectedWidth = (272 * density).toInt()
        assertEquals(listOf(Size(expectedWidth, expectedWidth / 3)), requestedSizes)
    }

    @Test
    fun healthyBannerAppearsBeforeSlowImageAndKeepsItsSelection() {
        val pending = CompletableDeferred<Unit>()
        slowImage = pending
        var selected by mutableStateOf<String?>(null)
        composeRule.setContent {
            CaroTheme {
                HomeBannerSection(
                    banners =
                        persistentListOf(
                            banner.copy(imageUrl = "https://caro.test/slow.png"),
                            banner.copy(id = "second", accessibleLabel = "두 번째 배너"),
                        ),
                    isVisibleAndIdle = true,
                    autoAdvanceStopped = true,
                    selectedBannerKey = selected,
                    onStopAutoAdvance = {},
                    onSelectedBanner = { selected = it },
                    onBannerClick = {},
                    onLibraryClick = {},
                )
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("두 번째 배너").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("두 번째 배너").assertIsDisplayed()
        assertTrue(!pending.isCompleted)
        pending.complete(Unit)
        composeRule.waitUntil(5_000) { completedImages.size == 2 }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("두 번째 배너").assertIsDisplayed()
        assertEquals("second:1", selected)
    }

    @Test
    fun autoAdvanceStopsForTheVisitAfterManualSwipe() {
        var stopped by mutableStateOf(false)
        var clicked: String? = null
        composeRule.setContent {
            CaroTheme {
                HomeBannerSection(
                    banners = persistentListOf(banner, banner.copy(id = "second", accessibleLabel = "두 번째 배너")),
                    isVisibleAndIdle = true,
                    autoAdvanceStopped = stopped,
                    selectedBannerKey = null,
                    onStopAutoAdvance = { stopped = true },
                    onSelectedBanner = {},
                    onBannerClick = { clicked = it.id },
                    onLibraryClick = {},
                )
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("첫 배너").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule
            .onNodeWithContentDescription("두 번째 배너")
            .assertIsDisplayed()
            .performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertTrue(stopped)
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule.onNodeWithContentDescription("첫 배너").assertIsDisplayed().performClick()
        assertEquals("first", clicked)
    }
}
