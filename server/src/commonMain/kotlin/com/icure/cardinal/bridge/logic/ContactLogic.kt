package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.sdk.model.Contact
import com.icure.cardinal.sdk.model.DecryptedContact
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.embed.DecryptedService
import com.icure.cardinal.sdk.model.embed.Service
import com.icure.cardinal.bridge.model.ContactWithLinks
import com.icure.cardinal.bridge.model.ServiceWithLinks
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.utils.InternalIcureApi

class ContactLogic(sdkInitializer: CardinalSdkInitializer) : SdkAware(sdkInitializer) {

	// CRUD

	suspend fun createContact(sessionId: String, contact: DecryptedContact): DecryptedContact =
		sdk(sessionId).contact.createContact(contact)

	suspend fun createContacts(sessionId: String, contacts: List<DecryptedContact>): List<DecryptedContact> =
		sdk(sessionId).contact.createContacts(contacts)

	suspend fun getContact(sessionId: String, entityId: String): DecryptedContact? =
		sdk(sessionId).contact.getContact(entityId)

	suspend fun getContacts(sessionId: String, entityIds: List<String>): List<DecryptedContact> =
		sdk(sessionId).contact.getContacts(entityIds)

	suspend fun modifyContact(sessionId: String, entity: DecryptedContact): DecryptedContact =
		sdk(sessionId).contact.modifyContact(entity)

	suspend fun modifyContacts(sessionId: String, entities: List<DecryptedContact>): List<DecryptedContact> =
		sdk(sessionId).contact.modifyContacts(entities)

	suspend fun deleteContactById(sessionId: String, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(sessionId).contact.deleteContactById(entityId, rev)

	suspend fun deleteContactsByIds(sessionId: String, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(sessionId).contact.deleteContactsByIds(entityIds)

	suspend fun undeleteContactById(sessionId: String, id: String, rev: String): DecryptedContact =
		sdk(sessionId).contact.undeleteContactById(id, rev)

	suspend fun purgeContactById(sessionId: String, id: String, rev: String) =
		sdk(sessionId).contact.purgeContactById(id, rev)

	// Filter/Match

	@OptIn(InternalIcureApi::class)
	suspend fun matchContactsBy(sessionId: String, filter: AbstractFilter<Contact>): List<String> =
		raw(sessionId).contact.matchContactsBy(filter).successBody()

	suspend fun filterContactsBy(sessionId: String, filter: AbstractFilter<Contact>): List<DecryptedContact> =
		getFromMatches(matchContactsBy(sessionId, filter)) { sdk(sessionId).contact.getContacts(it) }

	// Service-specific

	suspend fun getService(sessionId: String, serviceId: String): DecryptedService? =
		sdk(sessionId).contact.getService(serviceId)

	suspend fun getServices(sessionId: String, serviceIds: List<String>): List<DecryptedService> =
		sdk(sessionId).contact.getServices(serviceIds)

	@OptIn(InternalIcureApi::class)
	suspend fun matchServicesBy(sessionId: String, filter: AbstractFilter<Service>): List<String> =
		raw(sessionId).contact.matchServicesBy(filter).successBody()

	suspend fun filterServicesBy(sessionId: String, filter: AbstractFilter<Service>): List<DecryptedService> =
		getFromMatches(matchServicesBy(sessionId, filter)) { sdk(sessionId).contact.getServices(it) }

	// WithLinks

	private suspend fun withLinks(sessionId: String, contact: DecryptedContact): ContactWithLinks {
		val patientIds = sdk(sessionId).contact.decryptPatientIdOf(contact).map { it.entityId }.toSet()
		return ContactWithLinks(contact, patientIds)
	}

	private suspend fun serviceWithLinks(sessionId: String, service: DecryptedService): ServiceWithLinks {
		val patientIds = sdk(sessionId).contact.decryptPatientIdOfService(service).map { it.entityId }.toSet()
		return ServiceWithLinks(service, patientIds)
	}

	suspend fun createContactWithLinks(sessionId: String, contact: DecryptedContact): ContactWithLinks =
		withLinks(sessionId, createContact(sessionId, contact))

	suspend fun createContactsWithLinks(sessionId: String, contacts: List<DecryptedContact>): List<ContactWithLinks> =
		createContacts(sessionId, contacts).map { withLinks(sessionId, it) }

	suspend fun getContactWithLinks(sessionId: String, entityId: String): ContactWithLinks? =
		getContact(sessionId, entityId)?.let { withLinks(sessionId, it) }

	suspend fun getContactsWithLinks(sessionId: String, entityIds: List<String>): List<ContactWithLinks> =
		getContacts(sessionId, entityIds).map { withLinks(sessionId, it) }

	suspend fun modifyContactWithLinks(sessionId: String, entity: DecryptedContact): ContactWithLinks =
		withLinks(sessionId, modifyContact(sessionId, entity))

	suspend fun modifyContactsWithLinks(sessionId: String, entities: List<DecryptedContact>): List<ContactWithLinks> =
		modifyContacts(sessionId, entities).map { withLinks(sessionId, it) }

	suspend fun undeleteContactByIdWithLinks(sessionId: String, id: String, rev: String): ContactWithLinks =
		withLinks(sessionId, undeleteContactById(sessionId, id, rev))

	suspend fun filterContactsByWithLinks(sessionId: String, filter: AbstractFilter<Contact>): List<ContactWithLinks> =
		filterContactsBy(sessionId, filter).map { withLinks(sessionId, it) }

	suspend fun getServiceWithLinks(sessionId: String, serviceId: String): ServiceWithLinks? =
		getService(sessionId, serviceId)?.let { serviceWithLinks(sessionId, it) }

	suspend fun getServicesWithLinks(sessionId: String, serviceIds: List<String>): List<ServiceWithLinks> =
		getServices(sessionId, serviceIds).map { serviceWithLinks(sessionId, it) }

	suspend fun filterServicesByWithLinks(sessionId: String, filter: AbstractFilter<Service>): List<ServiceWithLinks> =
		filterServicesBy(sessionId, filter).map { serviceWithLinks(sessionId, it) }
}
