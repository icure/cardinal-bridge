package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.CalendarItemLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.CalendarItem
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
			call.respond(logic.createCalendarItem(credentials(), call.receive<DecryptedCalendarItem>()))
		}

		post("/createMany") {
			call.respond(logic.createCalendarItems(credentials(), call.receive<List<DecryptedCalendarItem>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val calendarItem = logic.getCalendarItem(credentials(), id)
			if (calendarItem != null) {
				call.respond(calendarItem)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getCalendarItems(credentials(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyCalendarItem(credentials(), call.receive<DecryptedCalendarItem>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyCalendarItems(credentials(), call.receive<List<DecryptedCalendarItem>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteCalendarItemById(credentials(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteCalendarItemsByIds(credentials(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteCalendarItemById(credentials(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeCalendarItemById(credentials(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchCalendarItemsBy(credentials(), call.receive<BaseFilterOptions<CalendarItem>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchCalendarItemsBySorted(credentials(), call.receive<BaseSortableFilterOptions<CalendarItem>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterCalendarItemsBy(credentials(), call.receive<BaseFilterOptions<CalendarItem>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterCalendarItemsBySorted(credentials(), call.receive<BaseSortableFilterOptions<CalendarItem>>()))
		}

		// CalendarItem-specific
		post("/book") {
			call.respond(logic.bookCalendarItemCheckingAvailability(credentials(), call.receive<DecryptedCalendarItem>()))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createCalendarItemWithLinks(credentials(), call.receive<DecryptedCalendarItem>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createCalendarItemsWithLinks(credentials(), call.receive<List<DecryptedCalendarItem>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getCalendarItemWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getCalendarItemsWithLinks(credentials(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyCalendarItemWithLinks(credentials(), call.receive<DecryptedCalendarItem>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyCalendarItemsWithLinks(credentials(), call.receive<List<DecryptedCalendarItem>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteCalendarItemByIdWithLinks(credentials(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterCalendarItemsByWithLinks(credentials(), call.receive<BaseFilterOptions<CalendarItem>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterCalendarItemsBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<CalendarItem>>()))
		}

		post("/book/withLinks") {
			call.respond(logic.bookCalendarItemCheckingAvailabilityWithLinks(credentials(), call.receive<DecryptedCalendarItem>()))
		}
	}
}
