package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.specializations.Base64String
import kotlinx.serialization.Serializable

@Serializable
data class SessionParams(
	/**
	 * Specify the base url of the cardinal backend to use for the session, overrides the default
	 */
	val baseUrl: String? = null,
	/**
	 * Specify the keys to use for the session
	 */
	val pkcs8Keys: Map<String, Set<Base64String>>
)