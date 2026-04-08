package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.HealthElementLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedHealthElement
import com.icure.cardinal.sdk.model.HealthElement
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

fun Route.healthElementRoutes(logic: HealthElementLogic) {
	route("/healthElement") {
		// CRUD
		post("/create") {
			call.respond(logic.createHealthElement(sessionId(), call.receive<DecryptedHealthElement>()))
		}

		post("/createMany") {
			call.respond(logic.createHealthElements(sessionId(), call.receive<List<DecryptedHealthElement>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val healthElement = logic.getHealthElement(sessionId(), id)
			if (healthElement != null) {
				call.respond(healthElement)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getHealthElements(sessionId(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyHealthElement(sessionId(), call.receive<DecryptedHealthElement>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyHealthElements(sessionId(), call.receive<List<DecryptedHealthElement>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteHealthElementById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteHealthElementsByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteHealthElementById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeHealthElementById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchHealthElementsBy(sessionId(), call.receive<BaseFilterOptions<HealthElement>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchHealthElementsBySorted(sessionId(), call.receive<BaseSortableFilterOptions<HealthElement>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterHealthElementsBy(sessionId(), call.receive<BaseFilterOptions<HealthElement>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterHealthElementsBySorted(sessionId(), call.receive<BaseSortableFilterOptions<HealthElement>>()))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createHealthElementWithLinks(sessionId(), call.receive<DecryptedHealthElement>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createHealthElementsWithLinks(sessionId(), call.receive<List<DecryptedHealthElement>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getHealthElementWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getHealthElementsWithLinks(sessionId(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyHealthElementWithLinks(sessionId(), call.receive<DecryptedHealthElement>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyHealthElementsWithLinks(sessionId(), call.receive<List<DecryptedHealthElement>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteHealthElementByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterHealthElementsByWithLinks(sessionId(), call.receive<BaseFilterOptions<HealthElement>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterHealthElementsBySortedWithLinks(sessionId(), call.receive<BaseSortableFilterOptions<HealthElement>>()))
		}
	}
}
