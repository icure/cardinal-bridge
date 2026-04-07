plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

group = "com.icure.bridge"
version = "0.0.1"

kotlin {
	jvm()

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(libs.ktor.serverCore)
				implementation(libs.ktor.serverCio)
				implementation(libs.ktor.serverContentMegotiation)
				implementation(libs.ktor.serializationJson)
				implementation(libs.cardinal.sdk)
			}
		}
	}
}