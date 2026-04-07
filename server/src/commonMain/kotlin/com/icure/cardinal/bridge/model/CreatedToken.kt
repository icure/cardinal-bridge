package com.icure.cardinal.bridge.model

import kotlinx.serialization.Serializable

@Serializable
data class CreatedToken(
	val token: String,
	val expirationTs: Long
)