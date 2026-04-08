package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.BasicCredentials
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.bridge.model.JwtCredentials
import com.icure.cardinal.sdk.model.specializations.Base64String
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64

fun Route.sessionRoutes(
	sdkInitializer: CardinalSdkInitializer
) {
	route("/session") {
		post {
			val credentials = credentials()
			val sessionId = sdkInitializer.createSession(credentials, call.receive())
			call.respond(HttpStatusCode.OK, JsonPrimitive(sessionId))
		}
		delete("/{sessionId}") {
			val sessionId = call.parameters["sessionId"]!!
			sdkInitializer.destroySession(sessionId)
			call.respond(HttpStatusCode.NoContent)
		}
	}
}

private suspend fun RoutingContext.credentials(): Credentials {
	val authHeader = call.request.headers["Authorization"]

	val credentials: Credentials? = when {
		authHeader?.startsWith("Basic ") == true -> {
			authHeader.removePrefix("Basic ")
				.let { Base64.decode(it).decodeToString() }
				.split(":", limit = 2)
				.takeIf { it.size == 2 }
				?.let { BasicCredentials(it[0], it[1]) }
		}
		authHeader?.startsWith("Bearer ") == true -> {
			authHeader.removePrefix("Bearer ").takeIf { it.isNotBlank() }?.let { JwtCredentials(it) }
		}
		else -> null
	}

	if (credentials == null) {
		call.respond(HttpStatusCode.Unauthorized)
		throw IllegalStateException("Missing or invalid Authorization header")
	}

	return credentials
}