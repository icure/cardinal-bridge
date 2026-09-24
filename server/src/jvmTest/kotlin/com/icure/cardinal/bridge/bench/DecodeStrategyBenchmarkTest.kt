package com.icure.cardinal.bridge.bench

import com.icure.cardinal.bridge.bench.support.BenchHcp
import com.icure.cardinal.bridge.bench.support.BenchMetrics
import com.icure.cardinal.bridge.bench.support.BenchReport
import com.icure.cardinal.bridge.bench.support.BridgeClient
import com.icure.cardinal.bridge.bench.support.BridgeUnderTest
import com.icure.cardinal.bridge.bench.support.CallResult
import com.icure.cardinal.bridge.bench.support.Containers
import com.icure.cardinal.bridge.bench.support.DatasetSeeder
import com.icure.cardinal.bridge.bench.support.ICureBootstrap
import com.icure.cardinal.bridge.bench.support.InProcessBridge
import com.icure.cardinal.bridge.bench.support.NativeBridge
import com.icure.cardinal.bridge.bench.support.SeededTier
import com.icure.cardinal.bridge.bench.support.Tier
import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.options.EntityListDecodingStrategy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Benchmarks `/bridge/contact/matchBy`, `/filterBy` and `/filterBy/withLinks` against a seeded Kraken, once with each
 * [EntityListDecodingStrategy], using the filter of the request that got stuck on qa.
 * The contacts are modelled after the ones returned by that request (see [DatasetSeeder]): created by an author and
 * shared with a reader, who is the data owner of the bridge session and of the filter.
 * One bridge per strategy runs during the whole benchmark, and each call is made with every strategy in turn, so that
 * jit warm-up and heap state do not favour the strategy measured last.
 *
 * The test fails if a call times out (see below), returns an error status (e.g. 500 when the bridge runs out of heap)
 * or returns an unexpected number of entities; the report still contains every call.
 *
 * Opt-in, runs only with `BRIDGE_BENCH=1`. Requires Docker, access to the Kraken image and the Kraken environment
 * variables (see [Containers]). Other options:
 * - `BRIDGE_BENCH_TIERS`: comma separated `<contacts>x<servicesPerContact>[x<stringBytes>]`, defaults to [Tier.DEFAULT].
 * - `BRIDGE_BENCH_TIMEOUT`: deadline of each bridge call (e.g. `10m`), defaults to 10 minutes. On timeout all coroutines
 *   and threads are dumped and the test fails.
 * - `BRIDGE_BENCH_KRAKEN_IMAGE`: Kraken image to use.
 * - `BRIDGE_BENCH_NATIVE_IMAGE`: a docker image of the bridge (e.g. built from the `Dockerfile`). When set, the calls are
 *   made to this image instead of the in-process bridges, with the decoding strategy it was built with. On timeout the
 *   health, cpu, memory and threads of the container are printed.
 * - `-PbenchHeap=2g`: heap of the test jvm, which also runs the bridge. Defaults to 1g.
 *
 * Results are printed and written to `server/build/reports/bridge-bench.md`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DecodeStrategyBenchmarkTest : StringSpec({
	val enabled = System.getenv("BRIDGE_BENCH") == "1"
	val callTimeout = System.getenv("BRIDGE_BENCH_TIMEOUT")?.takeIf { it.isNotBlank() }?.let { Duration.parse(it) } ?: 10.minutes
	val tiers = Tier.parseAll(System.getenv("BRIDGE_BENCH_TIERS")?.takeIf { it.isNotBlank() } ?: Tier.DEFAULT)
	val strategies = listOf(
		"Strict" to EntityListDecodingStrategy.Strict,
		"DiscardMalformed" to CardinalSdkInitializer.logAndDiscardMalformed,
	)
	val endpoints = listOf("contact/matchBy", "contact/filterBy", "contact/filterBy/withLinks")
	val nativeImage = System.getenv("BRIDGE_BENCH_NATIVE_IMAGE")?.takeIf { it.isNotBlank() }

	lateinit var reader: BenchHcp
	lateinit var seededTiers: List<SeededTier>
	val results = mutableListOf<CallResult>()

	fun filterOf(seededTier: SeededTier): JsonObject = buildJsonObject {
		put("\$type", "ContactByDataOwnerOpeningDateFilter")
		put("dataOwnerId", reader.hcpId)
		put("startDate", seededTier.startDate)
		put("endDate", seededTier.endDate)
	}

	beforeSpec {
		if (!enabled) return@beforeSpec
		DebugProbes.enableCreationStackTraces = false
		DebugProbes.install()
		Containers.start()
		val group = ICureBootstrap.createGroup(krakenUrl = Containers.krakenUrl, couchDbUrl = Containers.couchDbUrl)
		val author = group.createHcp("Author")
		reader = group.createHcp("Reader")
		seededTiers = DatasetSeeder.seed(author.sdk, reader.hcpId, tiers)
	}

	afterSpec {
		if (!enabled) return@afterSpec
		val report = BenchReport.markdown(results)
		println(report)
		println("Report written to ${BenchReport.write(report).absolutePath}")
		Containers.stop()
		DebugProbes.uninstall()
	}

	"compare decoding strategies".config(enabledIf = { enabled }, timeout = 3.hours) {
		val bridges: List<BridgeUnderTest> = if (nativeImage != null) {
			listOf(NativeBridge.start(nativeImage))
		} else {
			strategies.map { (name, strategy) ->
				InProcessBridge.start(name, CardinalSdkInitializer(null, Containers.krakenUrl, strategy))
			}
		}
		try {
			val sessions = bridges.map { bridge ->
				val client = BridgeClient(bridge, callTimeout)
				Triple(bridge, client, client.createSession(Containers.krakenUrl, reader))
			}
			// The first calls of a session initialise the sdk caches: keep them out of the measurements.
			sessions.forEach { (_, client, sessionId) ->
				client.post(sessionId, "contact/filterBy/withLinks", filterOf(seededTiers.first()))
			}
			seededTiers.forEach { seededTier ->
				endpoints.forEach { endpoint ->
					sessions.forEach { (bridge, client, sessionId) ->
						val result = BenchMetrics.measure(bridge, seededTier.tier.label, endpoint) {
							client.post(sessionId, endpoint, filterOf(seededTier))
						}
						println(result)
						results += result
					}
				}
			}
		} finally {
			bridges.forEach { it.close() }
		}
		val expectedEntities = seededTiers.associate { it.tier.label to it.tier.contacts }
		results.filter { it.error != null || it.entities != expectedEntities.getValue(it.tier) }.shouldBeEmpty()
	}
})
