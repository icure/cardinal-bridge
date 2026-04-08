package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.logic.CalendarItemLogic
import com.icure.cardinal.bridge.logic.ContactLogic
import com.icure.cardinal.bridge.logic.DocumentLogic
import com.icure.cardinal.bridge.logic.FormLogic
import com.icure.cardinal.bridge.logic.HealthElementLogic
import com.icure.cardinal.bridge.logic.MessageLogic
import com.icure.cardinal.bridge.logic.PatientLogic
import com.icure.cardinal.bridge.model.BasicCredentials
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.bridge.model.JwtCredentials
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
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

			get("/health") {
				call.respond(HttpStatusCode.OK)
			}
		}
	}
}

@OptIn(ExperimentalEncodingApi::class)
internal suspend fun RoutingContext.credentials(): Credentials {
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
