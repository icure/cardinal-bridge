package com.icure.cardinal.bridge.model

import com.icure.cardinal.sdk.model.Form
import kotlinx.serialization.Serializable

@Serializable
data class FormWithLinks(
    val form: Form,
    val patientIds: Set<String>
)
