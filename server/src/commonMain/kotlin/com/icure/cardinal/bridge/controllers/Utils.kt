package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.serialization.SerializationConfig
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingCall
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

suspend fun <T> RoutingCall.receiveJson(kSerializer: KSerializer<T>): T =
	receive<JsonElement>().let { jsonElement ->
		SerializationConfig.serverJson.decodeFromJsonElement(kSerializer, jsonElement)
	}