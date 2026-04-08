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
			call.respond(logic.createContact(credentials(), call.receive<DecryptedContact>()))
		}

		post("/createMany") {
			call.respond(logic.createContacts(credentials(), call.receive<List<DecryptedContact>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val contact = logic.getContact(credentials(), id)
			if (contact != null) {
				call.respond(contact)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getContacts(credentials(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyContact(credentials(), call.receive<DecryptedContact>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyContacts(credentials(), call.receive<List<DecryptedContact>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteContactById(credentials(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteContactsByIds(credentials(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteContactById(credentials(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeContactById(credentials(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchContactsBy(credentials(), call.receive<BaseFilterOptions<Contact>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchContactsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Contact>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterContactsBy(credentials(), call.receive<BaseFilterOptions<Contact>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterContactsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Contact>>()))
		}

		// Service-specific
		get("/service/{id}") {
			val id = call.parameters["id"]!!
			val service = logic.getService(credentials(), id)
			if (service != null) {
				call.respond(service)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/service/getMany") {
			call.respond(logic.getServices(credentials(), call.receive<List<String>>()))
		}

		post("/service/matchBy") {
			call.respond(logic.matchServicesBy(credentials(), call.receive<BaseFilterOptions<Service>>()))
		}

		post("/service/matchBySorted") {
			call.respond(logic.matchServicesBySorted(credentials(), call.receive<BaseSortableFilterOptions<Service>>()))
		}

		post("/service/filterBy") {
			call.respond(logic.filterServicesBy(credentials(), call.receive<BaseFilterOptions<Service>>()))
		}

		post("/service/filterBySorted") {
			call.respond(logic.filterServicesBySorted(credentials(), call.receive<BaseSortableFilterOptions<Service>>()))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createContactWithLinks(credentials(), call.receive<DecryptedContact>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createContactsWithLinks(credentials(), call.receive<List<DecryptedContact>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getContactWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getContactsWithLinks(credentials(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyContactWithLinks(credentials(), call.receive<DecryptedContact>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyContactsWithLinks(credentials(), call.receive<List<DecryptedContact>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteContactByIdWithLinks(credentials(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterContactsByWithLinks(credentials(), call.receive<BaseFilterOptions<Contact>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterContactsBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<Contact>>()))
		}

		get("/service/{id}/withLinks") {
			val result = logic.getServiceWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/service/getMany/withLinks") {
			call.respond(logic.getServicesWithLinks(credentials(), call.receive<List<String>>()))
		}

		post("/service/filterBy/withLinks") {
			call.respond(logic.filterServicesByWithLinks(credentials(), call.receive<BaseFilterOptions<Service>>()))
		}

		post("/service/filterBySorted/withLinks") {
			call.respond(logic.filterServicesBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<Service>>()))
		}
	}
}
