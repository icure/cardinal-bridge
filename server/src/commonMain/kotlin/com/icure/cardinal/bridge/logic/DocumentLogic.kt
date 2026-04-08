package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedDocument
import com.icure.cardinal.sdk.model.Document
import com.icure.cardinal.sdk.model.EncryptedDocument
import com.icure.cardinal.bridge.model.DocumentWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class DocumentLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createDocument(credentials: Credentials, entity: DecryptedDocument): DecryptedDocument =
		sdk(credentials).document.createDocument(entity)

	suspend fun createDocuments(credentials: Credentials, entities: List<DecryptedDocument>): List<DecryptedDocument> =
		sdk(credentials).document.createDocuments(entities)

	suspend fun getDocument(credentials: Credentials, entityId: String): DecryptedDocument? =
		sdk(credentials).document.getDocument(entityId)

	suspend fun getDocuments(credentials: Credentials, entityIds: List<String>): List<DecryptedDocument> =
		sdk(credentials).document.getDocuments(entityIds)

	suspend fun modifyDocument(credentials: Credentials, entity: DecryptedDocument): DecryptedDocument =
		sdk(credentials).document.modifyDocument(entity)

	suspend fun modifyDocuments(credentials: Credentials, entities: List<DecryptedDocument>): List<DecryptedDocument> =
		sdk(credentials).document.modifyDocuments(entities)

	suspend fun deleteDocumentById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).document.deleteDocumentById(entityId, rev)

	suspend fun deleteDocumentsByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).document.deleteDocumentsByIds(entityIds)

	suspend fun undeleteDocumentById(credentials: Credentials, id: String, rev: String): DecryptedDocument =
		sdk(credentials).document.undeleteDocumentById(id, rev)

	suspend fun purgeDocumentById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).document.purgeDocumentById(id, rev)

	// Filter/Match

	suspend fun matchDocumentsBy(credentials: Credentials, filter: BaseFilterOptions<Document>): List<String> =
		sdk(credentials).document.matchDocumentsBy(filter)

	suspend fun matchDocumentsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Document>): List<String> =
		sdk(credentials).document.matchDocumentsBySorted(filter)

	suspend fun filterDocumentsBy(credentials: Credentials, filter: BaseFilterOptions<Document>): List<DecryptedDocument> {
		val iterator = sdk(credentials).document.filterDocumentsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterDocumentsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Document>): List<DecryptedDocument> {
		val iterator = sdk(credentials).document.filterDocumentsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// Document-specific (raw attachment operations)

	suspend fun getRawMainAttachment(credentials: Credentials, documentId: String): ByteArray =
		sdk(credentials).document.getRawMainAttachment(documentId)

	suspend fun setRawMainAttachment(credentials: Credentials, documentId: String, rev: String, utis: List<String>?, attachment: ByteArray, encrypted: Boolean): EncryptedDocument =
		sdk(credentials).document.setRawMainAttachment(documentId, rev, utis, attachment, encrypted)

	suspend fun deleteMainAttachment(credentials: Credentials, entityId: String, rev: String): EncryptedDocument =
		sdk(credentials).document.deleteMainAttachment(entityId, rev)

	suspend fun getRawSecondaryAttachment(credentials: Credentials, documentId: String, key: String): ByteArray =
		sdk(credentials).document.getRawSecondaryAttachment(documentId, key)

	suspend fun setRawSecondaryAttachment(credentials: Credentials, documentId: String, key: String, rev: String, utis: List<String>?, attachment: ByteArray, encrypted: Boolean): EncryptedDocument =
		sdk(credentials).document.setRawSecondaryAttachment(documentId, key, rev, utis, attachment, encrypted)

	suspend fun deleteSecondaryAttachment(credentials: Credentials, documentId: String, key: String, rev: String): EncryptedDocument =
		sdk(credentials).document.deleteSecondaryAttachment(documentId, key, rev)

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, document: DecryptedDocument): DocumentWithLinks {
		val patientIds = sdk(credentials).document.decryptOwningEntityIdsOf(document).map { it.entityId }.toSet()
		return DocumentWithLinks(document, patientIds)
	}

	suspend fun createDocumentWithLinks(credentials: Credentials, entity: DecryptedDocument): DocumentWithLinks =
		withLinks(credentials, createDocument(credentials, entity))

	suspend fun createDocumentsWithLinks(credentials: Credentials, entities: List<DecryptedDocument>): List<DocumentWithLinks> =
		createDocuments(credentials, entities).map { withLinks(credentials, it) }

	suspend fun getDocumentWithLinks(credentials: Credentials, entityId: String): DocumentWithLinks? =
		getDocument(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getDocumentsWithLinks(credentials: Credentials, entityIds: List<String>): List<DocumentWithLinks> =
		getDocuments(credentials, entityIds).map { withLinks(credentials, it) }

	suspend fun modifyDocumentWithLinks(credentials: Credentials, entity: DecryptedDocument): DocumentWithLinks =
		withLinks(credentials, modifyDocument(credentials, entity))

	suspend fun modifyDocumentsWithLinks(credentials: Credentials, entities: List<DecryptedDocument>): List<DocumentWithLinks> =
		modifyDocuments(credentials, entities).map { withLinks(credentials, it) }

	suspend fun undeleteDocumentByIdWithLinks(credentials: Credentials, id: String, rev: String): DocumentWithLinks =
		withLinks(credentials, undeleteDocumentById(credentials, id, rev))

	suspend fun filterDocumentsByWithLinks(credentials: Credentials, filter: BaseFilterOptions<Document>): List<DocumentWithLinks> =
		filterDocumentsBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterDocumentsBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<Document>): List<DocumentWithLinks> =
		filterDocumentsBySorted(credentials, filter).map { withLinks(credentials, it) }
}
