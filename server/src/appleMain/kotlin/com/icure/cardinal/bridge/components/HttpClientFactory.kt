package com.icure.cardinal.bridge.components

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

internal actual fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
	HttpClient(Darwin) {
		block()
	}
