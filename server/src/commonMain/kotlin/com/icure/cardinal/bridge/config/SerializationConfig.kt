package com.icure.cardinal.bridge.config

import com.icure.cardinal.sdk.utils.Serialization
import com.icure.utils.InternalIcureApi
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

@OptIn(InternalIcureApi::class)
fun Application.configureSerialization() {
	install(ContentNegotiation) {
		json(Json {
			ignoreUnknownKeys = false
			serializersModule = Serialization.fullLanguageInteropJson.serializersModule
			classDiscriminator = "kotlinType"
		})
	}
}