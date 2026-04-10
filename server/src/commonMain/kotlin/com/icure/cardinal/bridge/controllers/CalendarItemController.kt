package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.CalendarItemLogic
import com.icure.cardinal.bridge.serialization.FilterSerializers
import com.icure.cardinal.sdk.model.DecryptedCalendarItem
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.calendarItemRoutes(logic: CalendarItemLogic) {
	route("/calendarItem") {
		// CRUD
		post("/create") {
			call.respond(logic.createCalendarItem(sessionId(), call.receive<DecryptedCalendarItem>()))
		}

		post("/createMany") {
			call.respond(logic.createCalendarItems(sessionId(), call.receive<List<DecryptedCalendarItem>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val calendarItem = logic.getCalendarItem(sessionId(), id)
			if (calendarItem != null) {
				call.respond(calendarItem)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getCalendarItems(sessionId(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyCalendarItem(sessionId(), call.receive<DecryptedCalendarItem>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyCalendarItems(sessionId(), call.receive<List<DecryptedCalendarItem>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteCalendarItemById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteCalendarItemsByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteCalendarItemById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeCalendarItemById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchCalendarItemsBy(sessionId(), call.receiveJson(FilterSerializers.calendarItem)))
		}

		post("/filterBy") {
			call.respond(logic.filterCalendarItemsBy(sessionId(), call.receiveJson(FilterSerializers.calendarItem)))
		}

		// CalendarItem-specific
		post("/book") {
			call.respond(logic.bookCalendarItemCheckingAvailability(sessionId(), call.receive<DecryptedCalendarItem>()))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createCalendarItemWithLinks(sessionId(), call.receive<DecryptedCalendarItem>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createCalendarItemsWithLinks(sessionId(), call.receive<List<DecryptedCalendarItem>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getCalendarItemWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getCalendarItemsWithLinks(sessionId(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyCalendarItemWithLinks(sessionId(), call.receive<DecryptedCalendarItem>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyCalendarItemsWithLinks(sessionId(), call.receive<List<DecryptedCalendarItem>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteCalendarItemByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterCalendarItemsByWithLinks(sessionId(), call.receiveJson(FilterSerializers.calendarItem)))
		}

		post("/book/withLinks") {
			call.respond(logic.bookCalendarItemCheckingAvailabilityWithLinks(sessionId(), call.receive<DecryptedCalendarItem>()))
		}
	}
}
