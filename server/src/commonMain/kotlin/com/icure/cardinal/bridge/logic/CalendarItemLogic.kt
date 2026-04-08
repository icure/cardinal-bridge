package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.CalendarItem
import com.icure.cardinal.sdk.model.DecryptedCalendarItem
import com.icure.cardinal.bridge.model.CalendarItemWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class CalendarItemLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createCalendarItem(credentials: Credentials, entity: DecryptedCalendarItem): DecryptedCalendarItem =
		sdk(credentials).calendarItem.createCalendarItem(entity)

	suspend fun createCalendarItems(credentials: Credentials, entities: List<DecryptedCalendarItem>): List<DecryptedCalendarItem> =
		sdk(credentials).calendarItem.createCalendarItems(entities)

	suspend fun getCalendarItem(credentials: Credentials, entityId: String): DecryptedCalendarItem? =
		sdk(credentials).calendarItem.getCalendarItem(entityId)

	suspend fun getCalendarItems(credentials: Credentials, entityIds: List<String>): List<DecryptedCalendarItem> =
		sdk(credentials).calendarItem.getCalendarItems(entityIds)

	suspend fun modifyCalendarItem(credentials: Credentials, entity: DecryptedCalendarItem): DecryptedCalendarItem =
		sdk(credentials).calendarItem.modifyCalendarItem(entity)

	suspend fun modifyCalendarItems(credentials: Credentials, entities: List<DecryptedCalendarItem>): List<DecryptedCalendarItem> =
		sdk(credentials).calendarItem.modifyCalendarItems(entities)

	suspend fun deleteCalendarItemById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).calendarItem.deleteCalendarItemById(entityId, rev)

	suspend fun deleteCalendarItemsByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).calendarItem.deleteCalendarItemsByIds(entityIds)

	suspend fun undeleteCalendarItemById(credentials: Credentials, id: String, rev: String): DecryptedCalendarItem =
		sdk(credentials).calendarItem.undeleteCalendarItemById(id, rev)

	suspend fun purgeCalendarItemById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).calendarItem.purgeCalendarItemById(id, rev)

	// Filter/Match

	suspend fun matchCalendarItemsBy(credentials: Credentials, filter: BaseFilterOptions<CalendarItem>): List<String> =
		sdk(credentials).calendarItem.matchCalendarItemsBy(filter)

	suspend fun matchCalendarItemsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<CalendarItem>): List<String> =
		sdk(credentials).calendarItem.matchCalendarItemsBySorted(filter)

	suspend fun filterCalendarItemsBy(credentials: Credentials, filter: BaseFilterOptions<CalendarItem>): List<DecryptedCalendarItem> {
		val iterator = sdk(credentials).calendarItem.filterCalendarItemsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterCalendarItemsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<CalendarItem>): List<DecryptedCalendarItem> {
		val iterator = sdk(credentials).calendarItem.filterCalendarItemsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// CalendarItem-specific

	suspend fun bookCalendarItemCheckingAvailability(credentials: Credentials, entity: DecryptedCalendarItem): DecryptedCalendarItem =
		sdk(credentials).calendarItem.bookCalendarItemCheckingAvailability(entity)

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, calendarItem: DecryptedCalendarItem): CalendarItemWithLinks {
		val patientIds = sdk(credentials).calendarItem.decryptPatientIdOf(calendarItem).map { it.entityId }.toSet()
		return CalendarItemWithLinks(calendarItem, patientIds)
	}

	suspend fun createCalendarItemWithLinks(credentials: Credentials, entity: DecryptedCalendarItem): CalendarItemWithLinks =
		withLinks(credentials, createCalendarItem(credentials, entity))

	suspend fun createCalendarItemsWithLinks(credentials: Credentials, entities: List<DecryptedCalendarItem>): List<CalendarItemWithLinks> =
		createCalendarItems(credentials, entities).map { withLinks(credentials, it) }

	suspend fun getCalendarItemWithLinks(credentials: Credentials, entityId: String): CalendarItemWithLinks? =
		getCalendarItem(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getCalendarItemsWithLinks(credentials: Credentials, entityIds: List<String>): List<CalendarItemWithLinks> =
		getCalendarItems(credentials, entityIds).map { withLinks(credentials, it) }

	suspend fun modifyCalendarItemWithLinks(credentials: Credentials, entity: DecryptedCalendarItem): CalendarItemWithLinks =
		withLinks(credentials, modifyCalendarItem(credentials, entity))

	suspend fun modifyCalendarItemsWithLinks(credentials: Credentials, entities: List<DecryptedCalendarItem>): List<CalendarItemWithLinks> =
		modifyCalendarItems(credentials, entities).map { withLinks(credentials, it) }

	suspend fun undeleteCalendarItemByIdWithLinks(credentials: Credentials, id: String, rev: String): CalendarItemWithLinks =
		withLinks(credentials, undeleteCalendarItemById(credentials, id, rev))

	suspend fun filterCalendarItemsByWithLinks(credentials: Credentials, filter: BaseFilterOptions<CalendarItem>): List<CalendarItemWithLinks> =
		filterCalendarItemsBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterCalendarItemsBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<CalendarItem>): List<CalendarItemWithLinks> =
		filterCalendarItemsBySorted(credentials, filter).map { withLinks(credentials, it) }

	suspend fun bookCalendarItemCheckingAvailabilityWithLinks(credentials: Credentials, entity: DecryptedCalendarItem): CalendarItemWithLinks =
		withLinks(credentials, bookCalendarItemCheckingAvailability(credentials, entity))
}
