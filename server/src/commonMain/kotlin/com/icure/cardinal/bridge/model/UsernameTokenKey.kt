package com.icure.cardinal.bridge.model

import kotlinx.serialization.Serializable

@Serializable
data class UsernameTokenKey(
	val username: String,
	val token: String,
	val recoveryKey: String
)
