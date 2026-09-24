import java.util.Properties
import kotlin.apply
import kotlin.collections.plus

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

group = "com.icure.bridge"
version = "0.0.7"

private fun Project.getLocalProperties() =
	Properties().apply {
		kotlin.runCatching {
			load(rootProject.file("local.properties").reader())
		}
	}

kotlin {
	jvm()
	val macosArm64 = macosArm64()
	val linuxX64 = linuxX64()
	val linuxArm64 = linuxArm64()
	val nativeTargets = listOf(macosArm64, linuxX64, linuxArm64)
	val linuxTargets = listOf(linuxX64, linuxArm64)
	linuxTargets.forEach { target ->
		target.binaries {
			all {
				freeCompilerArgs += listOf("-linker-option", "--allow-shlib-undefined")
getLocalProperties()["cinteropsLibsDir"]?.also { allDirs ->
					(allDirs as String).split(";").forEach {
						linkerOpts.add(0, "-L$it")
					}
				}
			}
		}
	}
	nativeTargets.forEach { target ->
		target.binaries.executable {
			entryPoint = "com.icure.cardinal.bridge.main"
		}
	}

	sourceSets {
		val commonMain by getting {
			dependencies {
				// Aligns the ktor modules the cardinal sdk depends on with the ktor version of the project.
				implementation(project.dependencies.platform(libs.ktor.bom))
				implementation(libs.ktor.serverCore)
				implementation(libs.ktor.serverCio)
				implementation(libs.ktor.serverContentNegotiation)
				implementation(libs.ktor.clientContentNegotiation)
				implementation(libs.ktor.serverStatusPages)
				implementation(libs.ktor.serializationJson)
				implementation(libs.clikt)
				implementation(libs.cardinal.sdk)
			}
		}
		val jvmTest by getting {
			dependencies {
				implementation(libs.kotest.runnerJunit5)
				implementation(libs.kotest.assertionsCore)
				implementation(libs.testcontainers.core)
				implementation(libs.kotlinx.coroutinesDebug)
			}
		}
		applyDefaultHierarchyTemplate()
		// The http client engines used by each platform.
		jvmMain.dependencies {
			implementation(libs.ktor.clientOkhttp)
		}
		appleMain.dependencies {
			implementation(libs.ktor.clientDarwin)
		}
		linuxMain.dependencies {
			implementation(libs.ktor.clientCurl)
		}
	}
}

// The decode benchmark (DecodeStrategyBenchmarkTest) only runs with BRIDGE_BENCH=1, see its kdoc for the other options.
val benchEnv = listOf(
	"BRIDGE_BENCH",
	"BRIDGE_BENCH_TIERS",
	"BRIDGE_BENCH_TIMEOUT",
	"BRIDGE_BENCH_KRAKEN_IMAGE",
	"BRIDGE_BENCH_NATIVE_IMAGE",
).associateWith { providers.environmentVariable(it).orElse("") }

tasks.named<Test>("jvmTest") {
	useJUnitPlatform()
	maxHeapSize = providers.gradleProperty("benchHeap").getOrElse("1g")
	jvmArgs("-XX:+EnableDynamicAgentLoading")
	testLogging {
		showStandardStreams = true
	}
	benchEnv.forEach { (name, value) -> inputs.property(name, value) }
	if (benchEnv.getValue("BRIDGE_BENCH").get() == "1") {
		outputs.upToDateWhen { false }
	}
}
