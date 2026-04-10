package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.api.raw.RawApiConfig
import com.icure.cardinal.sdk.auth.services.AuthProvider
import com.icure.cardinal.sdk.auth.services.AuthService
import com.icure.cardinal.sdk.model.embed.AuthenticationClass
import com.icure.cardinal.sdk.utils.RequestStatusException
import com.icure.utils.InternalIcureApi
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration.Companion.milliseconds

abstract class SdkAware(private val sdkInitializer: CardinalSdkInitializer) {
	protected fun sdk(sessionId: String) =
		sdkInitializer.getSdk(sessionId)

	@OptIn(InternalIcureApi::class)
	protected suspend fun rawMatchBy(sessionId: String, filter: JsonElement, vararg scopeSegments: String): List<String> {
		val rawApis = sdkInitializer.getRawApis(sessionId)
		val req = post(rawApis.rawApiConfig, rawApis.authProvider) {
			url {
				takeFrom(rawApis.url)
				appendPathSegments("rest", "v2", *scopeSegments, "match")
			}
			accept(Application.Json)
			setBody(TextContent(filter.toString(), Application.Json))
		}
		if (req.status.isSuccess()) {
			return req.body()
		} else throw RequestStatusException(
			HttpMethod.Post,
			"${rawApis.url}/rest/v2/${scopeSegments.joinToString("/")}/match",
			req.status.value,
			req.bodyAsText()
		)
	}

	@InternalIcureApi
	private suspend fun post(
		config: RawApiConfig,
		authProvider: AuthProvider? = null,
		accessControlKeysGroupId: String? = null,
		block: suspend HttpRequestBuilder.() -> Unit
	) = requestAndRetryOnServerOrConnectionError(
		config,
		HttpMethod.Post,
		authProvider?.getAuthService(),
		null,
		accessControlKeysGroupId,
		block
	)

	@InternalIcureApi
	private tailrec suspend fun requestAndRetryOnServerOrConnectionError(
		config: RawApiConfig,
		method: HttpMethod,
		authService: AuthService?,
		authenticationClass: AuthenticationClass?,
		accessControlKeysGroupId: String?,
		block: suspend HttpRequestBuilder.() -> Unit,
		remainingRetries: Int = config.retryConfiguration.maxRetries,
		delayOnFailureMs: Long = config.retryConfiguration.initialDelay.inWholeMilliseconds
	): HttpResponse {
		val result = request(
			config,
			method,
			authService,
			authenticationClass,
		) { block() }
		return when {
			remainingRetries <= 0 -> result.getOrThrow()
			result.isFailure || result.getOrThrow().status.value in 500 .. 599 -> {
				delay(delayOnFailureMs.milliseconds)
				requestAndRetryOnServerOrConnectionError(
					config,
					method,
					authService,
					authenticationClass,
					accessControlKeysGroupId,
					block,
					remainingRetries - 1,
					(delayOnFailureMs * config.retryConfiguration.exponentialBackoffFactor).toLong().let { newDelay ->
						config.retryConfiguration.exponentialBackoffCeil?.inWholeMilliseconds?.let { max ->
							newDelay.coerceAtMost(max)
						} ?: newDelay
					},
				)
			}
			else -> result.getOrThrow()
		}
	}

	@InternalIcureApi
	private suspend inline fun request(
		config: RawApiConfig,
		method: HttpMethod,
		authService: AuthService?,
		authenticationClass: AuthenticationClass?,
		block: HttpRequestBuilder.() -> Unit
	): Result<HttpResponse> {
		// If the builder fails we want to fail (could happen when getting the authorization, access control keys, ...)
		val requestBuilder = HttpRequestBuilder().apply {
			this.method = method
			headers {
				config.additionalHeaders.forEach { (header, headerValue) ->
					set(header, headerValue)
				}
				set("Cardinal-Model-Standard", "true")
				set("Cardinal-SDK-Version", "2.3.1")
			}
			config.requestTimeout?.also {
				timeout {
					requestTimeoutMillis = it.inWholeMilliseconds
				}
			}
			authService?.setAuthenticationInRequest(this, authenticationClass)
			block()
		}
		// We want to retry only for exceptions from actually executing the request
		return kotlin.runCatching { config.httpClient.request(requestBuilder) }
	}

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