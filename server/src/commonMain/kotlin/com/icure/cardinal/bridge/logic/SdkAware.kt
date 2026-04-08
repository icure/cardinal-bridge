package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer

abstract class SdkAware(private val sdkInitializer: CardinalSdkInitializer) {
	protected fun sdk(sessionId: String) =
		sdkInitializer.getSdk(sessionId)
}