package com.whatever.caro.core.test.datasource

import com.whatever.caro.core.remote.datasource.sample.SampleDataSourceImpl
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

class SampleDataSourceTest :
    FunSpec({
        test("getString은 /sample/sample 응답을 문자열로 반환한다") {
            // given
            val dataSource =
                createDataSource(
                    handler = { request ->
                        request.url.encodedPath shouldBe "/sample/sample"

                        respond(
                            content = "sample-response",
                            status = HttpStatusCode.OK,
                        )
                    },
                    factory = ::SampleDataSourceImpl,
                )

            // when
            val result = dataSource.getString()

            // then
            result shouldBe "sample-response"
        }

        test("getString은 서버 에러 응답 시 예외를 던진다") {
            // given
            val dataSource =
                createDataSource(
                    handler = {
                        respond(
                            content = "internal-server-error",
                            status = HttpStatusCode.InternalServerError,
                        )
                    },
                    factory = ::SampleDataSourceImpl,
                )

            // then
            shouldThrowAny {
                dataSource.getString()
            }
        }
    })
