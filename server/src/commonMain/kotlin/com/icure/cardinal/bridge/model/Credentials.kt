package com.icure.cardinal.bridge.model

sealed interface Credentials

data class JwtCredentials(
	val token: String
) : Credentials