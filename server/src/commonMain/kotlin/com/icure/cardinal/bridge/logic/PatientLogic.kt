package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedPatient
import com.icure.cardinal.sdk.model.Patient
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.bridge.model.PatientWithLinks

class PatientLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createPatient(credentials: Credentials, patient: DecryptedPatient): DecryptedPatient =
		sdk(credentials).patient.createPatient(patient)

	suspend fun createPatients(credentials: Credentials, patients: List<DecryptedPatient>): List<DecryptedPatient> =
		sdk(credentials).patient.createPatients(patients)

	suspend fun getPatient(credentials: Credentials, entityId: String): DecryptedPatient? =
		sdk(credentials).patient.getPatient(entityId)

	suspend fun getPatients(credentials: Credentials, patientIds: List<String>): List<DecryptedPatient> =
		sdk(credentials).patient.getPatients(patientIds)

	suspend fun modifyPatient(credentials: Credentials, entity: DecryptedPatient): DecryptedPatient =
		sdk(credentials).patient.modifyPatient(entity)

	suspend fun modifyPatients(credentials: Credentials, patients: List<DecryptedPatient>): List<DecryptedPatient> =
		sdk(credentials).patient.modifyPatients(patients)

	suspend fun deletePatientById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).patient.deletePatientById(entityId, rev)

	suspend fun deletePatientsByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).patient.deletePatientsByIds(entityIds)

	suspend fun undeletePatientById(credentials: Credentials, id: String, rev: String): DecryptedPatient =
		sdk(credentials).patient.undeletePatientById(id, rev)

	suspend fun purgePatientById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).patient.purgePatientById(id, rev)

	// Filter/Match

	suspend fun matchPatientsBy(credentials: Credentials, filter: BaseFilterOptions<Patient>): List<String> =
		sdk(credentials).patient.matchPatientsBy(filter)

	suspend fun matchPatientsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Patient>): List<String> =
		sdk(credentials).patient.matchPatientsBySorted(filter)

	suspend fun filterPatientsBy(credentials: Credentials, filter: BaseFilterOptions<Patient>): List<DecryptedPatient> {
		val iterator = sdk(credentials).patient.filterPatientsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterPatientsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Patient>): List<DecryptedPatient> {
		val iterator = sdk(credentials).patient.filterPatientsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// Patient-specific

	suspend fun getPatientResolvingMerges(credentials: Credentials, patientId: String, maxMergeDepth: Int?): DecryptedPatient =
		sdk(credentials).patient.getPatientResolvingMerges(patientId, maxMergeDepth)

	suspend fun mergePatients(credentials: Credentials, from: Patient, mergedInto: DecryptedPatient): DecryptedPatient =
		sdk(credentials).patient.mergePatients(from, mergedInto)

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, patient: DecryptedPatient): PatientWithLinks {
		val ownSecretIds = sdk(credentials).patient.getSecretIdsOf(patient).values.flatMap { refs -> refs.map { it.entityId } }.toSet()
		return PatientWithLinks(patient, ownSecretIds)
	}

	suspend fun createPatientWithLinks(credentials: Credentials, patient: DecryptedPatient): PatientWithLinks =
		withLinks(credentials, createPatient(credentials, patient))

	suspend fun createPatientsWithLinks(credentials: Credentials, patients: List<DecryptedPatient>): List<PatientWithLinks> =
		createPatients(credentials, patients).map { withLinks(credentials, it) }

	suspend fun getPatientWithLinks(credentials: Credentials, entityId: String): PatientWithLinks? =
		getPatient(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getPatientsWithLinks(credentials: Credentials, patientIds: List<String>): List<PatientWithLinks> =
		getPatients(credentials, patientIds).map { withLinks(credentials, it) }

	suspend fun modifyPatientWithLinks(credentials: Credentials, entity: DecryptedPatient): PatientWithLinks =
		withLinks(credentials, modifyPatient(credentials, entity))

	suspend fun modifyPatientsWithLinks(credentials: Credentials, patients: List<DecryptedPatient>): List<PatientWithLinks> =
		modifyPatients(credentials, patients).map { withLinks(credentials, it) }

	suspend fun undeletePatientByIdWithLinks(credentials: Credentials, id: String, rev: String): PatientWithLinks =
		withLinks(credentials, undeletePatientById(credentials, id, rev))

	suspend fun filterPatientsByWithLinks(credentials: Credentials, filter: BaseFilterOptions<Patient>): List<PatientWithLinks> =
		filterPatientsBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterPatientsBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<Patient>): List<PatientWithLinks> =
		filterPatientsBySorted(credentials, filter).map { withLinks(credentials, it) }

	suspend fun getPatientResolvingMergesWithLinks(credentials: Credentials, patientId: String, maxMergeDepth: Int?): PatientWithLinks =
		withLinks(credentials, getPatientResolvingMerges(credentials, patientId, maxMergeDepth))

	suspend fun mergePatientsWithLinks(credentials: Credentials, from: Patient, mergedInto: DecryptedPatient): PatientWithLinks =
		withLinks(credentials, mergePatients(credentials, from, mergedInto))
}
