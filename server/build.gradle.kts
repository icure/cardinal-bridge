plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

group = "com.icure.bridge"
version = "0.0.1"

kotlin {
	jvm()
	macosArm64 {
		binaries.executable() {
			entryPoint = "com.icure.cardinal.bridge.main"
		}
	}
	linuxX64()

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
		val nativeMain by creating {
			dependsOn(commonMain)
		}
		val macosArm64Main by getting {
			dependsOn(nativeMain)
		}
		val linuxX64Main by getting {
			dependsOn(nativeMain)
		}
	}
}
