package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.DecryptedForm
import kotlinx.serialization.Serializable

@Serializable
data class FormWithLinks(
    val form: DecryptedForm,
    val patientIds: Set<String>
)
