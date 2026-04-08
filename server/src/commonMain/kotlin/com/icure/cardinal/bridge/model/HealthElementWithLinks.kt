package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedHealthElement
import kotlinx.serialization.Serializable

@Serializable
data class HealthElementWithLinks(
    val healthElement: DecryptedHealthElement,
    val patientIds: Set<String>
)
