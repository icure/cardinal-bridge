package com.icure.cardinal.bridge.model

import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
	val username: String,
	val password: String
)