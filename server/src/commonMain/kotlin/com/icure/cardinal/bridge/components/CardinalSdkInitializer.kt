package com.icure.cardinal.bridge.components

import com.icure.cardinal.bridge.model.CreatedToken
import com.icure.cardinal.sdk.CardinalBaseSdk
import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.auth.UsernameLongToken
import com.icure.cardinal.sdk.crypto.CryptoStrategies
import com.icure.cardinal.sdk.crypto.KeyPairRecoverer
import com.icure.cardinal.sdk.crypto.entities.RecoveryDataKey
import com.icure.cardinal.sdk.model.DataOwnerWithType
import com.icure.cardinal.sdk.model.Group
import com.icure.cardinal.sdk.model.User
import com.icure.cardinal.sdk.options.AuthenticationMethod
import com.icure.cardinal.sdk.options.SdkOptions
import com.icure.cardinal.sdk.storage.impl.VolatileStorageFacade
import com.icure.kryptom.crypto.CryptoService
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

	companion object {
		private var instance: CardinalSdkInitializer? = null

		operator fun invoke(applicationId: String?, baseUrl: String): CardinalSdkInitializer {
			if (instance == null) {
				instance = CardinalSdkInitializer(
					applicationId = applicationId,
					baseUrl = baseUrl,
				)
			}
			return instance!!
		}

		operator fun invoke(): CardinalSdkInitializer = checkNotNull(instance) { "SDK wrapper has not been initialized" }

		suspend fun updateBridgeToken(login: String, currentToken: String, newToken: String) =
			CardinalSdkInitializer().updateBridgeToken(login, currentToken, newToken)

		suspend fun getBridgeUser(): User = CardinalSdkInitializer().getBridgeUser()

		suspend fun getGroupId(): String = CardinalSdkInitializer().getBridgeGroup().id

	}

	lateinit var sdk: CardinalSdk
		private set

	private var bridgeUser: User? = null

	private var bridgeGroup: Group? = null

	suspend fun getBridgeUser(): User =
		if (bridgeUser == null) {
			sdk.user.getCurrentUser().also {
				bridgeUser = it
			}
		} else bridgeUser!!

	suspend fun getBridgeGroup(): Group =
		if (bridgeGroup == null) {
			val groupId = checkNotNull(Companion.getBridgeUser().groupId) { "Bridge user has no group id" }
			sdk.group.getGroup(groupId).also {
				bridgeGroup = it
			}
		} else bridgeGroup!!

	/**
	 * Initializes a new instance of [CardinalSdk]. If an instance already exists, it will be overwritten.
	 * The initialization needs a recovery key to recover the private key of the user, that will be stored in a volatile
	 * facade.
	 *
	 * @param username the username of the user to log in.
	 * @param token a long-lived token for the user.
	 * @param recoveryKey a base32 encoded recovery key.
	 */
	suspend fun initialize(username: String, token: String, recoveryKey: String) {
		sdk = CardinalSdk.initialize(
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
						val recoveryResult = recoveryKey.let {
							keyPairRecoverer.recoverWithRecoveryKey(
								RecoveryDataKey.fromBase32(it),
								autoDelete = false
							)
						}.also {
							if (!it.isSuccess) {
								throw IllegalArgumentException("Invalid recovery key")
							}
						}.value
						return keysData.associate { recoveryRequest ->
							val dataOwner = recoveryRequest.dataOwnerDetails.dataOwner
							dataOwner.id to CryptoStrategies.RecoveredKeyData(
								recoveredKeys = recoveryResult.let {
									recoveryRequest.unavailableKeys.associate { unavailableKeyInfo ->
										val pub = unavailableKeyInfo.publicKey
										recoveryResult[dataOwner.id]?.get(pub)?.let { recoveredKey ->
											pub.fingerprintV1() to recoveredKey
										} ?: throw IllegalStateException("Cannot recover private key for public key: ${unavailableKeyInfo.publicKey}")
									}
								},
								keyAuthenticity = emptyMap()
							)
						}
					}

					override suspend fun generateNewKeyForDataOwner(
						self: DataOwnerWithType,
						cryptoPrimitives: CryptoService
					): CryptoStrategies.KeyGenerationRequestResult = CryptoStrategies.KeyGenerationRequestResult.Deny

				},
				createTransferKeys = false,
			)
		)
	}

	@OptIn(ExperimentalTime::class)
	suspend fun updateBridgeToken(login: String, currentToken: String, newToken: String): CreatedToken {
		val sdk = CardinalBaseSdk.initialize(
			projectId = applicationId,
			baseUrl = baseUrl,
			authenticationMethod = AuthenticationMethod.UsingCredentials(
				UsernameLongToken(login, currentToken)
			)
		)
		val currentUser = sdk.user.getCurrentUser()
		val duration = 356.days
		sdk.user.getToken(
			userId = currentUser.id,
			key = "bridgeToken",
			tokenValidity = duration.inWholeSeconds,
			token = newToken,
		)
		return CreatedToken(
			token = newToken,
			expirationTs = Clock.System.now().toEpochMilliseconds() + duration.inWholeMilliseconds
		)
	}

	fun isSdkInitialized(): Boolean = ::sdk.isInitialized
}