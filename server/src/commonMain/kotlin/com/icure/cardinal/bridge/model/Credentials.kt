package com.icure.cardinal.bridge.model

sealed interface Credentials

data class BasicCredentials(
	val username: String,
	val password: String
) : Credentials

data class JwtCredentials(
	val token: String
) : Credentials