package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode.INVALID_RESPONSE
import com.whatever.caro.core.remote.dto.base.ApiResponseDto
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal class CaroBaseResponseConverterConfig {
    lateinit var json: Json
}

internal val CaroBaseResponseConverter: ClientPlugin<CaroBaseResponseConverterConfig> =
    createClientPlugin(
        name = "CaroBaseResponseConverter",
        createConfiguration = ::CaroBaseResponseConverterConfig,
    ) {
        val json: Json = pluginConfig.json

        transformResponseBody { response, content, requestedType ->
            val contentType = response.contentType()
            if (contentType?.match(ContentType.Application.Json) != true) {
                return@transformResponseBody null
            }

            val kotlinType = requestedType.kotlinType ?: return@transformResponseBody null
            val payloadText = content.readRemaining().readText(Charsets.UTF_8)

            val requestedSerializer =
                runCatching {
                    json.serializersModule.serializer(kotlinType)
                }.getOrElse { throwable ->
                    throw CaroInvalidResponseException(
                        code = INVALID_RESPONSE,
                        message = "Invalid Response Error",
                        debugMessage =
                            buildDebugMessage(
                                reason = "요청 타입에 대한 serializer를 찾지 못했습니다.",
                                kotlinType = kotlinType,
                                responseStatus = response.status.value,
                                contentType = contentType,
                                payloadText = payloadText,
                                causeMessage = throwable.message,
                            ),
                        throwable = throwable,
                    )
                }

            val envelopeSerializer = ApiResponseDto.serializer(requestedSerializer)
            val baseResponse =
                runCatching {
                    json.decodeFromString(envelopeSerializer, payloadText)
                }.getOrElse { throwable ->
                    throw CaroInvalidResponseException(
                        code = INVALID_RESPONSE,
                        message = "Invalid Response Error",
                        debugMessage =
                            buildDebugMessage(
                                reason = "Response Body Decode 과정에서 실패 했습니다.",
                                kotlinType = kotlinType,
                                responseStatus = response.status.value,
                                contentType = contentType,
                                payloadText = payloadText,
                                causeMessage = throwable.message,
                            ),
                        throwable = throwable,
                    )
                }

            if (!baseResponse.success) {
                val error =
                    baseResponse.error ?: throw CaroInvalidResponseException(
                        code = INVALID_RESPONSE,
                        message = "Invalid Response Error",
                        debugMessage =
                            buildDebugMessage(
                                reason = "success=false 이지만 error 페이로드가 null 입니다.",
                                kotlinType = kotlinType,
                                responseStatus = response.status.value,
                                contentType = contentType,
                                payloadText = payloadText,
                            ),
                    )

                throw CaroServerException(
                    code = error.code,
                    message = error.message,
                    debugMessage = "CaroBaseResponseConverterPlugin에서 오류가 발생하였습니다. (baseResponse:${baseResponse})",
                )
            }

            val data = baseResponse.data

            if (data != null) {
                return@transformResponseBody data
            }

            if (requestedType.type == Unit::class) {
                return@transformResponseBody Unit
            }

            throw CaroInvalidResponseException(
                code = INVALID_RESPONSE,
                message = "Invalid Response Error",
                debugMessage =
                    buildDebugMessage(
                        reason = "success=true 이지만 non-Unit 응답의 data가 null입니다",
                        kotlinType = kotlinType,
                        responseStatus = response.status.value,
                        contentType = contentType,
                        payloadText = payloadText,
                    ),
            )
        }
    }

private fun buildDebugMessage(
    reason: String,
    kotlinType: Any,
    responseStatus: Int,
    contentType: ContentType?,
    payloadText: String,
    causeMessage: String? = null,
): String =
    buildString {
        append("Response Converter failed: ")
        append(reason)
        append(", requestedType=")
        append(kotlinType)
        append(", responseStatus=")
        append(responseStatus)
        append(", contentType=")
        append(contentType ?: "unknown")
        if (!causeMessage.isNullOrBlank()) {
            append(", cause=")
            append(causeMessage)
        }
        append(", payloadPreview=")
        append(payloadText.take(300))
    }
