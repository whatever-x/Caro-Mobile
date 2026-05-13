package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.model.exception.CaroClientException
import com.whatever.caro.core.model.exception.CaroException
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode.INVALID_RESPONSE
import com.whatever.caro.core.model.exception.ErrorCode.NETWORK_001
import com.whatever.caro.core.model.exception.ErrorCode.NETWORK_002
import com.whatever.caro.core.model.exception.ErrorCode.UNKNOWN
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.remote.dto.base.ApiResponseDto
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpCallValidatorConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal fun HttpCallValidatorConfig.configureCaroExceptionMapping(jsonParser: Json) {
    handleResponseExceptionWithRequest { cause, _ ->
        if (cause is CaroException) throw cause

        val serverResponseException = cause as? ResponseException
        if (serverResponseException != null) {
            val response = serverResponseException.response
            val responseText =
                runCatching {
                    response.bodyAsText()
                }.getOrElse { throwable ->
                    throw CaroInvalidResponseException(
                        code = INVALID_RESPONSE,
                        message = "Invalid Response Error",
                        debugMessage =
                            buildResponseDebugMessage(
                                reason = "Response Body를 읽는 것을 실패 했습니다.",
                                statusCode = response.status.value,
                                causeMessage = throwable.message,
                            ),
                        throwable = throwable,
                    )
                }

            val baseResponse =
                runCatching {
                    jsonParser.decodeFromString<ApiResponseDto<JsonElement>>(responseText)
                }.getOrElse { throwable ->
                    throw CaroInvalidResponseException(
                        code = INVALID_RESPONSE,
                        message = "Invalid Response Error",
                        debugMessage =
                            buildResponseDebugMessage(
                                reason = "Response Body Decode 과정에서 실패 했습니다.",
                                statusCode = response.status.value,
                                payloadText = responseText,
                                causeMessage = throwable.message,
                            ),
                        throwable = throwable,
                    )
                }

            val errorResponse =
                baseResponse.error ?: throw CaroInvalidResponseException(
                    code = INVALID_RESPONSE,
                    message = "Invalid Response Error",
                    debugMessage =
                        buildResponseDebugMessage(
                            reason = "Error Response가 null 입니다.",
                            statusCode = response.status.value,
                            payloadText = responseText,
                        ),
                )

            throw CaroServerException(
                code = errorResponse.code,
                message = errorResponse.message,
                debugMessage = errorResponse.debugMessage ?: "서버로부터 받은 debug 메세지가 비어있습니다.",
                description = errorResponse.description,
            )
        }

        throw when (cause) {
            is CancellationException -> {
                cause
            }

            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            -> {
                NetworkException(
                    code = NETWORK_002,
                    message = "Network Timeout Error",
                    debugMessage = "네트워크 타임아웃 발생: ${cause.message.orEmpty()}",
                    throwable = cause,
                )
            }

            is UnresolvedAddressException,
            is IOException,
            -> {
                NetworkException(
                    code = NETWORK_001,
                    message = "Network Error",
                    debugMessage = "네트워크 연결 오류 발생: ${cause.message.orEmpty()}",
                    throwable = cause,
                )
            }

            else -> {
                CaroClientException(
                    code = UNKNOWN,
                    message = "Unknown Error",
                    debugMessage = "알 수 없는 예외가 발생했습니다. cause=${cause.message.orEmpty()}",
                    throwable = cause,
                )
            }
        }
    }
}

private fun buildResponseDebugMessage(
    reason: String,
    statusCode: Int,
    payloadText: String? = null,
    causeMessage: String? = null,
): String =
    buildString {
        append("Response exception mapping 실패 : ")
        append(reason)
        append(", statusCode=")
        append(statusCode)
        if (!causeMessage.isNullOrBlank()) {
            append(", cause=")
            append(causeMessage)
        }
        if (payloadText != null) {
            append(", payloadPreview=")
            append(payloadText.take(300))
        }
    }
