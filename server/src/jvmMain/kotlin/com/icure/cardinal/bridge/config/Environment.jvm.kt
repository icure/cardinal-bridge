package com.icure.cardinal.bridge.config

actual fun getEnvironmentVariable(name: String): String? =
	System.getenv(name)