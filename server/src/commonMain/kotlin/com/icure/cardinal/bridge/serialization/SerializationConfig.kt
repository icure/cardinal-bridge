package com.icure.cardinal.bridge.serialization

import com.icure.cardinal.sdk.utils.Serialization
import com.icure.utils.InternalIcureApi
import kotlinx.serialization.json.Json

object SerializationConfig {
	@OptIn(InternalIcureApi::class)
	val serverJson = Json {
		ignoreUnknownKeys = false
		serializersModule = Serialization.fullLanguageInteropJson.serializersModule
		classDiscriminator = "kotlinType"
	}
}