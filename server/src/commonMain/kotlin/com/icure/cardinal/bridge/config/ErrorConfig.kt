package com.icure.cardinal.bridge.config

import com.icure.cardinal.sdk.utils.RequestStatusException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText

private fun makeErrorJson(
	type: String,
	vararg args: Pair<String, Any?>
): Map<String, Any> =
	mapOf(
		"type" to type,
		*args.filter { it.second != null }.toTypedArray()
	) as Map<String, Any>

fun Application.configureErrorHandler() {
	install(StatusPages) {
		exception<RequestStatusException> { call, cause ->
			call.respond(
				status = HttpStatusCode.fromValue(cause.statusCode),
				message = makeErrorJson(
					"RequestStatusException",
					"requestMethod" to cause.requestMethod.value,
					"requestUrl" to cause.url,
					"responseBody" to cause.body
				)
			)
		}

		exception<IllegalArgumentException> { call, cause ->
			call.respond(
				status = HttpStatusCode.BadRequest,
				message = makeErrorJson(
					"IllegalArgumentException",
					"message" to cause.message
				)
			)
		}
	}
}