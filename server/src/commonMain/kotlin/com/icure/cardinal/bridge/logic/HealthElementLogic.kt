package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedHealthElement
import com.icure.cardinal.sdk.model.HealthElement
import com.icure.cardinal.bridge.model.HealthElementWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class HealthElementLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createHealthElement(credentials: Credentials, entity: DecryptedHealthElement): DecryptedHealthElement =
		sdk(credentials).healthElement.createHealthElement(entity)

	suspend fun createHealthElements(credentials: Credentials, entities: List<DecryptedHealthElement>): List<DecryptedHealthElement> =
		sdk(credentials).healthElement.createHealthElements(entities)

	suspend fun getHealthElement(credentials: Credentials, entityId: String): DecryptedHealthElement? =
		sdk(credentials).healthElement.getHealthElement(entityId)

	suspend fun getHealthElements(credentials: Credentials, entityIds: List<String>): List<DecryptedHealthElement> =
		sdk(credentials).healthElement.getHealthElements(entityIds)

	suspend fun modifyHealthElement(credentials: Credentials, entity: DecryptedHealthElement): DecryptedHealthElement =
		sdk(credentials).healthElement.modifyHealthElement(entity)

	suspend fun modifyHealthElements(credentials: Credentials, entities: List<DecryptedHealthElement>): List<DecryptedHealthElement> =
		sdk(credentials).healthElement.modifyHealthElements(entities)

	suspend fun deleteHealthElementById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).healthElement.deleteHealthElementById(entityId, rev)

	suspend fun deleteHealthElementsByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).healthElement.deleteHealthElementsByIds(entityIds)

	suspend fun undeleteHealthElementById(credentials: Credentials, id: String, rev: String): DecryptedHealthElement =
		sdk(credentials).healthElement.undeleteHealthElementById(id, rev)

	suspend fun purgeHealthElementById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).healthElement.purgeHealthElementById(id, rev)

	// Filter/Match

	suspend fun matchHealthElementsBy(credentials: Credentials, filter: BaseFilterOptions<HealthElement>): List<String> =
		sdk(credentials).healthElement.matchHealthElementsBy(filter)

	suspend fun matchHealthElementsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<HealthElement>): List<String> =
		sdk(credentials).healthElement.matchHealthElementsBySorted(filter)

	suspend fun filterHealthElementsBy(credentials: Credentials, filter: BaseFilterOptions<HealthElement>): List<DecryptedHealthElement> {
		val iterator = sdk(credentials).healthElement.filterHealthElementsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterHealthElementsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<HealthElement>): List<DecryptedHealthElement> {
		val iterator = sdk(credentials).healthElement.filterHealthElementsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, he: DecryptedHealthElement): HealthElementWithLinks {
		val patientIds = sdk(credentials).healthElement.decryptPatientIdOf(he).map { it.entityId }.toSet()
		return HealthElementWithLinks(he, patientIds)
	}

	suspend fun createHealthElementWithLinks(credentials: Credentials, entity: DecryptedHealthElement): HealthElementWithLinks =
		withLinks(credentials, createHealthElement(credentials, entity))

	suspend fun createHealthElementsWithLinks(credentials: Credentials, entities: List<DecryptedHealthElement>): List<HealthElementWithLinks> =
		createHealthElements(credentials, entities).map { withLinks(credentials, it) }

	suspend fun getHealthElementWithLinks(credentials: Credentials, entityId: String): HealthElementWithLinks? =
		getHealthElement(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getHealthElementsWithLinks(credentials: Credentials, entityIds: List<String>): List<HealthElementWithLinks> =
		getHealthElements(credentials, entityIds).map { withLinks(credentials, it) }

	suspend fun modifyHealthElementWithLinks(credentials: Credentials, entity: DecryptedHealthElement): HealthElementWithLinks =
		withLinks(credentials, modifyHealthElement(credentials, entity))

	suspend fun modifyHealthElementsWithLinks(credentials: Credentials, entities: List<DecryptedHealthElement>): List<HealthElementWithLinks> =
		modifyHealthElements(credentials, entities).map { withLinks(credentials, it) }

	suspend fun undeleteHealthElementByIdWithLinks(credentials: Credentials, id: String, rev: String): HealthElementWithLinks =
		withLinks(credentials, undeleteHealthElementById(credentials, id, rev))

	suspend fun filterHealthElementsByWithLinks(credentials: Credentials, filter: BaseFilterOptions<HealthElement>): List<HealthElementWithLinks> =
		filterHealthElementsBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterHealthElementsBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<HealthElement>): List<HealthElementWithLinks> =
		filterHealthElementsBySorted(credentials, filter).map { withLinks(credentials, it) }
}
