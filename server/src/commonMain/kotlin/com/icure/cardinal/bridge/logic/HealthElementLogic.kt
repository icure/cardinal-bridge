package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.model.DecryptedHealthElement
import com.icure.cardinal.sdk.model.HealthElement
import com.icure.cardinal.bridge.model.HealthElementWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.utils.InternalIcureApi
import kotlinx.serialization.json.JsonElement

class HealthElementLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

	suspend fun createHealthElement(sessionId: String, entity: DecryptedHealthElement): DecryptedHealthElement =
		sdk(sessionId).healthElement.createHealthElement(entity)

	suspend fun createHealthElements(sessionId: String, entities: List<DecryptedHealthElement>): List<DecryptedHealthElement> =
		sdk(sessionId).healthElement.createHealthElements(entities)

	suspend fun getHealthElement(sessionId: String, entityId: String): DecryptedHealthElement? =
		sdk(sessionId).healthElement.getHealthElement(entityId)

	suspend fun getHealthElements(sessionId: String, entityIds: List<String>): List<DecryptedHealthElement> =
		sdk(sessionId).healthElement.getHealthElements(entityIds)

	suspend fun modifyHealthElement(sessionId: String, entity: DecryptedHealthElement): DecryptedHealthElement =
		sdk(sessionId).healthElement.modifyHealthElement(entity)

	suspend fun modifyHealthElements(sessionId: String, entities: List<DecryptedHealthElement>): List<DecryptedHealthElement> =
		sdk(sessionId).healthElement.modifyHealthElements(entities)

	suspend fun deleteHealthElementById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).healthElement.deleteHealthElementById(entityId, rev)

	suspend fun deleteHealthElementsByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).healthElement.deleteHealthElementsByIds(entityIds)

	suspend fun undeleteHealthElementById(sessionId: String, id: String, rev: String): DecryptedHealthElement =
		sdk(sessionId).healthElement.undeleteHealthElementById(id, rev)

	suspend fun purgeHealthElementById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).healthElement.purgeHealthElementById(id, rev)

	// Filter/Match

	@OptIn(InternalIcureApi::class)
	suspend fun matchHealthElementsBy(sessionId: String, filter: JsonElement): List<String> =
		rawMatchBy(sessionId, filter, "helement")

	suspend fun filterHealthElementsBy(sessionId: String, filter: JsonElement): List<DecryptedHealthElement> =
		getFromMatches(matchHealthElementsBy(sessionId, filter)) { sdk(sessionId).healthElement.getHealthElements(it) }

	// WithLinks

	private suspend fun withLinks(sessionId: String, he: DecryptedHealthElement): HealthElementWithLinks {
		val patientIds = sdk(sessionId).healthElement.decryptPatientIdOf(he).map { it.entityId }.toSet()
		return HealthElementWithLinks(he, patientIds)
	}

	suspend fun createHealthElementWithLinks(sessionId: String, entity: DecryptedHealthElement): HealthElementWithLinks =
		withLinks(sessionId, createHealthElement(sessionId, entity))

	suspend fun createHealthElementsWithLinks(sessionId: String, entities: List<DecryptedHealthElement>): List<HealthElementWithLinks> =
		createHealthElements(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun getHealthElementWithLinks(sessionId: String, entityId: String): HealthElementWithLinks? =
		getHealthElement(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getHealthElementsWithLinks(sessionId: String, entityIds: List<String>): List<HealthElementWithLinks> =
		getHealthElements(sessionId, entityIds).map { withLinks(sessionId, it) }

	suspend fun modifyHealthElementWithLinks(sessionId: String, entity: DecryptedHealthElement): HealthElementWithLinks =
		withLinks(sessionId, modifyHealthElement(sessionId, entity))

	suspend fun modifyHealthElementsWithLinks(sessionId: String, entities: List<DecryptedHealthElement>): List<HealthElementWithLinks> =
		modifyHealthElements(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteHealthElementByIdWithLinks(sessionId: String, id: String, rev: String): HealthElementWithLinks =
		withLinks(sessionId, undeleteHealthElementById(sessionId, id, rev))

	suspend fun filterHealthElementsByWithLinks(sessionId: String, filter: JsonElement): List<HealthElementWithLinks> =
		filterHealthElementsBy(sessionId, filter).map { withLinks(sessionId, it) }
}
