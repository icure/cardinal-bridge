package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.CalendarItem
import com.icure.cardinal.sdk.model.DecryptedCalendarItem
import com.icure.cardinal.bridge.model.CalendarItemWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class CalendarItemLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

	suspend fun createCalendarItem(sessionId: String, entity: DecryptedCalendarItem): DecryptedCalendarItem =
		sdk(sessionId).calendarItem.createCalendarItem(entity)

	suspend fun createCalendarItems(sessionId: String, entities: List<DecryptedCalendarItem>): List<DecryptedCalendarItem> =
		sdk(sessionId).calendarItem.createCalendarItems(entities)

	suspend fun getCalendarItem(sessionId: String, entityId: String): DecryptedCalendarItem? =
		sdk(sessionId).calendarItem.getCalendarItem(entityId)

	suspend fun getCalendarItems(sessionId: String, entityIds: List<String>): List<DecryptedCalendarItem> =
		sdk(sessionId).calendarItem.getCalendarItems(entityIds)

	suspend fun modifyCalendarItem(sessionId: String, entity: DecryptedCalendarItem): DecryptedCalendarItem =
		sdk(sessionId).calendarItem.modifyCalendarItem(entity)

	suspend fun modifyCalendarItems(sessionId: String, entities: List<DecryptedCalendarItem>): List<DecryptedCalendarItem> =
		sdk(sessionId).calendarItem.modifyCalendarItems(entities)

	suspend fun deleteCalendarItemById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).calendarItem.deleteCalendarItemById(entityId, rev)

	suspend fun deleteCalendarItemsByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).calendarItem.deleteCalendarItemsByIds(entityIds)

	suspend fun undeleteCalendarItemById(sessionId: String, id: String, rev: String): DecryptedCalendarItem =
		sdk(sessionId).calendarItem.undeleteCalendarItemById(id, rev)

	suspend fun purgeCalendarItemById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).calendarItem.purgeCalendarItemById(id, rev)

	// Filter/Match

	suspend fun matchCalendarItemsBy(sessionId: String, filter: BaseFilterOptions<CalendarItem>): List<String> =
		sdk(sessionId).calendarItem.matchCalendarItemsBy(filter)

	suspend fun matchCalendarItemsBySorted(sessionId: String, filter: BaseSortableFilterOptions<CalendarItem>): List<String> =
		sdk(sessionId).calendarItem.matchCalendarItemsBySorted(filter)

	suspend fun filterCalendarItemsBy(sessionId: String, filter: BaseFilterOptions<CalendarItem>): List<DecryptedCalendarItem> {
		val iterator = sdk(sessionId).calendarItem.filterCalendarItemsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterCalendarItemsBySorted(sessionId: String, filter: BaseSortableFilterOptions<CalendarItem>): List<DecryptedCalendarItem> {
		val iterator = sdk(sessionId).calendarItem.filterCalendarItemsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// CalendarItem-specific

	suspend fun bookCalendarItemCheckingAvailability(sessionId: String, entity: DecryptedCalendarItem): DecryptedCalendarItem =
		sdk(sessionId).calendarItem.bookCalendarItemCheckingAvailability(entity)

	// WithLinks

	private suspend fun withLinks(sessionId: String, calendarItem: DecryptedCalendarItem): CalendarItemWithLinks {
		val patientIds = sdk(sessionId).calendarItem.decryptPatientIdOf(calendarItem).map { it.entityId }.toSet()
		return CalendarItemWithLinks(calendarItem, patientIds)
	}

	suspend fun createCalendarItemWithLinks(sessionId: String, entity: DecryptedCalendarItem): CalendarItemWithLinks =
		withLinks(sessionId, createCalendarItem(sessionId, entity))

	suspend fun createCalendarItemsWithLinks(sessionId: String, entities: List<DecryptedCalendarItem>): List<CalendarItemWithLinks> =
		createCalendarItems(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun getCalendarItemWithLinks(sessionId: String, entityId: String): CalendarItemWithLinks? =
		getCalendarItem(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getCalendarItemsWithLinks(sessionId: String, entityIds: List<String>): List<CalendarItemWithLinks> =
		getCalendarItems(sessionId, entityIds).map { withLinks(sessionId, it) }

	suspend fun modifyCalendarItemWithLinks(sessionId: String, entity: DecryptedCalendarItem): CalendarItemWithLinks =
		withLinks(sessionId, modifyCalendarItem(sessionId, entity))

	suspend fun modifyCalendarItemsWithLinks(sessionId: String, entities: List<DecryptedCalendarItem>): List<CalendarItemWithLinks> =
		modifyCalendarItems(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteCalendarItemByIdWithLinks(sessionId: String, id: String, rev: String): CalendarItemWithLinks =
		withLinks(sessionId, undeleteCalendarItemById(sessionId, id, rev))

	suspend fun filterCalendarItemsByWithLinks(sessionId: String, filter: BaseFilterOptions<CalendarItem>): List<CalendarItemWithLinks> =
		filterCalendarItemsBy(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun filterCalendarItemsBySortedWithLinks(sessionId: String, filter: BaseSortableFilterOptions<CalendarItem>): List<CalendarItemWithLinks> =
		filterCalendarItemsBySorted(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun bookCalendarItemCheckingAvailabilityWithLinks(sessionId: String, entity: DecryptedCalendarItem): CalendarItemWithLinks =
		withLinks(sessionId, bookCalendarItemCheckingAvailability(sessionId, entity))
}