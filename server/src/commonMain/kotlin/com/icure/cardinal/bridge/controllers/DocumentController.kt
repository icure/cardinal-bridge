package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.DocumentLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedDocument
import com.icure.cardinal.sdk.model.Document
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.documentRoutes(logic: DocumentLogic) {
	route("/document") {
		// CRUD
		post("/create") {
			call.respond(logic.createDocument(credentials(), call.receive<DecryptedDocument>()))
		}

		post("/createMany") {
			call.respond(logic.createDocuments(credentials(), call.receive<List<DecryptedDocument>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val document = logic.getDocument(credentials(), id)
			if (document != null) {
				call.respond(document)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getDocuments(credentials(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyDocument(credentials(), call.receive<DecryptedDocument>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyDocuments(credentials(), call.receive<List<DecryptedDocument>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteDocumentById(credentials(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteDocumentsByIds(credentials(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteDocumentById(credentials(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeDocumentById(credentials(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchDocumentsBy(credentials(), call.receive<BaseFilterOptions<Document>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchDocumentsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Document>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterDocumentsBy(credentials(), call.receive<BaseFilterOptions<Document>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterDocumentsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Document>>()))
		}

		// Main attachment
		get("/mainAttachment/{documentId}") {
			val documentId = call.parameters["documentId"]!!
			val result = logic.getRawMainAttachment(credentials(), documentId)
			call.respondBytes(result)
		}

		put("/mainAttachment/{documentId}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val rev = call.parameters["rev"]!!
			val utis = call.request.queryParameters["utis"]?.split(",")
			val encrypted = call.request.queryParameters["encrypted"]?.toBoolean() ?: false
			val attachment = call.receive<ByteArray>()
			call.respond(logic.setRawMainAttachment(credentials(), documentId, rev, utis, attachment, encrypted))
		}

		delete("/mainAttachment/{documentId}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteMainAttachment(credentials(), documentId, rev))
		}

		// Secondary attachment
		get("/secondaryAttachment/{documentId}/{key}") {
			val documentId = call.parameters["documentId"]!!
			val key = call.parameters["key"]!!
			val result = logic.getRawSecondaryAttachment(credentials(), documentId, key)
			call.respondBytes(result)
		}

		put("/secondaryAttachment/{documentId}/{key}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val key = call.parameters["key"]!!
			val rev = call.parameters["rev"]!!
			val utis = call.request.queryParameters["utis"]?.split(",")
			val encrypted = call.request.queryParameters["encrypted"]?.toBoolean() ?: false
			val attachment = call.receive<ByteArray>()
			call.respond(logic.setRawSecondaryAttachment(credentials(), documentId, key, rev, utis, attachment, encrypted))
		}

		delete("/secondaryAttachment/{documentId}/{key}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val key = call.parameters["key"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteSecondaryAttachment(credentials(), documentId, key, rev))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createDocumentWithLinks(credentials(), call.receive<DecryptedDocument>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createDocumentsWithLinks(credentials(), call.receive<List<DecryptedDocument>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getDocumentWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getDocumentsWithLinks(credentials(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyDocumentWithLinks(credentials(), call.receive<DecryptedDocument>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyDocumentsWithLinks(credentials(), call.receive<List<DecryptedDocument>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteDocumentByIdWithLinks(credentials(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterDocumentsByWithLinks(credentials(), call.receive<BaseFilterOptions<Document>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterDocumentsBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<Document>>()))
		}
	}
}
