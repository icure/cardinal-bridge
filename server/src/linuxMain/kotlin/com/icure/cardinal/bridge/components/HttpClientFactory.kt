package com.icure.cardinal.bridge.components

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.curl.Curl

internal actual fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Curl) {
    engine {
        caInfo = "/etc/ssl/certs/ca-certificates.crt"
    }
    block()
}
