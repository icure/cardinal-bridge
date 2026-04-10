package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.Document
import kotlinx.serialization.Serializable

@Serializable
data class DocumentWithLinks(
    val document: Document,
    val patientIds: Set<String>
)
