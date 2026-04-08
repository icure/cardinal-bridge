package com.icure.cardinal.bridge

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.config.configureErrorHandler
import com.icure.cardinal.bridge.config.configureSerialization
import com.icure.cardinal.bridge.controllers.configureRouting
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

class ServerMain : CliktCommand() {
	val port: Int by option(help = "Server port").int().default(8080)
	val applicationId: String? by option(help = "The application id to use for connecting to cardinal")
	val baseUrl: String by option(help = "The base url of the cardinal backend").default("https://api.icure.cloud")

	override fun run() {
		embeddedServer(
			CIO,
			port = port,
			module = {
				configureSerialization()
				configureErrorHandler()
				configureRouting(
					CardinalSdkInitializer(
						applicationId,
						baseUrl
					)
				)
			}
		).start(wait = true)
	}
}

fun main(args: Array<String>) = ServerMain().main(args)