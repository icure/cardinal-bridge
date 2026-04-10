package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedPatient
import com.icure.cardinal.sdk.model.Patient
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.bridge.model.PatientWithLinks
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.utils.InternalIcureApi

class PatientLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

	suspend fun createPatient(sessionId: String, patient: DecryptedPatient): DecryptedPatient =
		sdk(sessionId).patient.createPatient(patient)

	suspend fun createPatients(sessionId: String, patients: List<DecryptedPatient>): List<DecryptedPatient> =
		sdk(sessionId).patient.createPatients(patients)

	suspend fun getPatient(sessionId: String, entityId: String): DecryptedPatient? =
		sdk(sessionId).patient.getPatient(entityId)

	suspend fun getPatients(sessionId: String, patientIds: List<String>): List<DecryptedPatient> =
		sdk(sessionId).patient.getPatients(patientIds)

	suspend fun modifyPatient(sessionId: String, entity: DecryptedPatient): DecryptedPatient =
		sdk(sessionId).patient.modifyPatient(entity)

	suspend fun modifyPatients(sessionId: String, patients: List<DecryptedPatient>): List<DecryptedPatient> =
		sdk(sessionId).patient.modifyPatients(patients)

	suspend fun deletePatientById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).patient.deletePatientById(entityId, rev)

	suspend fun deletePatientsByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).patient.deletePatientsByIds(entityIds)

	suspend fun undeletePatientById(sessionId: String, id: String, rev: String): DecryptedPatient =
		sdk(sessionId).patient.undeletePatientById(id, rev)

	suspend fun purgePatientById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).patient.purgePatientById(id, rev)

	// Filter/Match

	@OptIn(InternalIcureApi::class)
	suspend fun matchPatientsBy(sessionId: String, filter: AbstractFilter<Patient>): List<String> =
		raw(sessionId).patient.matchPatientsBy(filter).successBody()

	suspend fun filterPatientsBy(sessionId: String, filter: AbstractFilter<Patient>): List<DecryptedPatient> =
		getFromMatches(matchPatientsBy(sessionId, filter), { sdk(sessionId).patient.getPatients(it) })

	// Patient-specific

	suspend fun getPatientResolvingMerges(sessionId: String, patientId: String, maxMergeDepth: Int?): DecryptedPatient =
		sdk(sessionId).patient.getPatientResolvingMerges(patientId, maxMergeDepth)

	suspend fun mergePatients(sessionId: String, from: Patient, mergedInto: DecryptedPatient): DecryptedPatient =
		sdk(sessionId).patient.mergePatients(from, mergedInto)

	// WithLinks

	private suspend fun withLinks(sessionId: String, patient: DecryptedPatient): PatientWithLinks {
		val ownSecretIds = sdk(sessionId).patient.getSecretIdsOf(patient).values.flatMap { refs -> refs.map { it.entityId } }.toSet()
		return PatientWithLinks(patient, ownSecretIds)
	}

	suspend fun createPatientWithLinks(sessionId: String, patient: DecryptedPatient): PatientWithLinks =
		withLinks(sessionId, createPatient(sessionId, patient))

	suspend fun createPatientsWithLinks(sessionId: String, patients: List<DecryptedPatient>): List<PatientWithLinks> =
		createPatients(sessionId, patients).map { withLinks(sessionId, it) }

	suspend fun getPatientWithLinks(sessionId: String, entityId: String): PatientWithLinks? =
		getPatient(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getPatientsWithLinks(sessionId: String, patientIds: List<String>): List<PatientWithLinks> =
		getPatients(sessionId, patientIds).map { withLinks(sessionId, it) }

	suspend fun modifyPatientWithLinks(sessionId: String, entity: DecryptedPatient): PatientWithLinks =
		withLinks(sessionId, modifyPatient(sessionId, entity))

	suspend fun modifyPatientsWithLinks(sessionId: String, patients: List<DecryptedPatient>): List<PatientWithLinks> =
		modifyPatients(sessionId, patients).map { withLinks(sessionId, it) }

	suspend fun undeletePatientByIdWithLinks(sessionId: String, id: String, rev: String): PatientWithLinks =
		withLinks(sessionId, undeletePatientById(sessionId, id, rev))

	suspend fun filterPatientsByWithLinks(sessionId: String, filter: AbstractFilter<Patient>): List<PatientWithLinks> =
		filterPatientsBy(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun getPatientResolvingMergesWithLinks(sessionId: String, patientId: String, maxMergeDepth: Int?): PatientWithLinks =
		withLinks(sessionId, getPatientResolvingMerges(sessionId, patientId, maxMergeDepth))

	suspend fun mergePatientsWithLinks(sessionId: String, from: Patient, mergedInto: DecryptedPatient): PatientWithLinks =
		withLinks(sessionId, mergePatients(sessionId, from, mergedInto))
}
