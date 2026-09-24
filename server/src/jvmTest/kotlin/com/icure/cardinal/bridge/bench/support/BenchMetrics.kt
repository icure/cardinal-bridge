package com.icure.cardinal.bridge.bench.support

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.Locale

/**
 * Measurements of one call to a bridge.
 *
 * [error] is set if the bridge answered with an error status. See [BridgeUnderTest.memoryMeasure] for the meaning of
 * [peakMemoryBytes], [gcCount] and [gcMillis], which depends on the bridge.
 */
data class CallResult(
	val bridge: String,
	val tier: String,
	val endpoint: String,
	val entities: Int,
	val responseBytes: Int,
	val wallMillis: Long,
	val gcCount: Long?,
	val gcMillis: Long?,
	val peakMemoryBytes: Long,
	val error: String? = null,
)

object BenchMetrics {
	/**
	 * Runs [call] on [bridge] and measures it. [call] returns the raw json array returned by the bridge.
	 */
	fun measure(bridge: BridgeUnderTest, tier: String, endpoint: String, call: () -> ByteArray): CallResult {
		bridge.startMemoryMeasure()
		val start = System.nanoTime()
		val response = runCatching { call() }.onFailure { if (it !is BridgeCallException) throw it }
		val wallMillis = (System.nanoTime() - start) / 1_000_000
		val memory = bridge.memoryMeasure()
		val body = response.getOrNull()
		return CallResult(
			bridge = bridge.name,
			tier = tier,
			endpoint = endpoint,
			entities = body?.let { countTopLevelArrayElements(it) } ?: 0,
			responseBytes = body?.size ?: 0,
			wallMillis = wallMillis,
			gcCount = memory.gcCount,
			gcMillis = memory.gcMillis,
			peakMemoryBytes = memory.peakBytes,
			error = response.exceptionOrNull()?.message,
		)
	}

	/**
	 * Counts the elements of a json array without parsing it, to keep big responses from weighing on the heap.
	 */
	fun countTopLevelArrayElements(json: ByteArray): Int {
		var depth = 0
		var inString = false
		var escaped = false
		var separators = 0
		var hasElements = false
		for (byte in json) {
			val c = byte.toInt().toChar()
			if (inString) {
				when {
					escaped -> escaped = false
					c == '\\' -> escaped = true
					c == '"' -> inString = false
				}
				continue
			}
			when (c) {
				'"' -> {
					inString = true
					if (depth == 1) hasElements = true
				}
				'[', '{' -> {
					depth++
					if (depth == 2) hasElements = true
				}
				']', '}' -> depth--
				',' -> if (depth == 1) separators++
				' ', '\n', '\r', '\t' -> {}
				else -> if (depth == 1) hasElements = true
			}
		}
		return if (hasElements) separators + 1 else 0
	}
}

object BenchReport {
	private const val MB = 1024.0 * 1024.0

	fun markdown(results: List<CallResult>): String = buildString {
		appendLine("| Bridge | Tier | Endpoint | Entities | Response MB | Wall ms | GC count | GC ms | Peak memory MB | Error |")
		appendLine("|---|---|---|---:|---:|---:|---:|---:|---:|---|")
		results.forEach { r ->
			appendLine(
				"| ${r.bridge} | ${r.tier} | ${r.endpoint} | ${r.entities} | ${"%.1f".format(Locale.ROOT, r.responseBytes / MB)} | " +
					"${r.wallMillis} | ${r.gcCount ?: "-"} | ${r.gcMillis ?: "-"} | ${"%.0f".format(Locale.ROOT, r.peakMemoryBytes / MB)} | " +
					"${r.error?.lineSequence()?.first()?.take(80).orEmpty()} |"
			)
		}
	}

	/**
	 * Writes [report] to `build/reports/bridge-bench.md` of the module.
	 */
	fun write(report: String): File =
		File("build/reports/bridge-bench.md").apply {
			parentFile.mkdirs()
			writeText(report)
		}
}

@OptIn(ExperimentalCoroutinesApi::class)
object Diagnostics {
	fun dumpCoroutinesAndThreads(): String = buildString {
		appendLine("=== Coroutines ===")
		if (DebugProbes.isInstalled) {
			val out = ByteArrayOutputStream()
			DebugProbes.dumpCoroutines(PrintStream(out))
			append(out.toString())
		} else {
			appendLine("DebugProbes not installed")
		}
		appendLine("=== Threads ===")
		Thread.getAllStackTraces().forEach { (thread, stack) ->
			appendLine("\"${thread.name}\" ${thread.state}")
			stack.forEach { appendLine("    at $it") }
			appendLine()
		}
	}
}
