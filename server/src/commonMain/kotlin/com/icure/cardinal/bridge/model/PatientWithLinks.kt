package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.Patient
import kotlinx.serialization.Serializable

@Serializable
data class PatientWithLinks(
    val patient: Patient,
    val ownSecretIds: Set<String>
)
