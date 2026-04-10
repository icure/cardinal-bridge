package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.FormLogic
import com.icure.cardinal.bridge.serialization.FilterSerializers
import com.icure.cardinal.sdk.model.DecryptedForm
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

fun Route.formRoutes(logic: FormLogic) {
	route("/form") {
		// CRUD
		post("/create") {
			call.respond(logic.createForm(sessionId(), call.receive<DecryptedForm>()))
		}

		post("/createMany") {
			call.respond(logic.createForms(sessionId(), call.receive<List<DecryptedForm>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val form = logic.getForm(sessionId(), id)
			if (form != null) {
				call.respond(form)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getForms(sessionId(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyForm(sessionId(), call.receive<DecryptedForm>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyForms(sessionId(), call.receive<List<DecryptedForm>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteFormById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteFormsByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteFormById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeFormById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchFormsBy(sessionId(), call.receiveJson(FilterSerializers.form)))
		}

		post("/filterBy") {
			call.respond(logic.filterFormsBy(sessionId(), call.receiveJson(FilterSerializers.form)))
		}

		// Form-specific
		get("/latestByUniqueId/{uniqueId}") {
			val uniqueId = call.parameters["uniqueId"]!!
			call.respond(logic.getLatestFormByUniqueId(sessionId(), uniqueId))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createFormWithLinks(sessionId(), call.receive<DecryptedForm>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createFormsWithLinks(sessionId(), call.receive<List<DecryptedForm>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getFormWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getFormsWithLinks(sessionId(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyFormWithLinks(sessionId(), call.receive<DecryptedForm>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyFormsWithLinks(sessionId(), call.receive<List<DecryptedForm>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteFormByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterFormsByWithLinks(sessionId(), call.receiveJson(FilterSerializers.form)))
		}

		get("/latestByUniqueId/{uniqueId}/withLinks") {
			call.respond(logic.getLatestFormByUniqueIdWithLinks(sessionId(), call.parameters["uniqueId"]!!))
		}
	}
}
