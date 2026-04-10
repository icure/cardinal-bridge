package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.model.DecryptedForm
import com.icure.cardinal.sdk.model.Form
import com.icure.cardinal.bridge.model.FormWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.utils.InternalIcureApi

class FormLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

/*
	suspend fun createForm(sessionId: String, entity: DecryptedForm): DecryptedForm =
		sdk(sessionId).form.createForm(entity)

	suspend fun createForms(sessionId: String, entities: List<DecryptedForm>): List<DecryptedForm> =
		sdk(sessionId).form.createForms(entities)
*/

	suspend fun getForm(sessionId: String, entityId: String): Form? =
		sdk(sessionId).form.tryAndRecover.getForm(entityId)

	suspend fun getForms(sessionId: String, entityIds: List<String>): List<Form> =
		sdk(sessionId).form.tryAndRecover.getForms(entityIds)

/*
	suspend fun modifyForm(sessionId: String, entity: DecryptedForm): DecryptedForm =
		sdk(sessionId).form.modifyForm(entity)

	suspend fun modifyForms(sessionId: String, entities: List<DecryptedForm>): List<DecryptedForm> =
		sdk(sessionId).form.modifyForms(entities)

	suspend fun deleteFormById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).form.deleteFormById(entityId, rev)

	suspend fun deleteFormsByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).form.deleteFormsByIds(entityIds)

	suspend fun undeleteFormById(sessionId: String, id: String, rev: String): DecryptedForm =
		sdk(sessionId).form.undeleteFormById(id, rev)

	suspend fun purgeFormById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).form.purgeFormById(id, rev)
*/

	// Filter/Match

	@OptIn(InternalIcureApi::class)
	suspend fun matchFormsBy(sessionId: String, filter: AbstractFilter<Form>): List<String> =
		raw(sessionId).form.matchFormsBy(filter).successBody()

	suspend fun filterFormsBy(sessionId: String, filter: AbstractFilter<Form>): List<Form> =
		getFromMatches(matchFormsBy(sessionId, filter)) { sdk(sessionId).form.tryAndRecover.getForms(it) }

	// Form-specific

	suspend fun getLatestFormByUniqueId(sessionId: String, uniqueId: String): DecryptedForm =
		sdk(sessionId).form.getLatestFormByUniqueId(uniqueId)

	// WithLinks

	private suspend fun withLinks(sessionId: String, form: Form): FormWithLinks {
		val patientIds = sdk(sessionId).form.decryptPatientIdOf(form).map { it.entityId }.toSet()
		return FormWithLinks(form, patientIds)
	}

/*
	suspend fun createFormWithLinks(sessionId: String, entity: DecryptedForm): FormWithLinks =
		withLinks(sessionId, createForm(sessionId, entity))

	suspend fun createFormsWithLinks(sessionId: String, entities: List<DecryptedForm>): List<FormWithLinks> =
		createForms(sessionId, entities).map { withLinks(sessionId, it) }
*/

	suspend fun getFormWithLinks(sessionId: String, entityId: String): FormWithLinks? =
		getForm(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getFormsWithLinks(sessionId: String, entityIds: List<String>): List<FormWithLinks> =
		getForms(sessionId, entityIds).map { withLinks(sessionId, it) }

/*
	suspend fun modifyFormWithLinks(sessionId: String, entity: DecryptedForm): FormWithLinks =
		withLinks(sessionId, modifyForm(sessionId, entity))

	suspend fun modifyFormsWithLinks(sessionId: String, entities: List<DecryptedForm>): List<FormWithLinks> =
		modifyForms(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteFormByIdWithLinks(sessionId: String, id: String, rev: String): FormWithLinks =
		withLinks(sessionId, undeleteFormById(sessionId, id, rev))
*/

	suspend fun filterFormsByWithLinks(sessionId: String, filter: AbstractFilter<Form>): List<FormWithLinks> =
		filterFormsBy(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun getLatestFormByUniqueIdWithLinks(sessionId: String, uniqueId: String): FormWithLinks =
		withLinks(sessionId, getLatestFormByUniqueId(sessionId, uniqueId))
}
