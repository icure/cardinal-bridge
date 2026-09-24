package com.icure.cardinal.bridge.bench.support

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import java.security.KeyPairGenerator
import java.util.Base64

/**
 * CouchDB + Kraken, configured like the khermes e2e tests.
 *
 * Kraken signs its JWTs with `JWT_AUTH_PRIV_KEY`, `JWT_AUTH_PUB_KEY`, `JWT_REFRESH_PRIV_KEY` and `JWT_REFRESH_PUB_KEY`,
 * taken from the environment when set, otherwise generated for the run. `ICURE_GCP_SERVICE_ACCOUNT_KEY` is also passed
 * from the environment. The image can be overridden with `BRIDGE_BENCH_KRAKEN_IMAGE`.
 */
object Containers {
	const val COUCHDB_USERNAME = "icure"
	const val COUCHDB_PASSWORD = "icure"

	private const val DEFAULT_KRAKEN_IMAGE = "docker.taktik.be/icure/icure-kraken:26.8.7-hotfix.2608.g8bbf607a9e"

	internal val network: Network = Network.newNetwork()

	/** Last lines logged by Kraken, printed if it fails to start. */
	private val krakenLogs = ArrayDeque<String>()

	private val jwtKeys: Map<String, String> = listOf("AUTH", "REFRESH").flatMap { usage ->
		val envPrivate = System.getenv("JWT_${usage}_PRIV_KEY")?.takeIf { it.isNotBlank() }
		val envPublic = System.getenv("JWT_${usage}_PUB_KEY")?.takeIf { it.isNotBlank() }
		val (private, public) = if (envPrivate != null && envPublic != null) {
			envPrivate to envPublic
		} else {
			// Base64 of the DER encoded keys: pkcs8 for the private key, x509 for the public key.
			val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
			Base64.getEncoder().encodeToString(keyPair.private.encoded) to Base64.getEncoder().encodeToString(keyPair.public.encoded)
		}
		listOf("JWT_${usage}_PRIV_KEY" to private, "JWT_${usage}_PUB_KEY" to public)
	}.toMap()

	private val couchdb: GenericContainer<*> = GenericContainer("couchdb:3.4.2")
		.withNetwork(network)
		.withNetworkAliases("couchdb-test")
		.withExposedPorts(5984)
		.withEnv("COUCHDB_USER", COUCHDB_USERNAME)
		.withEnv("COUCHDB_PASSWORD", COUCHDB_PASSWORD)
		.waitingFor(Wait.forHttp("/_up").forStatusCode(200))

	private val kraken: GenericContainer<*> = GenericContainer(
		System.getenv("BRIDGE_BENCH_KRAKEN_IMAGE")?.takeIf { it.isNotBlank() } ?: DEFAULT_KRAKEN_IMAGE
	)
		.withNetwork(network)
		.withNetworkAliases("kraken")
		.withEnv("ICURE_COUCHDB_URL", "http://couchdb-test:5984")
		.withEnv("ICURE_COUCHDB_PREFIX", "icure-__")
		.withEnv("ICURE_GROUPS_ADMINGROUP", "xx")
		.withEnv("ICURE_COUCHDB_USERNAME", COUCHDB_USERNAME)
		.withEnv("ICURE_COUCHDB_PASSWORD", COUCHDB_PASSWORD)
		.withEnv("ICURE_GCP_SERVICE_ACCOUNT_EMAIL", "fake-account@taktik-icure.iam.gserviceaccount.com")
		.withEnv("ICURE_EXTERNALSERVICES_USEFAKES", "true")
		.withEnv("ICURE_SYNC_GLOBAL_DATABASES", "true")
		.withEnv("ICURE_GCP_SERVICE_ACCOUNT_KEY", System.getenv("ICURE_GCP_SERVICE_ACCOUNT_KEY") ?: "")
		.withEnv(jwtKeys)
		.withExposedPorts(16043)
		.withLogConsumer { frame ->
			val line = frame.utf8StringWithoutLineEnding
			if (" TRACE " !in line && " DEBUG " !in line) synchronized(krakenLogs) {
				krakenLogs.addLast(line)
				if (krakenLogs.size > 200) krakenLogs.removeFirst()
			}
		}
		.dependsOn(couchdb)
		.waitingFor(
			Wait.forHttp("/actuator/health").forPort(16043).forStatusCode(200)
				.withStartupTimeout(java.time.Duration.ofMinutes(5))
		)

	fun start() {
		couchdb.start()
		try {
			kraken.start()
		} catch (e: Exception) {
			println("Kraken failed to start, last log lines:\n" + synchronized(krakenLogs) { krakenLogs.joinToString("\n") })
			throw e
		}
	}

	fun stop() {
		kraken.stop()
		couchdb.stop()
	}

	val couchDbUrl: String get() = "http://${couchdb.host}:${couchdb.getMappedPort(5984)}"
	val krakenUrl: String get() = "http://${kraken.host}:${kraken.getMappedPort(16043)}"

	/** Url of Kraken for other containers of [network]. */
	const val krakenNetworkUrl = "http://kraken:16043"
}
