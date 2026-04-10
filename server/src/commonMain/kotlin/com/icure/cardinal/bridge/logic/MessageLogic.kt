package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.model.DecryptedMessage
import com.icure.cardinal.sdk.model.Message
import com.icure.cardinal.bridge.model.MessageWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.utils.InternalIcureApi

class MessageLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

/*
	suspend fun createMessage(sessionId: String, entity: DecryptedMessage): DecryptedMessage =
		sdk(sessionId).message.createMessage(entity)

	suspend fun createMessages(sessionId: String, entities: List<DecryptedMessage>): List<DecryptedMessage> =
		sdk(sessionId).message.createMessages(entities)
*/

	suspend fun getMessage(sessionId: String, entityId: String): Message? =
		sdk(sessionId).message.tryAndRecover.getMessage(entityId)

	suspend fun getMessages(sessionId: String, entityIds: List<String>): List<Message> =
		sdk(sessionId).message.tryAndRecover.getMessages(entityIds)

/*
	suspend fun modifyMessage(sessionId: String, entity: DecryptedMessage): DecryptedMessage =
		sdk(sessionId).message.modifyMessage(entity)

	suspend fun modifyMessages(sessionId: String, entities: List<DecryptedMessage>): List<DecryptedMessage> =
		sdk(sessionId).message.modifyMessages(entities)

	suspend fun deleteMessageById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).message.deleteMessageById(entityId, rev)

	suspend fun deleteMessagesByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).message.deleteMessagesByIds(entityIds)

	suspend fun undeleteMessageById(sessionId: String, id: String, rev: String): DecryptedMessage =
		sdk(sessionId).message.undeleteMessageById(id, rev)

	suspend fun purgeMessageById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).message.purgeMessageById(id, rev)
*/

	// Filter/Match

	@OptIn(InternalIcureApi::class)
	suspend fun matchMessagesBy(sessionId: String, filter: AbstractFilter<Message>): List<String> =
		raw(sessionId).message.matchMessagesBy(filter).successBody()

	suspend fun filterMessagesBy(sessionId: String, filter: AbstractFilter<Message>): List<Message> =
		getFromMatches(matchMessagesBy(sessionId, filter)) { sdk(sessionId).message.tryAndRecover.getMessages(it) }

	// Message-specific

/*
	suspend fun createMessageInTopic(sessionId: String, entity: DecryptedMessage): DecryptedMessage =
		sdk(sessionId).message.createMessageInTopic(entity)
*/

	// WithLinks

	private suspend fun withLinks(sessionId: String, message: Message): MessageWithLinks {
		val sdk = sdk(sessionId)
		val patientIds = sdk.message.decryptPatientIdOf(message).map { it.entityId }.toSet()
		val ownSecretIds = sdk.message.getSecretIdsOf(message).keys.toSet()
		return MessageWithLinks(message, patientIds, ownSecretIds)
	}

/*
	suspend fun createMessageWithLinks(sessionId: String, entity: DecryptedMessage): MessageWithLinks =
		withLinks(sessionId, createMessage(sessionId, entity))

	suspend fun createMessagesWithLinks(sessionId: String, entities: List<DecryptedMessage>): List<MessageWithLinks> =
		createMessages(sessionId, entities).map { withLinks(sessionId, it) }
*/

	suspend fun getMessageWithLinks(sessionId: String, entityId: String): MessageWithLinks? =
		getMessage(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getMessagesWithLinks(sessionId: String, entityIds: List<String>): List<MessageWithLinks> =
		getMessages(sessionId, entityIds).map { withLinks(sessionId, it) }

/*
	suspend fun modifyMessageWithLinks(sessionId: String, entity: DecryptedMessage): MessageWithLinks =
		withLinks(sessionId, modifyMessage(sessionId, entity))

	suspend fun modifyMessagesWithLinks(sessionId: String, entities: List<DecryptedMessage>): List<MessageWithLinks> =
		modifyMessages(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteMessageByIdWithLinks(sessionId: String, id: String, rev: String): MessageWithLinks =
		withLinks(sessionId, undeleteMessageById(sessionId, id, rev))
*/

	suspend fun filterMessagesByWithLinks(sessionId: String, filter: AbstractFilter<Message>): List<MessageWithLinks> =
		filterMessagesBy(sessionId, filter).map { withLinks(sessionId, it) }

/*
	suspend fun createMessageInTopicWithLinks(sessionId: String, entity: DecryptedMessage): MessageWithLinks =
		withLinks(sessionId, createMessageInTopic(sessionId, entity))
*/
}
