package com.icure.cardinal.bridge.bench.support

import com.icure.cardinal.bridge.bridgeModule
import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import io.kotest.assertions.fail
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/**
 * Memory used by a bridge during a call. The gc values are only known for bridges running in the test jvm.
 */
data class MemoryMeasure(val peakBytes: Long, val gcCount: Long?, val gcMillis: Long?)

/**
 * A bridge called by the benchmark.
 */
interface BridgeUnderTest : AutoCloseable {
	/** Name of the bridge in the report. */
	val name: String

	/** Base url of the bridge routes, as seen from the test. */
	val url: String

	/** Url of Kraken as seen from the bridge, used as base url of the sessions. */
	val krakenUrl: String

	/** Starts measuring the memory used by the bridge for a call. */
	fun startMemoryMeasure()

	/** Memory used by the bridge since [startMemoryMeasure]. */
	fun memoryMeasure(): MemoryMeasure

	/** State of the bridge, printed when a call times out. */
	fun diagnostics(): String
}

/**
 * The bridge application running in the test jvm, served by the same engine as in production on a random port.
 */
class InProcessBridge private constructor(
	override val name: String,
	private val server: EmbeddedServer<*, *>,
	override val url: String,
) : BridgeUnderTest {
	companion object {
		private val heapPools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
		private val collectors = ManagementFactory.getGarbageCollectorMXBeans()

		suspend fun start(name: String, sdkInitializer: CardinalSdkInitializer): InProcessBridge {
			val server = embeddedServer(CIO, port = 0) { bridgeModule(sdkInitializer) }.start(wait = false)
			val port = server.engine.resolvedConnectors().first().port
			return InProcessBridge(name, server, "http://localhost:$port/bridge")
		}
	}

	override val krakenUrl: String get() = Containers.krakenUrl

	private var gcCountBefore = 0L
	private var gcMillisBefore = 0L

	/**
	 * Starts from a freshly collected heap.
	 */
	override fun startMemoryMeasure() {
		System.gc()
		heapPools.forEach { it.resetPeakUsage() }
		gcCountBefore = collectors.sumOf { it.collectionCount.coerceAtLeast(0) }
		gcMillisBefore = collectors.sumOf { it.collectionTime.coerceAtLeast(0) }
	}

	/**
	 * The peak is the sum of the peak usage of each heap memory pool, an upper bound of the actual peak. It includes the
	 * work of the test http client, which is negligible compared to the bridge.
	 */
	override fun memoryMeasure() = MemoryMeasure(
		peakBytes = heapPools.sumOf { it.peakUsage.used },
		gcCount = collectors.sumOf { it.collectionCount.coerceAtLeast(0) } - gcCountBefore,
		gcMillis = collectors.sumOf { it.collectionTime.coerceAtLeast(0) } - gcMillisBefore,
	)

	override fun diagnostics(): String = Diagnostics.dumpCoroutinesAndThreads()

	override fun close() {
		server.stop(1_000, 5_000)
	}
}

/**
 * The bridge docker image, running on the network of [Containers]. It uses the decoding strategy the image was built
 * with.
 */
