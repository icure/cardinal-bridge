package com.icure.cardinal.bridge

import com.icure.cardinal.bridge.config.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.cio.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
	configureSerialization()
}