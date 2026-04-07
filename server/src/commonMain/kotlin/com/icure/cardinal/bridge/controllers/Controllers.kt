package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.logic.BridgeLogic
import com.icure.cardinal.bridge.model.UsernameTokenKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
	routing {
		val bridgeLogic = BridgeLogic()

		route("/bridge") {

			get("/user/current") {
				call.respond(
					bridgeLogic.getCurrentUser(),
				)
			}

			get("/health") {
				call.respond(HttpStatusCode.OK)
			}
		}
	}
}