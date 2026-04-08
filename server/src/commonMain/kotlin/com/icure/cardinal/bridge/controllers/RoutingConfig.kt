package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.logic.CalendarItemLogic
import com.icure.cardinal.bridge.logic.ContactLogic
import com.icure.cardinal.bridge.logic.DocumentLogic
import com.icure.cardinal.bridge.logic.FormLogic
import com.icure.cardinal.bridge.logic.HealthElementLogic
import com.icure.cardinal.bridge.logic.MessageLogic
import com.icure.cardinal.bridge.logic.PatientLogic
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
fun Application.configureRouting(
	sdkInitializer: CardinalSdkInitializer
) {
	routing {
		route("/bridge") {
			patientRoutes(PatientLogic(sdkInitializer))
			contactRoutes(ContactLogic(sdkInitializer))
			formRoutes(FormLogic(sdkInitializer))
			healthElementRoutes(HealthElementLogic(sdkInitializer))
			messageRoutes(MessageLogic(sdkInitializer))
			documentRoutes(DocumentLogic(sdkInitializer))
			calendarItemRoutes(CalendarItemLogic(sdkInitializer))
			sessionRoutes(sdkInitializer)

			get("/health") {
				call.respond(HttpStatusCode.NoContent)
			}
		}
	}
}

@OptIn(ExperimentalEncodingApi::class)
internal fun RoutingContext.sessionId() =
	requireNotNull(call.request.headers["Session"]) {
		"Missing required session header"
	}

