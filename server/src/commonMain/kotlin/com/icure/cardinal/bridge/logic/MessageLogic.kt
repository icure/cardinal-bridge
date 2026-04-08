package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedMessage
import com.icure.cardinal.sdk.model.Message
import com.icure.cardinal.bridge.model.MessageWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class MessageLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

	suspend fun createMessage(sessionId: String, entity: DecryptedMessage): DecryptedMessage =
		sdk(sessionId).message.createMessage(entity)

	suspend fun createMessages(sessionId: String, entities: List<DecryptedMessage>): List<DecryptedMessage> =
		sdk(sessionId).message.createMessages(entities)

	suspend fun getMessage(sessionId: String, entityId: String): DecryptedMessage? =
		sdk(sessionId).message.getMessage(entityId)

	suspend fun getMessages(sessionId: String, entityIds: List<String>): List<DecryptedMessage> =
		sdk(sessionId).message.getMessages(entityIds)

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

	// Filter/Match

	suspend fun matchMessagesBy(sessionId: String, filter: BaseFilterOptions<Message>): List<String> =
		sdk(sessionId).message.matchMessagesBy(filter)

	suspend fun matchMessagesBySorted(sessionId: String, filter: BaseSortableFilterOptions<Message>): List<String> =
		sdk(sessionId).message.matchMessagesBySorted(filter)

	suspend fun filterMessagesBy(sessionId: String, filter: BaseFilterOptions<Message>): List<DecryptedMessage> {
		val iterator = sdk(sessionId).message.filterMessagesBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterMessagesBySorted(sessionId: String, filter: BaseSortableFilterOptions<Message>): List<DecryptedMessage> {
		val iterator = sdk(sessionId).message.filterMessagesBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// Message-specific

	suspend fun createMessageInTopic(sessionId: String, entity: DecryptedMessage): DecryptedMessage =
		sdk(sessionId).message.createMessageInTopic(entity)

	// WithLinks

	private suspend fun withLinks(sessionId: String, message: DecryptedMessage): MessageWithLinks {
		val sdk = sdk(sessionId)
		val patientIds = sdk.message.decryptPatientIdOf(message).map { it.entityId }.toSet()
		val ownSecretIds = sdk.message.getSecretIdsOf(message).values.flatMap { refs -> refs.map { it.entityId } }.toSet()
		return MessageWithLinks(message, patientIds, ownSecretIds)
	}

	suspend fun createMessageWithLinks(sessionId: String, entity: DecryptedMessage): MessageWithLinks =
		withLinks(sessionId, createMessage(sessionId, entity))

	suspend fun createMessagesWithLinks(sessionId: String, entities: List<DecryptedMessage>): List<MessageWithLinks> =
		createMessages(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun getMessageWithLinks(sessionId: String, entityId: String): MessageWithLinks? =
		getMessage(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getMessagesWithLinks(sessionId: String, entityIds: List<String>): List<MessageWithLinks> =
		getMessages(sessionId, entityIds).map { withLinks(sessionId, it) }

	suspend fun modifyMessageWithLinks(sessionId: String, entity: DecryptedMessage): MessageWithLinks =
		withLinks(sessionId, modifyMessage(sessionId, entity))

	suspend fun modifyMessagesWithLinks(sessionId: String, entities: List<DecryptedMessage>): List<MessageWithLinks> =
		modifyMessages(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteMessageByIdWithLinks(sessionId: String, id: String, rev: String): MessageWithLinks =
		withLinks(sessionId, undeleteMessageById(sessionId, id, rev))

	suspend fun filterMessagesByWithLinks(sessionId: String, filter: BaseFilterOptions<Message>): List<MessageWithLinks> =
		filterMessagesBy(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun filterMessagesBySortedWithLinks(sessionId: String, filter: BaseSortableFilterOptions<Message>): List<MessageWithLinks> =
		filterMessagesBySorted(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun createMessageInTopicWithLinks(sessionId: String, entity: DecryptedMessage): MessageWithLinks =
		withLinks(sessionId, createMessageInTopic(sessionId, entity))
}
