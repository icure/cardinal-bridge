package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.MessageLogic
import com.icure.cardinal.bridge.serialization.FilterSerializers
import com.icure.cardinal.bridge.serialization.SerializationConfig
import com.icure.cardinal.sdk.model.Message
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import com.icure.cardinal.sdk.model.DecryptedMessage
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

fun Route.messageRoutes(logic: MessageLogic) {
	route("/message") {
		// CRUD
/*
		post("/create") {
			call.respond(logic.createMessage(sessionId(), call.receive<DecryptedMessage>()))
		}

		post("/createMany") {
			call.respond(logic.createMessages(sessionId(), call.receive<List<DecryptedMessage>>()))
		}
*/

		get("/{id}") {
			val id = call.parameters["id"]!!
			val message = logic.getMessage(sessionId(), id)
			if (message != null) {
				call.respond(
					TextContent(
						SerializationConfig.serverJson.encodeToString(
							PolymorphicSerializer(Message::class),
							message
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
					ListSerializer(PolymorphicSerializer(Message::class)),
					logic.getMessages(sessionId(), call.receive<List<String>>())
				),
				ContentType.Application.Json
			))
		}

/*
		put("/modify") {
			call.respond(logic.modifyMessage(sessionId(), call.receive<DecryptedMessage>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyMessages(sessionId(), call.receive<List<DecryptedMessage>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteMessageById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteMessagesByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteMessageById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeMessageById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}
*/

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchMessagesBy(sessionId(), call.receiveJson(FilterSerializers.message)))
		}

		post("/filterBy") {
			call.respond(TextContent(
				SerializationConfig.serverJson.encodeToString(
					ListSerializer(PolymorphicSerializer(Message::class)),
					logic.filterMessagesBy(sessionId(), call.receiveJson(FilterSerializers.message))
				),
				ContentType.Application.Json
			))
		}

		// Message-specific
/*
		post("/createInTopic") {
			call.respond(logic.createMessageInTopic(sessionId(), call.receive<DecryptedMessage>()))
		}
*/

		// WithLinks variants
/*
		post("/create/withLinks") {
			call.respond(logic.createMessageWithLinks(sessionId(), call.receive<DecryptedMessage>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createMessagesWithLinks(sessionId(), call.receive<List<DecryptedMessage>>()))
		}
*/

		get("/{id}/withLinks") {
			val result = logic.getMessageWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getMessagesWithLinks(sessionId(), call.receive<List<String>>()))
		}

/*
		put("/modify/withLinks") {
			call.respond(logic.modifyMessageWithLinks(sessionId(), call.receive<DecryptedMessage>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyMessagesWithLinks(sessionId(), call.receive<List<DecryptedMessage>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteMessageByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}
*/

		post("/filterBy/withLinks") {
			call.respond(logic.filterMessagesByWithLinks(sessionId(), call.receiveJson(FilterSerializers.message)))
		}

/*
		post("/createInTopic/withLinks") {
			call.respond(logic.createMessageInTopicWithLinks(sessionId(), call.receive<DecryptedMessage>()))
		}
*/
	}
}
