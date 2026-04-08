package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.FormLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedForm
import com.icure.cardinal.sdk.model.Form
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
			call.respond(logic.createForm(credentials(), call.receive<DecryptedForm>()))
		}

		post("/createMany") {
			call.respond(logic.createForms(credentials(), call.receive<List<DecryptedForm>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val form = logic.getForm(credentials(), id)
			if (form != null) {
				call.respond(form)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getForms(credentials(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyForm(credentials(), call.receive<DecryptedForm>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyForms(credentials(), call.receive<List<DecryptedForm>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteFormById(credentials(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteFormsByIds(credentials(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteFormById(credentials(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeFormById(credentials(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchFormsBy(credentials(), call.receive<BaseFilterOptions<Form>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchFormsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Form>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterFormsBy(credentials(), call.receive<BaseFilterOptions<Form>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterFormsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Form>>()))
		}

		// Form-specific
		get("/latestByUniqueId/{uniqueId}") {
			val uniqueId = call.parameters["uniqueId"]!!
			call.respond(logic.getLatestFormByUniqueId(credentials(), uniqueId))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createFormWithLinks(credentials(), call.receive<DecryptedForm>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createFormsWithLinks(credentials(), call.receive<List<DecryptedForm>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getFormWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getFormsWithLinks(credentials(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyFormWithLinks(credentials(), call.receive<DecryptedForm>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyFormsWithLinks(credentials(), call.receive<List<DecryptedForm>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteFormByIdWithLinks(credentials(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterFormsByWithLinks(credentials(), call.receive<BaseFilterOptions<Form>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterFormsBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<Form>>()))
		}

		get("/latestByUniqueId/{uniqueId}/withLinks") {
			call.respond(logic.getLatestFormByUniqueIdWithLinks(credentials(), call.parameters["uniqueId"]!!))
		}
	}
}
