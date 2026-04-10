package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.Contact
import kotlinx.serialization.Serializable

@Serializable
data class ContactWithLinks(
    val contact: Contact,
    val patientIds: Set<String>
)
