package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.DocumentLogic
import com.icure.cardinal.bridge.serialization.FilterSerializers
import com.icure.cardinal.bridge.serialization.SerializationConfig
import com.icure.cardinal.sdk.model.Document
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import com.icure.cardinal.sdk.model.DecryptedDocument
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
/*
		post("/create") {
			call.respond(logic.createDocument(sessionId(), call.receive<DecryptedDocument>()))
		}

		post("/createMany") {
			call.respond(logic.createDocuments(sessionId(), call.receive<List<DecryptedDocument>>()))
		}
*/

		get("/{id}") {
			val id = call.parameters["id"]!!
			val document = logic.getDocument(sessionId(), id)
			if (document != null) {
				call.respond(
					TextContent(
						SerializationConfig.serverJson.encodeToString(
							PolymorphicSerializer(Document::class),
							document
						),
						ContentType.Application.Json
					)
				)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(TextContent(
				SerializationConfig.serverJson.encodeToString(
					ListSerializer(PolymorphicSerializer(Document::class)),
					logic.getDocuments(sessionId(), call.receive<List<String>>())
				),
				ContentType.Application.Json
			))
		}

/*
		put("/modify") {
			call.respond(logic.modifyDocument(sessionId(), call.receive<DecryptedDocument>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyDocuments(sessionId(), call.receive<List<DecryptedDocument>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteDocumentById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteDocumentsByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteDocumentById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeDocumentById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}
*/

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchDocumentsBy(sessionId(), call.receiveJson(FilterSerializers.document)))
		}

		post("/filterBy") {
			call.respond(TextContent(
				SerializationConfig.serverJson.encodeToString(
					ListSerializer(PolymorphicSerializer(Document::class)),
					logic.filterDocumentsBy(sessionId(), call.receiveJson(FilterSerializers.document))
				),
				ContentType.Application.Json
			))
		}

		// Main attachment
		get("/mainAttachment/{documentId}") {
			val documentId = call.parameters["documentId"]!!
			val result = logic.getRawMainAttachment(sessionId(), documentId)
			call.respondBytes(result)
		}

/*
		put("/mainAttachment/{documentId}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val rev = call.parameters["rev"]!!
			val utis = call.request.queryParameters["utis"]?.split(",")
			val encrypted = call.request.queryParameters["encrypted"]?.toBoolean() ?: false
			val attachment = call.receive<ByteArray>()
			call.respond(logic.setRawMainAttachment(sessionId(), documentId, rev, utis, attachment, encrypted))
		}

		delete("/mainAttachment/{documentId}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteMainAttachment(sessionId(), documentId, rev))
		}
*/

		// Secondary attachment
		get("/secondaryAttachment/{documentId}/{key}") {
			val documentId = call.parameters["documentId"]!!
			val key = call.parameters["key"]!!
			val result = logic.getRawSecondaryAttachment(sessionId(), documentId, key)
			call.respondBytes(result)
		}

/*
		put("/secondaryAttachment/{documentId}/{key}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val key = call.parameters["key"]!!
			val rev = call.parameters["rev"]!!
			val utis = call.request.queryParameters["utis"]?.split(",")
			val encrypted = call.request.queryParameters["encrypted"]?.toBoolean() ?: false
			val attachment = call.receive<ByteArray>()
			call.respond(logic.setRawSecondaryAttachment(sessionId(), documentId, key, rev, utis, attachment, encrypted))
		}

		delete("/secondaryAttachment/{documentId}/{key}/{rev}") {
			val documentId = call.parameters["documentId"]!!
			val key = call.parameters["key"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteSecondaryAttachment(sessionId(), documentId, key, rev))
		}
*/

		// WithLinks variants
/*
		post("/create/withLinks") {
			call.respond(logic.createDocumentWithLinks(sessionId(), call.receive<DecryptedDocument>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createDocumentsWithLinks(sessionId(), call.receive<List<DecryptedDocument>>()))
		}
*/

		get("/{id}/withLinks") {
			val result = logic.getDocumentWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getDocumentsWithLinks(sessionId(), call.receive<List<String>>()))
		}

/*
		put("/modify/withLinks") {
			call.respond(logic.modifyDocumentWithLinks(sessionId(), call.receive<DecryptedDocument>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyDocumentsWithLinks(sessionId(), call.receive<List<DecryptedDocument>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteDocumentByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}
*/

		post("/filterBy/withLinks") {
			call.respond(logic.filterDocumentsByWithLinks(sessionId(), call.receiveJson(FilterSerializers.document)))
		}
	}
}
