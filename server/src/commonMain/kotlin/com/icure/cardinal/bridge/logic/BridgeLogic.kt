package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.model.User

class BridgeLogic {

	val sdk: CardinalSdk
		get() = CardinalSdkInitializer.Companion().sdk


	suspend fun getCurrentUser(): User = sdk.user.getCurrentUser()

}