package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.MessageLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedMessage
import com.icure.cardinal.sdk.model.Message
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
		post("/create") {
			call.respond(logic.createMessage(credentials(), call.receive<DecryptedMessage>()))
		}

		post("/createMany") {
			call.respond(logic.createMessages(credentials(), call.receive<List<DecryptedMessage>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val message = logic.getMessage(credentials(), id)
			if (message != null) {
				call.respond(message)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getMessages(credentials(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyMessage(credentials(), call.receive<DecryptedMessage>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyMessages(credentials(), call.receive<List<DecryptedMessage>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deleteMessageById(credentials(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deleteMessagesByIds(credentials(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeleteMessageById(credentials(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgeMessageById(credentials(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchMessagesBy(credentials(), call.receive<BaseFilterOptions<Message>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchMessagesBySorted(credentials(), call.receive<BaseSortableFilterOptions<Message>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterMessagesBy(credentials(), call.receive<BaseFilterOptions<Message>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterMessagesBySorted(credentials(), call.receive<BaseSortableFilterOptions<Message>>()))
		}

		// Message-specific
		post("/createInTopic") {
			call.respond(logic.createMessageInTopic(credentials(), call.receive<DecryptedMessage>()))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createMessageWithLinks(credentials(), call.receive<DecryptedMessage>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createMessagesWithLinks(credentials(), call.receive<List<DecryptedMessage>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getMessageWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getMessagesWithLinks(credentials(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyMessageWithLinks(credentials(), call.receive<DecryptedMessage>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyMessagesWithLinks(credentials(), call.receive<List<DecryptedMessage>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeleteMessageByIdWithLinks(credentials(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterMessagesByWithLinks(credentials(), call.receive<BaseFilterOptions<Message>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterMessagesBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<Message>>()))
		}

		post("/createInTopic/withLinks") {
			call.respond(logic.createMessageInTopicWithLinks(credentials(), call.receive<DecryptedMessage>()))
		}
	}
}
