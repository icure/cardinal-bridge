package com.icure.cardinal.bridge.config

import com.icure.cardinal.bridge.serialization.SerializationConfig
import com.icure.utils.InternalIcureApi
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

@OptIn(InternalIcureApi::class)
fun Application.configureSerialization() {
	install(ContentNegotiation) {
		json(SerializationConfig.serverJson)
	}
}