package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.Message
import kotlinx.serialization.Serializable

@Serializable
data class MessageWithLinks(
    val message: Message,
    val patientIds: Set<String>,
    val ownSecretIds: Set<String>
)
