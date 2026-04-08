package com.icure.cardinal.bridge.controllers

import com.icure.cardinal.bridge.logic.PatientLogic
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
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
import com.icure.cardinal.bridge.model.PatientWithLinks
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
			call.respond(logic.createPatient(credentials(), call.receive<DecryptedPatient>()))
		}

		post("/createMany") {
			call.respond(logic.createPatients(credentials(), call.receive<List<DecryptedPatient>>()))
		}

		get("/{id}") {
			val id = call.parameters["id"]!!
			val patient = logic.getPatient(credentials(), id)
			if (patient != null) {
				call.respond(patient)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}

		post("/getMany") {
			call.respond(logic.getPatients(credentials(), call.receive<List<String>>()))
		}

		put("/modify") {
			call.respond(logic.modifyPatient(credentials(), call.receive<DecryptedPatient>()))
		}

		put("/modifyMany") {
			call.respond(logic.modifyPatients(credentials(), call.receive<List<DecryptedPatient>>()))
		}

		delete("/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.deletePatientById(credentials(), id, rev))
		}

		post("/deleteMany") {
			call.respond(logic.deletePatientsByIds(credentials(), call.receive<List<StoredDocumentIdentifier>>()))
		}

		post("/undelete/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			call.respond(logic.undeletePatientById(credentials(), id, rev))
		}

		delete("/purge/{id}/{rev}") {
			val id = call.parameters["id"]!!
			val rev = call.parameters["rev"]!!
			logic.purgePatientById(credentials(), id, rev)
			call.respond(HttpStatusCode.NoContent)
		}

		// Filter/Match
		post("/matchBy") {
			call.respond(logic.matchPatientsBy(credentials(), call.receive<BaseFilterOptions<Patient>>()))
		}

		post("/matchBySorted") {
			call.respond(logic.matchPatientsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Patient>>()))
		}

		post("/filterBy") {
			call.respond(logic.filterPatientsBy(credentials(), call.receive<BaseFilterOptions<Patient>>()))
		}

		post("/filterBySorted") {
			call.respond(logic.filterPatientsBySorted(credentials(), call.receive<BaseSortableFilterOptions<Patient>>()))
		}

		// Patient-specific
		get("/resolve/{id}") {
			val id = call.parameters["id"]!!
			val maxMergeDepth = call.request.queryParameters["maxMergeDepth"]?.toIntOrNull()
			call.respond(logic.getPatientResolvingMerges(credentials(), id, maxMergeDepth))
		}

		post("/merge") {
			val request = call.receive<MergePatientsRequest>()
			call.respond(logic.mergePatients(credentials(), request.from, request.mergedInto))
		}

		// WithLinks variants
		post("/create/withLinks") {
			call.respond(logic.createPatientWithLinks(credentials(), call.receive<DecryptedPatient>()))
		}

		post("/createMany/withLinks") {
			call.respond(logic.createPatientsWithLinks(credentials(), call.receive<List<DecryptedPatient>>()))
		}

		get("/{id}/withLinks") {
			val result = logic.getPatientWithLinks(credentials(), call.parameters["id"]!!)
			if (result != null) call.respond(result) else call.respond(HttpStatusCode.NotFound)
		}

		post("/getMany/withLinks") {
			call.respond(logic.getPatientsWithLinks(credentials(), call.receive<List<String>>()))
		}

		put("/modify/withLinks") {
			call.respond(logic.modifyPatientWithLinks(credentials(), call.receive<DecryptedPatient>()))
		}

		put("/modifyMany/withLinks") {
			call.respond(logic.modifyPatientsWithLinks(credentials(), call.receive<List<DecryptedPatient>>()))
		}

		post("/undelete/{id}/{rev}/withLinks") {
			call.respond(logic.undeletePatientByIdWithLinks(credentials(), call.parameters["id"]!!, call.parameters["rev"]!!))
		}

		post("/filterBy/withLinks") {
			call.respond(logic.filterPatientsByWithLinks(credentials(), call.receive<BaseFilterOptions<Patient>>()))
		}

		post("/filterBySorted/withLinks") {
			call.respond(logic.filterPatientsBySortedWithLinks(credentials(), call.receive<BaseSortableFilterOptions<Patient>>()))
		}

		get("/resolve/{id}/withLinks") {
			val maxMergeDepth = call.request.queryParameters["maxMergeDepth"]?.toIntOrNull()
			call.respond(logic.getPatientResolvingMergesWithLinks(credentials(), call.parameters["id"]!!, maxMergeDepth))
		}

		post("/merge/withLinks") {
			val request = call.receive<MergePatientsRequest>()
			call.respond(logic.mergePatientsWithLinks(credentials(), request.from, request.mergedInto))
		}
	}
}
