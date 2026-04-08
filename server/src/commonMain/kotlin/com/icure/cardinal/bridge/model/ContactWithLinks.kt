package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedContact
import kotlinx.serialization.Serializable

@Serializable
data class ContactWithLinks(
    val contact: DecryptedContact,
    val patientIds: Set<String>
)