class NativeBridge private constructor(
	private val container: GenericContainer<*>,
	private val logs: ArrayDeque<String>,
) : BridgeUnderTest {
	companion object {
		fun start(image: String): NativeBridge {
			val logs = ArrayDeque<String>()
			val container: GenericContainer<*> = GenericContainer(image)
				.withNetwork(Containers.network)
				.withExposedPorts(8080)
				.withLogConsumer { frame ->
					synchronized(logs) {
						logs.addLast(frame.utf8StringWithoutLineEnding)
						if (logs.size > 200) logs.removeFirst()
					}
				}
				.waitingFor(Wait.forHttp("/bridge/health").forPort(8080).forStatusCode(204))
			container.start()
			return NativeBridge(container, logs)
		}
	}

	override val name: String get() = "native"
	override val url: String get() = "http://${container.host}:${container.getMappedPort(8080)}/bridge"
	override val krakenUrl: String get() = Containers.krakenNetworkUrl

	/**
	 * The cgroup memory peak can't be reset on every kernel: the peak is the maximum since the bridge started.
	 */
	override fun startMemoryMeasure() {}

	override fun memoryMeasure() = MemoryMeasure(
		peakBytes = exec("cat /sys/fs/cgroup/memory.peak").trim().toLongOrNull() ?: -1,
		gcCount = null,
		gcMillis = null,
	)

	/**
	 * Tells apart a frozen process (health does not answer, no cpu used), a blocked http client (health answers, no cpu
	 * used) and a busy bridge (cpu used). The wait channel of each thread shows where it is blocked in the kernel.
	 */
	override fun diagnostics(): String = buildString {
		appendLine("=== Native bridge ===")
		appendLine("GET /bridge/health: ${healthProbe()}")
		val cpuBefore = cpuUsageMicros()
		Thread.sleep(2_000)
		val cpuAfter = cpuUsageMicros()
		appendLine(
			"cpu over 2s: " + if (cpuBefore != null && cpuAfter != null) "${(cpuAfter - cpuBefore) / 20_000.0}%" else "unknown"
		)
		appendLine("memory.current: ${exec("cat /sys/fs/cgroup/memory.current").trim()} bytes")
		appendLine("threads (name, state, wait channel):")
		append(exec("for t in /proc/1/task/*; do echo \"  \$(cat \$t/comm) \$(cut -d' ' -f3 \$t/stat) \$(cat \$t/wchan)\"; done"))
		appendLine("=== Bridge log tail ===")
		synchronized(logs) { logs.forEach { appendLine(it) } }
	}

	private fun healthProbe(): String = try {
		val response = HttpClient.newHttpClient().sendAsync(
			HttpRequest.newBuilder(URI("$url/health")).GET().build(),
			HttpResponse.BodyHandlers.discarding()
		).get(5, TimeUnit.SECONDS)
		"status ${response.statusCode()}"
	} catch (_: TimeoutException) {
		"no answer within 5s"
	}

	private fun cpuUsageMicros(): Long? =
		exec("cat /sys/fs/cgroup/cpu.stat").lineSequence()
			.firstOrNull { it.startsWith("usage_usec") }
			?.substringAfter(' ')?.trim()?.toLongOrNull()

	private fun exec(command: String): String =
		runCatching { container.execInContainer("sh", "-c", command).stdout }.getOrElse { "exec failed: $it" }

	override fun close() {
		container.stop()
	}
}

/**
 * The bridge answered with an error status.
 */
class BridgeCallException(message: String) : Exception(message)

/**
 * Calls [bridge] over http. Each call fails the test after [timeout], after printing the diagnostics of the bridge.
 * Error statuses are thrown as [BridgeCallException].
 */
class BridgeClient(private val bridge: BridgeUnderTest, private val timeout: Duration) {
	private val http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

	/**
	 * Creates a session for [hcp], logging in through [krakenUrl] as seen from the test.
	 */
	fun createSession(krakenUrl: String, hcp: BenchHcp): String {
		val token = krakenToken(krakenUrl, hcp)
		val params = buildJsonObject {
			put("baseUrl", bridge.krakenUrl)
			putJsonObject("pkcs8Keys") {
				putJsonArray(hcp.hcpId) { add(hcp.pkcs8.s) }
			}
		}
		val response = send(
			HttpRequest.newBuilder(URI("${bridge.url}/session"))
				.header("Authorization", "Bearer $token")
				.jsonPost(params)
		)
		return Json.parseToJsonElement(response.decodeToString()).jsonPrimitive.content
	}

	/**
	 * Posts [body] to `/bridge/[endpoint]` and returns the raw response body.
	 */
	fun post(sessionId: String, endpoint: String, body: JsonElement): ByteArray =
		send(
			HttpRequest.newBuilder(URI("${bridge.url}/$endpoint"))
				.header("Session", sessionId)
				.jsonPost(body)
		)

	private fun krakenToken(krakenUrl: String, hcp: BenchHcp): String {
		val credentials = buildJsonObject {
			put("username", hcp.login)
			put("password", hcp.password)
		}
		val response = send(HttpRequest.newBuilder(URI("$krakenUrl/rest/v2/auth/login")).jsonPost(credentials))
		return Json.parseToJsonElement(response.decodeToString()).jsonObject.getValue("token").jsonPrimitive.content
	}

	private fun HttpRequest.Builder.jsonPost(body: JsonElement): HttpRequest =
		header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
			.build()

	private fun send(request: HttpRequest): ByteArray {
		val future = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
		val response = try {
			future.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
		} catch (_: TimeoutException) {
			println(bridge.diagnostics())
			future.cancel(true)
			fail("${request.method()} ${request.uri()} did not complete within $timeout, see the ${bridge.name} bridge diagnostics above")
		}
		if (response.statusCode() != 200) {
			throw BridgeCallException("${response.statusCode()}: ${response.body().decodeToString().take(2_000)}")
		}
		return response.body()
	}
}
