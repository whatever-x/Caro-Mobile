package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode.NETWORK_001
import com.whatever.caro.core.model.exception.ErrorCode.UNKNOWN_001
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.remote.model.CaroBaseResponse
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpCallValidatorConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal fun HttpCallValidatorConfig.installCaroResponseHandler(jsonParser: Json) {
    handleResponseExceptionWithRequest { cause, _ ->
        val serverResponseException = cause as? ResponseException
        if (serverResponseException != null) {
            val baseResponse =
                runCatching {
                    jsonParser.decodeFromString<CaroBaseResponse<JsonElement>>(
                        serverResponseException.response.bodyAsText(),
                    )
                }.getOrNull()

            val errorResponse = baseResponse?.error

            throw CaroServerException(
                code =
                    errorResponse?.code ?: serverResponseException.response.status.value
                        .toString(),
                message = errorResponse?.message ?: "Caro Server error",
                debugMessage = errorResponse?.debugMessage ?: "Caro Server error",
                description = errorResponse?.description,
            )
        }

        throw when (cause) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            is UnresolvedAddressException,
            is IOException,
                -> {
                NetworkException(
                    code = NETWORK_001,
                    message = "Network Error",
                    debugMessage = "네트워크 오류 발생\n원인 : ${cause.message}",
                )
            }

            else -> {
                NetworkException(
                    code = UNKNOWN_001,
                    message = "Unknown Error",
                    debugMessage = "알 수 없는 오류 발생\n원인 : ${cause.message}",
                )
            }
        }
    }
}
