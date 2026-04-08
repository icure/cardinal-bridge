package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedMessage
import kotlinx.serialization.Serializable

@Serializable
data class MessageWithLinks(
    val message: DecryptedMessage,
    val patientIds: Set<String>,
    val ownSecretIds: Set<String>
)
