package com.icure.cardinal.bridge.config

import com.icure.cardinal.sdk.utils.Serialization
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configureSerialization() {
	install(ContentNegotiation) {
		json(Serialization.json)
	}
}