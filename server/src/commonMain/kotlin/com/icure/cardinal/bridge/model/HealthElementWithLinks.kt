package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.HealthElement
import kotlinx.serialization.Serializable

@Serializable
data class HealthElementWithLinks(
    val healthElement: HealthElement,
    val patientIds: Set<String>
)
