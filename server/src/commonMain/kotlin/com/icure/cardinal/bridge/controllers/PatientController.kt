package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.PatientLogic
import com.icure.cardinal.sdk.model.DecryptedPatient
import com.icure.cardinal.sdk.model.Patient
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class MergePatientsRequest(
	val from: Patient,
	val mergedInto: DecryptedPatient,
)

fun Route.patientRoutes(logic: PatientLogic) {
	route("/patient") {
		// CRUD
		post("/create") {
			call.respond(logic.createPatient(sessionId(), call.receive<DecryptedPatient>()))
		}

		post("/createMany") {
			call.respond(logic.createPatients(sessionId(), call.receive<List<DecryptedPatient>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val patient = logic.getPatient(sessionId(), id)
			if (patient != null) {
				call.respond(patient)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getPatients(sessionId(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyPatient(sessionId(), call.receive<DecryptedPatient>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyPatients(sessionId(), call.receive<List<DecryptedPatient>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deletePatientById(sessionId(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deletePatientsByIds(sessionId(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeletePatientById(sessionId(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgePatientById(sessionId(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchPatientsBy(sessionId(), call.receive()))
		}


		post("/filterBy") {
			call.respond(logic.filterPatientsBy(sessionId(), call.receive()))
		}

		// Patient-specific
		get("/resolve/{id}") {
			val id = call.parameters["id"]!!
			val maxMergeDepth = call.request.queryParameters["maxMergeDepth"]?.toIntOrNull()
			call.respond(logic.getPatientResolvingMerges(sessionId(), id, maxMergeDepth))
		}

		post("/merge") {
			val request = call.receive<MergePatientsRequest>()
			call.respond(logic.mergePatients(sessionId(), request.from, request.mergedInto))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createPatientWithLinks(sessionId(), call.receive<DecryptedPatient>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createPatientsWithLinks(sessionId(), call.receive<List<DecryptedPatient>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getPatientWithLinks(sessionId(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getPatientsWithLinks(sessionId(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyPatientWithLinks(sessionId(), call.receive<DecryptedPatient>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyPatientsWithLinks(sessionId(), call.receive<List<DecryptedPatient>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeletePatientByIdWithLinks(sessionId(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterPatientsByWithLinks(sessionId(), call.receive()))
		}

		get("/resolve/{id}/withLinks") {
			val maxMergeDepth = call.request.queryParameters["maxMergeDepth"]?.toIntOrNull()
			call.respond(logic.getPatientResolvingMergesWithLinks(sessionId(), call.parameters["id"]!!, maxMergeDepth))
		}

		post("/merge/withLinks") {
			val request = call.receive<MergePatientsRequest>()
			call.respond(logic.mergePatientsWithLinks(sessionId(), request.from, request.mergedInto))
		}
	}
}
