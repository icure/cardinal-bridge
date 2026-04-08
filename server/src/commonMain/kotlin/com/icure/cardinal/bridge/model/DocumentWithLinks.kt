package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedDocument
import kotlinx.serialization.Serializable

@Serializable
data class DocumentWithLinks(
    val document: DecryptedDocument,
    val patientIds: Set<String>
)
