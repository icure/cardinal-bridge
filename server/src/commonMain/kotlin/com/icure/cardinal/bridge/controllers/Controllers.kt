package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.BridgeLogic
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.model.Patient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Application.configureRouting() {
	routing {
		val bridgeLogic = BridgeLogic()

		route("/bridge") {

			get("/user/current") {
				call.respond(
					bridgeLogic.getCurrentUser(),
				)
			}

			post("/patient/matchBy") {
				call.respond(bridgeLogic.patientMatchBy(
                    credentials = credentials(),
                    filter = call.receive<BaseFilterOptions<Patient>>()
                ))
			}

			get("/health") {
				call.respond(HttpStatusCode.OK)
			}
		}
	}
}

private suspend fun RoutingContext.credentials(): Credentials {
	val authHeader = call.request.headers["Authorization"]
	val credentials = authHeader
		?.removePrefix("Basic ")
		?.let { Base64.decode(it).decodeToString() }
		?.split(":", limit = 2)
		?.takeIf { it.size == 2 }
		?.let { Credentials(it[0], it[1]) }

	if (credentials == null) {
		this.call.respond(HttpStatusCode.Unauthorized)
		throw IllegalStateException("Missing or invalid Authorization header")
	}

	return credentials
}
