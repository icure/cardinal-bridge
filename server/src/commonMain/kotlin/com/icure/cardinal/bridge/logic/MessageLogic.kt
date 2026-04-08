package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedMessage
import com.icure.cardinal.sdk.model.Message
import com.icure.cardinal.bridge.model.MessageWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class MessageLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createMessage(credentials: Credentials, entity: DecryptedMessage): DecryptedMessage =
		sdk(credentials).message.createMessage(entity)

	suspend fun createMessages(credentials: Credentials, entities: List<DecryptedMessage>): List<DecryptedMessage> =
		sdk(credentials).message.createMessages(entities)

	suspend fun getMessage(credentials: Credentials, entityId: String): DecryptedMessage? =
		sdk(credentials).message.getMessage(entityId)

	suspend fun getMessages(credentials: Credentials, entityIds: List<String>): List<DecryptedMessage> =
		sdk(credentials).message.getMessages(entityIds)

	suspend fun modifyMessage(credentials: Credentials, entity: DecryptedMessage): DecryptedMessage =
		sdk(credentials).message.modifyMessage(entity)

	suspend fun modifyMessages(credentials: Credentials, entities: List<DecryptedMessage>): List<DecryptedMessage> =
		sdk(credentials).message.modifyMessages(entities)

	suspend fun deleteMessageById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).message.deleteMessageById(entityId, rev)

	suspend fun deleteMessagesByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).message.deleteMessagesByIds(entityIds)

	suspend fun undeleteMessageById(credentials: Credentials, id: String, rev: String): DecryptedMessage =
		sdk(credentials).message.undeleteMessageById(id, rev)

	suspend fun purgeMessageById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).message.purgeMessageById(id, rev)

	// Filter/Match

	suspend fun matchMessagesBy(credentials: Credentials, filter: BaseFilterOptions<Message>): List<String> =
		sdk(credentials).message.matchMessagesBy(filter)

	suspend fun matchMessagesBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Message>): List<String> =
		sdk(credentials).message.matchMessagesBySorted(filter)

	suspend fun filterMessagesBy(credentials: Credentials, filter: BaseFilterOptions<Message>): List<DecryptedMessage> {
		val iterator = sdk(credentials).message.filterMessagesBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterMessagesBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Message>): List<DecryptedMessage> {
		val iterator = sdk(credentials).message.filterMessagesBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// Message-specific

	suspend fun createMessageInTopic(credentials: Credentials, entity: DecryptedMessage): DecryptedMessage =
		sdk(credentials).message.createMessageInTopic(entity)

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, message: DecryptedMessage): MessageWithLinks {
		val sdk = sdk(credentials)
		val patientIds = sdk.message.decryptPatientIdOf(message).map { it.entityId }.toSet()
		val ownSecretIds = sdk.message.getSecretIdsOf(message).values.flatMap { refs -> refs.map { it.entityId } }.toSet()
		return MessageWithLinks(message, patientIds, ownSecretIds)
	}

	suspend fun createMessageWithLinks(credentials: Credentials, entity: DecryptedMessage): MessageWithLinks =
		withLinks(credentials, createMessage(credentials, entity))

	suspend fun createMessagesWithLinks(credentials: Credentials, entities: List<DecryptedMessage>): List<MessageWithLinks> =
		createMessages(credentials, entities).map { withLinks(credentials, it) }

	suspend fun getMessageWithLinks(credentials: Credentials, entityId: String): MessageWithLinks? =
		getMessage(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getMessagesWithLinks(credentials: Credentials, entityIds: List<String>): List<MessageWithLinks> =
		getMessages(credentials, entityIds).map { withLinks(credentials, it) }

	suspend fun modifyMessageWithLinks(credentials: Credentials, entity: DecryptedMessage): MessageWithLinks =
		withLinks(credentials, modifyMessage(credentials, entity))

	suspend fun modifyMessagesWithLinks(credentials: Credentials, entities: List<DecryptedMessage>): List<MessageWithLinks> =
		modifyMessages(credentials, entities).map { withLinks(credentials, it) }

	suspend fun undeleteMessageByIdWithLinks(credentials: Credentials, id: String, rev: String): MessageWithLinks =
		withLinks(credentials, undeleteMessageById(credentials, id, rev))

	suspend fun filterMessagesByWithLinks(credentials: Credentials, filter: BaseFilterOptions<Message>): List<MessageWithLinks> =
		filterMessagesBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterMessagesBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<Message>): List<MessageWithLinks> =
		filterMessagesBySorted(credentials, filter).map { withLinks(credentials, it) }

	suspend fun createMessageInTopicWithLinks(credentials: Credentials, entity: DecryptedMessage): MessageWithLinks =
		withLinks(credentials, createMessageInTopic(credentials, entity))
}
