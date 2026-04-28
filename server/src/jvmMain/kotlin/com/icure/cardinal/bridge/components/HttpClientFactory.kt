package com.icure.cardinal.bridge.components

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal actual fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient {
    block()
}
