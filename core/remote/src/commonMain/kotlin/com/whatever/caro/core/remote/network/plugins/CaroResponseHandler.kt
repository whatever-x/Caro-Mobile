package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.remote.model.CaroBaseResponse
import com.whatever.caro.core.remote.model.NetworkException
import io.ktor.client.plugins.HttpCallValidatorConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal fun HttpCallValidatorConfig.installCaroResponseHandler(jsonParser: Json) {
    handleResponseExceptionWithRequest { cause, _ ->
        val responseException =
            cause as? ResponseException ?: return@handleResponseExceptionWithRequest

        val baseResponse =
            runCatching {
                jsonParser.decodeFromString<CaroBaseResponse<JsonElement>>(
                    responseException.response.bodyAsText(),
                )
            }.getOrNull()

        val errorResponse = baseResponse?.error

        throw NetworkException(
            code =
                errorResponse?.code ?: responseException.response.status.value
                    .toString(),
            message = errorResponse?.message ?: "Server error",
            debugMessage = errorResponse?.debugMessage ?: cause.message.orEmpty(),
            description = errorResponse?.description,
        )
    }
}
