package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.model.exception.CaroClientException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode.UNKNOWN_001
import com.whatever.caro.core.remote.model.CaroBaseResponse
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

            val requestedSerializer = json.serializersModule.serializer(kotlinType)

            val envelopeSerializer = CaroBaseResponse.serializer(requestedSerializer)
            val baseResponse = json.decodeFromString(envelopeSerializer, payloadText)

            if (!baseResponse.success) {
                val error = baseResponse.error

                throw CaroServerException(
                    code = error?.code ?: "9999",
                    message = error?.message ?: "Unknown Error",
                    debugMessage = error?.debugMessage ?: "서버로부터 받은 debug 메세지가 비어있습니다.",
                    description = error?.description,
                )
            }

            val data = baseResponse.data

            if (data != null) {
                return@transformResponseBody data
            }

            if (requestedType.type == Unit::class) {
                return@transformResponseBody Unit
            }

            throw CaroClientException(
                code = UNKNOWN_001,
                message = "Unknown Error",
                debugMessage =
                    buildString {
                        append("Response unwrap failed: expected non-null data but got null")
                        append(", requestedType=")
                        append(kotlinType)
                        append(", responseStatus=")
                        append(response.status.value)
                        append(", contentType=")
                        append(contentType)
                        append(", payloadPreview=")
                        append(payloadText.take(300))
                    },
            )
        }
    }
