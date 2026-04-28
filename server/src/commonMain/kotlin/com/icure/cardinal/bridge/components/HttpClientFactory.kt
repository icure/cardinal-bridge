package com.icure.cardinal.bridge.components

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal expect fun buildHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
