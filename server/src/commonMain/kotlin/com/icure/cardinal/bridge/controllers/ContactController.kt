package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.ContactLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.Contact
import com.icure.cardinal.sdk.model.DecryptedContact
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.embed.Service
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.contactRoutes(logic: ContactLogic) {
	route("/contact") {
		// CRUD
		post("/create") {
			call.respond(logic.createContact(sessionId(), call.receive<DecryptedContact>()))
		}

		post("/createMany") {
			call.respond(logic.createContacts(sessionId(), call.receive<List<DecryptedContact>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val contact = logic.getContact(sessionId(), id)
			if (contact != null) {
				call.respond(contact)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getContacts(sessionId(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyContact(sessionId(), call.receive<DecryptedContact>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyContacts(sessionId(), call.receive<List<DecryptedContact>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteContactById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteContactsByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteContactById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeContactById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchContactsBy(sessionId(), call.receive<BaseFilterOptions<Contact>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchContactsBySorted(sessionId(), call.receive<BaseSortableFilterOptions<Contact>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterContactsBy(sessionId(), call.receive<BaseFilterOptions<Contact>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterContactsBySorted(sessionId(), call.receive<BaseSortableFilterOptions<Contact>>()))
		}

		// Service-specific
		get("/service/{id}") {
			val id = call.parameters["id"]!!
			val service = logic.getService(sessionId(), id)
			if (service != null) {
				call.respond(service)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/service/getMany") {
			call.respond(logic.getServices(sessionId(), call.receive<List<String>>()))
		}

		post("/service/matchBy") {
			call.respond(logic.matchServicesBy(sessionId(), call.receive<BaseFilterOptions<Service>>()))
		}

		post("/service/matchBySorted") {
			call.respond(logic.matchServicesBySorted(sessionId(), call.receive<BaseSortableFilterOptions<Service>>()))
		}

		post("/service/filterBy") {
			call.respond(logic.filterServicesBy(sessionId(), call.receive<BaseFilterOptions<Service>>()))
		}

		post("/service/filterBySorted") {
			call.respond(logic.filterServicesBySorted(sessionId(), call.receive<BaseSortableFilterOptions<Service>>()))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createContactWithLinks(sessionId(), call.receive<DecryptedContact>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createContactsWithLinks(sessionId(), call.receive<List<DecryptedContact>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getContactWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getContactsWithLinks(sessionId(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyContactWithLinks(sessionId(), call.receive<DecryptedContact>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyContactsWithLinks(sessionId(), call.receive<List<DecryptedContact>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteContactByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterContactsByWithLinks(sessionId(), call.receive<BaseFilterOptions<Contact>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterContactsBySortedWithLinks(sessionId(), call.receive<BaseSortableFilterOptions<Contact>>()))
		}

		get("/service/{id}/withLinks") {
			val result = logic.getServiceWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/service/getMany/withLinks") {
			call.respond(logic.getServicesWithLinks(sessionId(), call.receive<List<String>>()))
		}

		post("/service/filterBy/withLinks") {
			call.respond(logic.filterServicesByWithLinks(sessionId(), call.receive<BaseFilterOptions<Service>>()))
		}

		post("/service/filterBySorted/withLinks") {
			call.respond(logic.filterServicesBySortedWithLinks(sessionId(), call.receive<BaseSortableFilterOptions<Service>>()))
		}
	}
}
