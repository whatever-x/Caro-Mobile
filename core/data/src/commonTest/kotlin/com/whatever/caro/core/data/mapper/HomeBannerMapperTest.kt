package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.remote.dto.banner.BannerDestinationResponse
import com.whatever.caro.core.remote.dto.banner.HomeBannerResponse
import com.whatever.caro.core.remote.dto.banner.HomeBannersResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

class HomeBannerMapperTest :
    FunSpec({
        val serverTime = "2026-11-11T00:00:00Z"
        val receivedAt = Instant.parse("2030-01-01T00:00:00Z")
        val valid =
            HomeBannerResponse(
                id = "library",
                revision = 2,
                imageUrl = "https://caro.test/banner.png",
                aspectRatio = 3.0,
                accessibleLabel = "무료 제공 덱 둘러보기",
                destination = BannerDestinationResponse("LIBRARY_LIST"),
                startsAt = "2026-11-10T00:00:00Z",
                endsAt = "2026-11-11T00:01:00Z",
            )

        fun response(vararg items: HomeBannerResponse) = HomeBannersResponse(items.toList(), serverTime, "config-1")

        test("서버 시각으로 남은 노출 시간을 계산하고 제공 덱 목적지를 보존한다") {
            val banner = valid.copy(destination = BannerDestinationResponse("LIBRARY_DECK", "42"))
            val result = response(banner).toHomeBanners(receivedAt).single()
            result.expiresAt shouldBe Instant.parse("2030-01-01T00:01:00Z")
            result.libraryDeckId shouldBe 42L
            result.key shouldBe "library:2"
        }
        test("빈 목록은 정상적인 빈 결과다") {
            response().toHomeBanners(receivedAt) shouldBe emptyList()
        }
        test("미지원 목적지와 손상된 소재를 제외하고 응답 순서를 보존한다") {
            val items =
                response(
                    valid.copy(id = "future", destination = BannerDestinationResponse("EXTERNAL_URL")),
                    valid.copy(id = "broken", imageUrl = "http://caro.test/insecure.png"),
                    valid.copy(id = "bad-ratio", aspectRatio = 0.0),
                    valid.copy(id = "bad-date", endsAt = "invalid"),
                    valid.copy(id = "bad-target", destination = BannerDestinationResponse("LIBRARY_DECK", "0")),
                    valid.copy(id = "first"),
                    valid.copy(id = "second"),
                ).toHomeBanners(receivedAt)
            items.map { it.id } shouldBe listOf("first", "second")
        }
        test("노출 시작 전과 만료 시각인 소재는 표시하지 않는다") {
            response(
                valid.copy(startsAt = "2026-11-12T00:00:00Z"),
                valid.copy(endsAt = serverTime),
                valid.copy(accessibleLabel = ""),
            ).toHomeBanners(receivedAt) shouldBe emptyList()
        }
        test("소재 id 중복을 제거하고 최대 다섯 장을 유지한다") {
            val banners = (listOf(valid, valid) + (1..7).map { valid.copy(id = "item-$it") }).toTypedArray()
            response(*banners).toHomeBanners(receivedAt).map { it.id } shouldBe
                listOf("library", "item-1", "item-2", "item-3", "item-4")
        }
    })
