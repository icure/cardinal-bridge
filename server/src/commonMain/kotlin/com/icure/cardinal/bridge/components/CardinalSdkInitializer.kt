package com.icure.cardinal.bridge.components

import com.icure.cardinal.bridge.model.CreatedToken
import com.icure.cardinal.sdk.CardinalBaseSdk
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.auth.UsernameLongToken
import com.icure.cardinal.sdk.crypto.CryptoStrategies
import com.icure.cardinal.sdk.crypto.KeyPairRecoverer
import com.icure.cardinal.sdk.crypto.entities.RecoveryDataKey
import com.icure.cardinal.sdk.crypto.impl.exportSpkiHex
import com.icure.cardinal.sdk.model.DataOwnerWithType
import com.icure.cardinal.sdk.model.Group
import com.icure.cardinal.sdk.model.User
import com.icure.cardinal.sdk.model.extensions.publicKeysWithSha256Spki
import com.icure.cardinal.sdk.model.specializations.Base64String
import com.icure.cardinal.sdk.options.AuthenticationMethod
import com.icure.cardinal.sdk.options.SdkOptions
import com.icure.cardinal.sdk.storage.impl.VolatileStorageFacade
import com.icure.cardinal.sdk.utils.decode
import com.icure.kryptom.crypto.CryptoService
import com.icure.kryptom.crypto.RsaAlgorithm
import com.icure.kryptom.crypto.asn.AsnToJwkConverter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime


/**
 * This class allows to instantiate or renew an instance of [CardinalSdk] that can be used at the controller level.
 * It is implemented as a Singleton as [koin](https://insert-koin.io/docs/quickstart/ktor/) does not work for the
 * Windows target.
 *
 * @param applicationId the applicationId of the database.
 * @param baseUrl the Cardinal backend url to use (api or nightly).
 */
class CardinalSdkInitializer private constructor(
	private val applicationId: String?,
	private val baseUrl: String,
) {
	// TODO should we cache SDKs?

	/**
	 * @param username the username of the user to log in.
	 * @param token a long-lived token for the user.
	 * @param pkcs8Keys the base64 encoded key of the user (optional) and parents (mandatory) in pkcs8, by data owner id.
	 */
	suspend fun initialize(username: String, token: String, pkcs8Keys: Map<String, Set<Base64String>>): CardinalSdk =
		CardinalSdk.initialize(
			projectId = applicationId,
			baseUrl = baseUrl,
			authenticationMethod = AuthenticationMethod.UsingCredentials(
				UsernameLongToken(username, token)
			),
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