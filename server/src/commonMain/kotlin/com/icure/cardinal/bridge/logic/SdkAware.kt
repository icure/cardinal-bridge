package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.utils.InternalIcureApi

abstract class SdkAware(private val sdkInitializer: CardinalSdkInitializer) {
	protected fun sdk(sessionId: String) =
		sdkInitializer.getSdk(sessionId)

	@InternalIcureApi
	protected fun raw(sessionId: String) =
		sdkInitializer.getRawApis(sessionId)

	protected inline fun <T> getFromMatches(matchesResult: List<String>, doGet: (List<String>) -> List<T>): List<T> {
		val res = ArrayList<T>(matchesResult.size)
		matchesResult.chunked(FILTER_BATCH_SIZE).forEach { chunk ->
			res.addAll(doGet(chunk))
		}
		return res
	}

	companion object {
		/**
		 * After match filter gets entities in batches of this number elements
		 */
		protected const val FILTER_BATCH_SIZE = 1000
	}
}