package com.icure.cardinal.bridge.components

import com.icure.cardinal.bridge.model.BasicCredentials
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.bridge.model.JwtCredentials
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.auth.AuthSecretDetails
import com.icure.cardinal.sdk.auth.AuthSecretProvider
import com.icure.cardinal.sdk.auth.AuthenticationProcessApi
import com.icure.cardinal.sdk.auth.UsernamePassword
import com.icure.cardinal.sdk.model.embed.AuthenticationClass
import com.icure.cardinal.sdk.crypto.CryptoStrategies
import com.icure.cardinal.sdk.crypto.KeyPairRecoverer
import com.icure.cardinal.sdk.crypto.impl.exportSpkiHex
import com.icure.cardinal.sdk.model.DataOwnerWithType
import com.icure.cardinal.sdk.model.extensions.publicKeysWithSha256Spki
import com.icure.cardinal.sdk.model.specializations.Base64String
import com.icure.cardinal.sdk.options.AuthenticationMethod
import com.icure.cardinal.sdk.options.SdkOptions
import com.icure.cardinal.sdk.storage.impl.VolatileStorageFacade
import com.icure.cardinal.sdk.utils.decode
import com.icure.kryptom.crypto.CryptoService
import com.icure.kryptom.crypto.RsaAlgorithm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile


/**
 * This class allows to instantiate or renew an instance of [CardinalSdk] that can be used at the controller level.
 *
 * @param applicationId the applicationId of the database.
 * @param baseUrl the Cardinal backend url to use (api or nightly).
 */
class CardinalSdkInitializer(
	private val applicationId: String?,
	private val baseUrl: String,
) {
	@Volatile
	private var cache = emptyMap<Credentials, Deferred<CardinalSdk>>()
	private val cacheMutex = Mutex()

	/**
	 * Get a cardinal sdk for the given [credentials].
	 * If there is already an SDK with those credentials it is returned, otherwise a new one is created.
	 * Failures are not cached.
	 */
	suspend fun getOrInit(credentials: Credentials, pkcs8Keys: Map<String, Set<Base64String>> = emptyMap()): CardinalSdk =
		coroutineScope {
			var res: CardinalSdk? = null
			while (res == null) {
				val existing = cache[credentials]
				if (existing != null) {
					try {
						res = existing.await()
						continue
					} catch (_: Exception) {
						// else ignore, move on
					}
				}
				ensureActive()
				val jobCreatedByMe = cacheMutex.withLock {
					if (existing === cache[credentials]) {
						val job = async(start = CoroutineStart.LAZY) { initialize(credentials, pkcs8Keys) }
						cache = cache + (credentials to job)
						job
					} else null
				}
				if (jobCreatedByMe != null) res = jobCreatedByMe.await()
				// Someone else created the job wait for them
			}
			res
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
