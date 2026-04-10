package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.embed.Service
import kotlinx.serialization.Serializable

@Serializable
data class ServiceWithLinks(
    val service: Service,
    val patientIds: Set<String>
)
