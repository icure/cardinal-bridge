package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.CalendarItem
import kotlinx.serialization.Serializable

@Serializable
data class CalendarItemWithLinks(
    val calendarItem: CalendarItem,
    val patientIds: Set<String>
)
