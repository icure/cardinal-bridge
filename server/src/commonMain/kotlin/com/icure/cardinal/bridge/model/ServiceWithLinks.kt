package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.embed.DecryptedService
import kotlinx.serialization.Serializable

@Serializable
data class ServiceWithLinks(
    val service: DecryptedService,
    val patientIds: Set<String>
)
