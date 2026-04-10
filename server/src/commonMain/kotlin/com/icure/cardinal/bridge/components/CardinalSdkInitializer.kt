package com.icure.cardinal.bridge.components

import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.bridge.model.JwtCredentials
import com.icure.cardinal.bridge.model.SessionParams
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.api.raw.RawApiConfig
import com.icure.cardinal.sdk.api.raw.RawCalendarItemApi
import com.icure.cardinal.sdk.api.raw.RawContactApi
import com.icure.cardinal.sdk.api.raw.RawDocumentApi
import com.icure.cardinal.sdk.api.raw.RawFormApi
import com.icure.cardinal.sdk.api.raw.RawHealthElementApi
import com.icure.cardinal.sdk.api.raw.RawMessageApi
import com.icure.cardinal.sdk.api.raw.RawPatientApi
import com.icure.cardinal.sdk.api.raw.impl.RawCalendarItemApiImpl
import com.icure.cardinal.sdk.api.raw.impl.RawContactApiImpl
import com.icure.cardinal.sdk.api.raw.impl.RawDocumentApiImpl
import com.icure.cardinal.sdk.api.raw.impl.RawFormApiImpl
import com.icure.cardinal.sdk.api.raw.impl.RawHealthElementApiImpl
import com.icure.cardinal.sdk.api.raw.impl.RawMessageApiImpl
import com.icure.cardinal.sdk.api.raw.impl.RawPatientApiImpl
import com.icure.cardinal.sdk.auth.JwtBearer
import com.icure.cardinal.sdk.auth.JwtBearerAndRefresh
import com.icure.cardinal.sdk.auth.services.AuthProvider
import com.icure.cardinal.sdk.auth.services.JwtBasedAuthProvider
import com.icure.cardinal.sdk.auth.services.TokenBasedAuthService
import com.icure.cardinal.sdk.crypto.AccessControlKeysHeadersProvider
import com.icure.cardinal.sdk.crypto.CryptoStrategies
import com.icure.cardinal.sdk.crypto.KeyPairRecoverer
import com.icure.cardinal.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.cardinal.sdk.crypto.impl.exportSpkiHex
import com.icure.cardinal.sdk.model.DataOwnerWithType
import com.icure.cardinal.sdk.model.embed.AuthenticationClass
import com.icure.cardinal.sdk.model.extensions.publicKeysWithSha256Spki
import com.icure.cardinal.sdk.model.specializations.Base64String
import com.icure.cardinal.sdk.options.AuthenticationMethod
import com.icure.cardinal.sdk.options.RequestRetryConfiguration
import com.icure.cardinal.sdk.options.SdkOptions
import com.icure.cardinal.sdk.storage.impl.VolatileStorageFacade
import com.icure.cardinal.sdk.utils.RequestStatusException
import com.icure.cardinal.sdk.utils.Serialization
import com.icure.cardinal.sdk.utils.decode
import com.icure.kryptom.crypto.CryptoService
import com.icure.kryptom.crypto.RsaAlgorithm
import com.icure.utils.InternalIcureApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@InternalIcureApi
class RawApis(
	url: String,
	authProvider: AuthProvider,
	accessControlKeysHeadersProvider: AccessControlKeysHeadersProvider?,
	rawApiConfig: RawApiConfig
) {
	val calendarItem: RawCalendarItemApi by lazy {
		RawCalendarItemApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
	val contact: RawContactApi by lazy {
		RawContactApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
	val document: RawDocumentApi by lazy {
		RawDocumentApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
	val form: RawFormApi by lazy {
		RawFormApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
	val healthElement: RawHealthElementApi by lazy {
		RawHealthElementApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
	val message: RawMessageApi by lazy {
		RawMessageApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
	val patient: RawPatientApi by lazy {
		RawPatientApiImpl(url, authProvider, accessControlKeysHeadersProvider, rawApiConfig)
	}
}
/**
 * This class allows instantiating an instance of [CardinalSdk] which will be accessible from a given "sessionId".
 *
 * @param applicationId the applicationId of the database.
 * @param defaultBaseUrl the Cardinal backend url to use (api or nightly).
 */
@OptIn(InternalIcureApi::class)
class CardinalSdkInitializer(
	private val applicationId: String?,
	private val defaultBaseUrl: String,
) {
	@Volatile
	private var cache = emptyMap<String, Pair<CardinalSdk, RawApis>>()
	private val cacheMutex = Mutex()

	/**
	 * Get a cardinal sdk for the given [sessionId] if it exists.
	 */
	fun getSdk(sessionId: String): CardinalSdk =
		requireNotNull(cache[sessionId]) { "No SDK for session $sessionId" }.first

	@InternalIcureApi
	fun getRawApis(sessionId: String): RawApis =
		requireNotNull(cache[sessionId]) { "No SDK for session $sessionId" }.second

	/**
	 * Dispose of the cardinal sdk associated with the provided [sessionId]
	 */
	suspend fun destroySession(sessionId: String) {
		cacheMutex.withLock {
			requireNotNull(cache[sessionId]) {
				"No SDK for session $sessionId"
			}.also {
				cache = cache - sessionId
			}
		}.first.scope.cancel()
	}

	/**
	 * Creates a new Cardinal SDK with the provided [credentials] and [sessionParams], and associates it with the returned
	 * session id.
	 * SDKs are kept indefinitely, even if they became unusable due to expired [credentials], until [destroySession] is
	 * called.
	 */
	@OptIn(ExperimentalUuidApi::class)
	suspend fun createSession(credentials: Credentials, sessionParams: SessionParams): String {
		val authProvider = createAuthProviderForCredentials(credentials)
		val sdk = initialize(authProvider, sessionParams)
		val json = Serialization.lenientJson
		val rawApis = RawApis(
			sessionParams.baseUrlOrDefault(),
			authProvider,
			NoAccessControlKeysHeadersProvider,
			RawApiConfig(
				httpClient = HttpClient {
					install(ContentNegotiation) {
						json(json = json)
					}
					install(HttpTimeout) {
						requestTimeoutMillis = 5 * 60 * 1_000L
					}
				},
				additionalHeaders = emptyMap(),
				requestTimeout = 5.minutes,
				json = json,
				retryConfiguration = RequestRetryConfiguration()
			),

		)
		while (true) {
			val id = Uuid.random().toHexDashString()
			cacheMutex.withLock {
				if (id !in cache) {
					cache = cache + (id to Pair(sdk, rawApis))
					return id
				}
				// else try another id
			}
		}
	}

	private fun createAuthProviderForCredentials(credentials: Credentials): AuthProvider = when (credentials) {
		is JwtCredentials -> OneJwtAuthProvider(credentials.token)
	}

	private fun SessionParams.baseUrlOrDefault() = baseUrl ?: defaultBaseUrl

	private suspend fun initialize(authProvider: AuthProvider, sessionParams: SessionParams): CardinalSdk =
		CardinalSdk.initialize(
			projectId = applicationId,
			baseUrl = sessionParams.baseUrlOrDefault(),
			authenticationMethod = AuthenticationMethod.UsingAuthProvider(authProvider),
			baseStorage = VolatileStorageFacade(),
			options = SdkOptions(
				cryptoStrategies = object : CryptoStrategies {
					override suspend fun recoverAndVerifySelfHierarchyKeys(
						keysData: List<CryptoStrategies.KeyDataRecoveryRequest>,
						cryptoPrimitives: CryptoService,
						keyPairRecoverer: KeyPairRecoverer
					): Map<String, CryptoStrategies.RecoveredKeyData> {
						return keysData.associate { recoveryRequest ->
							val dataOwner = recoveryRequest.dataOwnerDetails.dataOwner
							val pubSpkiForSha256 = dataOwner.publicKeysWithSha256Spki
							val keysOfDataOwnerByPubSpki = sessionParams.pkcs8Keys[dataOwner.id]?.associate { pkcs8Base64 ->
								val pkcs8Bytes = pkcs8Base64.decode()
								val asRsaSha1 = cryptoPrimitives.rsa.loadKeyPairPkcs8(RsaAlgorithm.RsaEncryptionAlgorithm.OaepWithSha1, pkcs8Bytes)
								val spki = cryptoPrimitives.rsa.exportSpkiHex(asRsaSha1.public)
								if (spki in pubSpkiForSha256) {
									spki to cryptoPrimitives.rsa.loadKeyPairPkcs8(RsaAlgorithm.RsaEncryptionAlgorithm.OaepWithSha256, pkcs8Bytes)
								} else {
									spki to asRsaSha1
								}
							}.orEmpty()
							dataOwner.id to CryptoStrategies.RecoveredKeyData(
								recoveredKeys = keysOfDataOwnerByPubSpki.mapKeys {
									it.key.fingerprintV1()
								},
								keyAuthenticity = keysOfDataOwnerByPubSpki.keys.associate { it.fingerprintV1() to true }
							)
						}
					}

					override suspend fun generateNewKeyForDataOwner(
						self: DataOwnerWithType,
						cryptoPrimitives: CryptoService
					): CryptoStrategies.KeyGenerationRequestResult = CryptoStrategies.KeyGenerationRequestResult.ParentDelegator
				},
				createTransferKeys = false,
				useHierarchicalDataOwners = true,
				lenientJson = true
			)
		)
}

@InternalIcureApi
private class OneJwtAuthProvider(
	val bearerToken: String
) : JwtBasedAuthProvider {
	private val bearer = JwtBearer(bearerToken)

	private val authService = object : TokenBasedAuthService<JwtBearer> {
		override suspend fun getToken(): JwtBearer = bearer

		override suspend fun setAuthenticationInRequest(
			builder: HttpRequestBuilder,
			authenticationClass: AuthenticationClass?
		) {
			builder.bearerAuth(bearerToken)
		}

		override suspend fun invalidateCurrentAuthentication(
			error: RequestStatusException,
			requiredAuthClass: AuthenticationClass?
		) {
			throw UnsupportedOperationException("invalidateCurrentAuthentication can't work if there is only one bearer")
		}
	}

	override fun getAuthService(): TokenBasedAuthService<JwtBearer> =
		authService

	override suspend fun getBearerAndRefreshToken(): JwtBearerAndRefresh {
		throw UnsupportedOperationException("Only bearer is available")
	}

	override suspend fun changeScope(dataOwnerId: String): AuthProvider {
		throw UnsupportedOperationException("Can't change scope if only Bearer is available")
	}

	override suspend fun switchGroup(newGroupId: String): AuthProvider {
		val decodedTokenGroup = kotlin.runCatching {
			Json.parseToJsonElement(Base64String(bearerToken.split(".")[1]).decode().decodeToString()).jsonObject.getValue("g").jsonPrimitive.also { check (it.isString) }.content
		}.getOrNull() ?: throw IllegalArgumentException("Failed to parse cardinal JWT $bearerToken")
		if (decodedTokenGroup != newGroupId) {
			throw IllegalArgumentException("JWT group $decodedTokenGroup doesn't match the requested group $newGroupId")
		}
		return this
	}
}
