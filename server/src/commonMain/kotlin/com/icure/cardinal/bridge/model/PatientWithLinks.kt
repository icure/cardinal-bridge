package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedPatient
import kotlinx.serialization.Serializable

@Serializable
data class PatientWithLinks(
    val patient: DecryptedPatient,
    val ownSecretIds: Set<String>
)
