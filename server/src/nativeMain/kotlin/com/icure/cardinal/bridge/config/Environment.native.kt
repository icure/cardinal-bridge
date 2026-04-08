package com.icure.cardinal.bridge.config

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvironmentVariable(name: String): String? =
	getenv(name)?.toKStringFromUtf8()