import java.util.Properties
import kotlin.apply
import kotlin.collections.plus

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

group = "com.icure.bridge"
version = "0.0.1"

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
		applyDefaultHierarchyTemplate()
	}
}
