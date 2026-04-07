package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.auth.UsernamePassword
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.model.Patient
import com.icure.cardinal.sdk.model.User
import com.icure.cardinal.sdk.model.specializations.Base64String
import com.icure.cardinal.sdk.options.AuthenticationMethod
import com.icure.cardinal.sdk.options.SdkOptions
import com.icure.cardinal.sdk.storage.impl.VolatileStorageFacade
import io.ktor.util.collections.ConcurrentMap

class BridgeLogic {
	private val cardinalSdkInitializer = CardinalSdkInitializer(
		System.getProperty("CARDINAL_APPLICATION_ID"),
		System.getProperty("CARDINAL_BASE_URL") ?: "https://api.cardinal.com"
	)

	suspend fun getCurrentUser(): User = TODO()

	suspend fun patientMatchBy(credentials: Credentials, filter: BaseFilterOptions<Patient>): List<String> =
        cardinalSdkInitializer.initialize(credentials.username, credentials.password, emptyMap()).patient.matchPatientsBy(filter)
}
