package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.DecryptedForm
import com.icure.cardinal.sdk.model.Form
import com.icure.cardinal.bridge.model.FormWithLinks
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier

class FormLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createForm(credentials: Credentials, entity: DecryptedForm): DecryptedForm =
		sdk(credentials).form.createForm(entity)

	suspend fun createForms(credentials: Credentials, entities: List<DecryptedForm>): List<DecryptedForm> =
		sdk(credentials).form.createForms(entities)

	suspend fun getForm(credentials: Credentials, entityId: String): DecryptedForm? =
		sdk(credentials).form.getForm(entityId)

	suspend fun getForms(credentials: Credentials, entityIds: List<String>): List<DecryptedForm> =
		sdk(credentials).form.getForms(entityIds)

	suspend fun modifyForm(credentials: Credentials, entity: DecryptedForm): DecryptedForm =
		sdk(credentials).form.modifyForm(entity)

	suspend fun modifyForms(credentials: Credentials, entities: List<DecryptedForm>): List<DecryptedForm> =
		sdk(credentials).form.modifyForms(entities)

	suspend fun deleteFormById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).form.deleteFormById(entityId, rev)

	suspend fun deleteFormsByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).form.deleteFormsByIds(entityIds)

	suspend fun undeleteFormById(credentials: Credentials, id: String, rev: String): DecryptedForm =
		sdk(credentials).form.undeleteFormById(id, rev)

	suspend fun purgeFormById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).form.purgeFormById(id, rev)

	// Filter/Match

	suspend fun matchFormsBy(credentials: Credentials, filter: BaseFilterOptions<Form>): List<String> =
		sdk(credentials).form.matchFormsBy(filter)

	suspend fun matchFormsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Form>): List<String> =
		sdk(credentials).form.matchFormsBySorted(filter)

	suspend fun filterFormsBy(credentials: Credentials, filter: BaseFilterOptions<Form>): List<DecryptedForm> {
		val iterator = sdk(credentials).form.filterFormsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterFormsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Form>): List<DecryptedForm> {
		val iterator = sdk(credentials).form.filterFormsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// Form-specific

	suspend fun getLatestFormByUniqueId(credentials: Credentials, uniqueId: String): DecryptedForm =
		sdk(credentials).form.getLatestFormByUniqueId(uniqueId)

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, form: DecryptedForm): FormWithLinks {
		val patientIds = sdk(credentials).form.decryptPatientIdOf(form).map { it.entityId }.toSet()
		return FormWithLinks(form, patientIds)
	}

	suspend fun createFormWithLinks(credentials: Credentials, entity: DecryptedForm): FormWithLinks =
		withLinks(credentials, createForm(credentials, entity))

	suspend fun createFormsWithLinks(credentials: Credentials, entities: List<DecryptedForm>): List<FormWithLinks> =
		createForms(credentials, entities).map { withLinks(credentials, it) }

	suspend fun getFormWithLinks(credentials: Credentials, entityId: String): FormWithLinks? =
		getForm(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getFormsWithLinks(credentials: Credentials, entityIds: List<String>): List<FormWithLinks> =
		getForms(credentials, entityIds).map { withLinks(credentials, it) }

	suspend fun modifyFormWithLinks(credentials: Credentials, entity: DecryptedForm): FormWithLinks =
		withLinks(credentials, modifyForm(credentials, entity))

	suspend fun modifyFormsWithLinks(credentials: Credentials, entities: List<DecryptedForm>): List<FormWithLinks> =
		modifyForms(credentials, entities).map { withLinks(credentials, it) }

	suspend fun undeleteFormByIdWithLinks(credentials: Credentials, id: String, rev: String): FormWithLinks =
		withLinks(credentials, undeleteFormById(credentials, id, rev))

	suspend fun filterFormsByWithLinks(credentials: Credentials, filter: BaseFilterOptions<Form>): List<FormWithLinks> =
		filterFormsBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterFormsBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<Form>): List<FormWithLinks> =
		filterFormsBySorted(credentials, filter).map { withLinks(credentials, it) }

	suspend fun getLatestFormByUniqueIdWithLinks(credentials: Credentials, uniqueId: String): FormWithLinks =
		withLinks(credentials, getLatestFormByUniqueId(credentials, uniqueId))
}
