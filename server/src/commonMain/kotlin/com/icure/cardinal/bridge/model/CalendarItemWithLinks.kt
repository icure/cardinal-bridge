package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedCalendarItem
import kotlinx.serialization.Serializable

@Serializable
data class CalendarItemWithLinks(
    val calendarItem: DecryptedCalendarItem,
    val patientIds: Set<String>
)
