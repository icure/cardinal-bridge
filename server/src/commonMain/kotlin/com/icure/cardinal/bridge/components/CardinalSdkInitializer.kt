package com.icure.cardinal.bridge.components

import com.icure.cardinal.bridge.model.BasicCredentials
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.bridge.model.JwtCredentials
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.auth.AuthSecretDetails
import com.icure.cardinal.sdk.auth.AuthSecretProvider
import com.icure.cardinal.sdk.auth.AuthenticationProcessApi
import com.icure.cardinal.sdk.auth.UsernamePassword
import com.icure.cardinal.sdk.crypto.CryptoStrategies
import com.icure.cardinal.sdk.crypto.KeyPairRecoverer
import com.icure.cardinal.sdk.crypto.impl.exportSpkiHex
import com.icure.cardinal.sdk.model.DataOwnerWithType
import com.icure.cardinal.sdk.model.embed.AuthenticationClass
import com.icure.cardinal.sdk.model.extensions.publicKeysWithSha256Spki
import com.icure.cardinal.sdk.model.specializations.Base64String
import com.icure.cardinal.sdk.options.AuthenticationMethod
import com.icure.cardinal.sdk.options.SdkOptions
import com.icure.cardinal.sdk.storage.impl.VolatileStorageFacade
import com.icure.cardinal.sdk.utils.decode
import com.icure.kryptom.crypto.CryptoService
import com.icure.kryptom.crypto.RsaAlgorithm
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * This class allows instantiating an instance of [CardinalSdk] which will be accessible from a given "sessionId".
 *
 * @param applicationId the applicationId of the database.
 * @param baseUrl the Cardinal backend url to use (api or nightly).
 */
class CardinalSdkInitializer(
	private val applicationId: String?,
	private val baseUrl: String,
) {
	@Volatile
	private var cache = emptyMap<String, CardinalSdk>()
	private val cacheMutex = Mutex()

	/**
	 * Get a cardinal sdk for the given [sessionId] if it exists.
	 */
	fun getSdk(sessionId: String): CardinalSdk =
		requireNotNull(cache[sessionId]) { "No SDK for session $sessionId" }

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
		}.scope.cancel()
	}

	/**
	 * Creates a new Cardinal SDK with the provided [credentials] and [pkcs8Keys], and associates it with the returned
	 * session id.
	 * SDKs are kept indefinitely, even if they became unusable due to expired sessionId, until [destroySession] is
	 * called.
	 */
	@OptIn(ExperimentalUuidApi::class)
	suspend fun createSession(credentials: Credentials, pkcs8Keys: Map<String, Set<Base64String>>): String {
		val sdk = initialize(credentials, pkcs8Keys)
		while (true) {
			val id = Uuid.random().toHexDashString()
			cacheMutex.withLock {
				if (id !in cache) {
					cache = cache + (id to sdk)
					return id
				}
				// else try another id
			}
		}
	}


	private suspend fun initialize(credentials: Credentials, pkcs8Keys: Map<String, Set<Base64String>>): CardinalSdk =
		CardinalSdk.initialize(
			projectId = applicationId,
			baseUrl = baseUrl,
			authenticationMethod = when (credentials) {
				is BasicCredentials -> AuthenticationMethod.UsingCredentials(
					UsernamePassword(credentials.username, credentials.password)
				)
				is JwtCredentials -> AuthenticationMethod.UsingSecretProvider(
					loginUsername = null,
					existingJwt = credentials.token,
					secretProvider = object : AuthSecretProvider {
						override suspend fun getSecret(
							acceptedSecrets: Set<AuthenticationClass>,
							previousAttempts: List<AuthSecretDetails>,
							authProcessApi: AuthenticationProcessApi
						): AuthSecretDetails =
							throw IllegalStateException("JWT token expired, re-authentication required")
					}
				)
			},
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
							val keysOfDataOwnerByPubSpki = pkcs8Keys[dataOwner.id]?.associate { pkcs8Base64 ->
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
