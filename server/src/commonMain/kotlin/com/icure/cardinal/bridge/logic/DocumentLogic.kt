package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.model.DecryptedDocument
import com.icure.cardinal.sdk.model.Document
import com.icure.cardinal.sdk.model.EncryptedDocument
import com.icure.cardinal.bridge.model.DocumentWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.utils.InternalIcureApi
import kotlinx.serialization.json.JsonElement

class DocumentLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

	suspend fun createDocument(sessionId: String, entity: DecryptedDocument): DecryptedDocument =
		sdk(sessionId).document.createDocument(entity)

	suspend fun createDocuments(sessionId: String, entities: List<DecryptedDocument>): List<DecryptedDocument> =
		sdk(sessionId).document.createDocuments(entities)

	suspend fun getDocument(sessionId: String, entityId: String): DecryptedDocument? =
		sdk(sessionId).document.getDocument(entityId)

	suspend fun getDocuments(sessionId: String, entityIds: List<String>): List<DecryptedDocument> =
		sdk(sessionId).document.getDocuments(entityIds)

	suspend fun modifyDocument(sessionId: String, entity: DecryptedDocument): DecryptedDocument =
		sdk(sessionId).document.modifyDocument(entity)

	suspend fun modifyDocuments(sessionId: String, entities: List<DecryptedDocument>): List<DecryptedDocument> =
		sdk(sessionId).document.modifyDocuments(entities)

	suspend fun deleteDocumentById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).document.deleteDocumentById(entityId, rev)

	suspend fun deleteDocumentsByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).document.deleteDocumentsByIds(entityIds)

	suspend fun undeleteDocumentById(sessionId: String, id: String, rev: String): DecryptedDocument =
		sdk(sessionId).document.undeleteDocumentById(id, rev)

	suspend fun purgeDocumentById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).document.purgeDocumentById(id, rev)

	// Filter/Match

	@OptIn(InternalIcureApi::class)
	suspend fun matchDocumentsBy(sessionId: String, filter: JsonElement): List<String> =
		rawMatchBy(sessionId, filter, "document")

	suspend fun filterDocumentsBy(sessionId: String, filter: JsonElement): List<DecryptedDocument> =
		getFromMatches(matchDocumentsBy(sessionId, filter)) { sdk(sessionId).document.getDocuments(it) }

	// Document-specific (raw attachment operations)

	suspend fun getRawMainAttachment(sessionId: String, documentId: String): ByteArray =
		sdk(sessionId).document.getRawMainAttachment(documentId)

	suspend fun setRawMainAttachment(sessionId: String, documentId: String, rev: String, utis: List<String>?, attachment: ByteArray, encrypted: Boolean): EncryptedDocument =
		sdk(sessionId).document.setRawMainAttachment(documentId, rev, utis, attachment, encrypted)

	suspend fun deleteMainAttachment(sessionId: String, entityId: String, rev: String): EncryptedDocument =
		sdk(sessionId).document.deleteMainAttachment(entityId, rev)

	suspend fun getRawSecondaryAttachment(sessionId: String, documentId: String, key: String): ByteArray =
		sdk(sessionId).document.getRawSecondaryAttachment(documentId, key)

	suspend fun setRawSecondaryAttachment(sessionId: String, documentId: String, key: String, rev: String, utis: List<String>?, attachment: ByteArray, encrypted: Boolean): EncryptedDocument =
		sdk(sessionId).document.setRawSecondaryAttachment(documentId, key, rev, utis, attachment, encrypted)

	suspend fun deleteSecondaryAttachment(sessionId: String, documentId: String, key: String, rev: String): EncryptedDocument =
		sdk(sessionId).document.deleteSecondaryAttachment(documentId, key, rev)

	// WithLinks

	private suspend fun withLinks(sessionId: String, document: DecryptedDocument): DocumentWithLinks {
		val patientIds = sdk(sessionId).document.decryptOwningEntityIdsOf(document).map { it.entityId }.toSet()
		return DocumentWithLinks(document, patientIds)
	}

	suspend fun createDocumentWithLinks(sessionId: String, entity: DecryptedDocument): DocumentWithLinks =
		withLinks(sessionId, createDocument(sessionId, entity))

	suspend fun createDocumentsWithLinks(sessionId: String, entities: List<DecryptedDocument>): List<DocumentWithLinks> =
		createDocuments(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun getDocumentWithLinks(sessionId: String, entityId: String): DocumentWithLinks? =
		getDocument(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getDocumentsWithLinks(sessionId: String, entityIds: List<String>): List<DocumentWithLinks> =
		getDocuments(sessionId, entityIds).map { withLinks(sessionId, it) }

	suspend fun modifyDocumentWithLinks(sessionId: String, entity: DecryptedDocument): DocumentWithLinks =
		withLinks(sessionId, modifyDocument(sessionId, entity))

	suspend fun modifyDocumentsWithLinks(sessionId: String, entities: List<DecryptedDocument>): List<DocumentWithLinks> =
		modifyDocuments(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteDocumentByIdWithLinks(sessionId: String, id: String, rev: String): DocumentWithLinks =
		withLinks(sessionId, undeleteDocumentById(sessionId, id, rev))

	suspend fun filterDocumentsByWithLinks(sessionId: String, filter: JsonElement): List<DocumentWithLinks> =
		filterDocumentsBy(sessionId, filter).map { withLinks(sessionId, it) }
}
