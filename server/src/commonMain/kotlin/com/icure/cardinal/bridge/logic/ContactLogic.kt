package com.icure.cardinal.bridge.logic

import com.icure.cardinal.bridge.components.CardinalSdkInitializer
import com.icure.cardinal.bridge.model.Credentials
import com.icure.cardinal.sdk.filters.BaseFilterOptions
import com.icure.cardinal.sdk.filters.BaseSortableFilterOptions
import com.icure.cardinal.sdk.model.Contact
import com.icure.cardinal.sdk.model.DecryptedContact
import com.icure.cardinal.sdk.model.StoredDocumentIdentifier
import com.icure.cardinal.sdk.model.embed.DecryptedService
import com.icure.cardinal.sdk.model.embed.Service
import com.icure.cardinal.bridge.model.ContactWithLinks
import com.icure.cardinal.bridge.model.ServiceWithLinks

class ContactLogic(private val sdkInitializer: CardinalSdkInitializer) {
	private suspend fun sdk(credentials: Credentials) =
		sdkInitializer.getOrInit(credentials)

	// CRUD

	suspend fun createContact(credentials: Credentials, contact: DecryptedContact): DecryptedContact =
		sdk(credentials).contact.createContact(contact)

	suspend fun createContacts(credentials: Credentials, contacts: List<DecryptedContact>): List<DecryptedContact> =
		sdk(credentials).contact.createContacts(contacts)

	suspend fun getContact(credentials: Credentials, entityId: String): DecryptedContact? =
		sdk(credentials).contact.getContact(entityId)

	suspend fun getContacts(credentials: Credentials, entityIds: List<String>): List<DecryptedContact> =
		sdk(credentials).contact.getContacts(entityIds)

	suspend fun modifyContact(credentials: Credentials, entity: DecryptedContact): DecryptedContact =
		sdk(credentials).contact.modifyContact(entity)

	suspend fun modifyContacts(credentials: Credentials, entities: List<DecryptedContact>): List<DecryptedContact> =
		sdk(credentials).contact.modifyContacts(entities)

	suspend fun deleteContactById(credentials: Credentials, entityId: String, rev: String): StoredDocumentIdentifier =
		sdk(credentials).contact.deleteContactById(entityId, rev)

	suspend fun deleteContactsByIds(credentials: Credentials, entityIds: List<StoredDocumentIdentifier>): List<StoredDocumentIdentifier> =
		sdk(credentials).contact.deleteContactsByIds(entityIds)

	suspend fun undeleteContactById(credentials: Credentials, id: String, rev: String): DecryptedContact =
		sdk(credentials).contact.undeleteContactById(id, rev)

	suspend fun purgeContactById(credentials: Credentials, id: String, rev: String) =
		sdk(credentials).contact.purgeContactById(id, rev)

	// Filter/Match

	suspend fun matchContactsBy(credentials: Credentials, filter: BaseFilterOptions<Contact>): List<String> =
		sdk(credentials).contact.matchContactsBy(filter)

	suspend fun matchContactsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Contact>): List<String> =
		sdk(credentials).contact.matchContactsBySorted(filter)

	suspend fun filterContactsBy(credentials: Credentials, filter: BaseFilterOptions<Contact>): List<DecryptedContact> {
		val iterator = sdk(credentials).contact.filterContactsBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterContactsBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Contact>): List<DecryptedContact> {
		val iterator = sdk(credentials).contact.filterContactsBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// Service-specific

	suspend fun getService(credentials: Credentials, serviceId: String): DecryptedService? =
		sdk(credentials).contact.getService(serviceId)

	suspend fun getServices(credentials: Credentials, serviceIds: List<String>): List<DecryptedService> =
		sdk(credentials).contact.getServices(serviceIds)

	suspend fun matchServicesBy(credentials: Credentials, filter: BaseFilterOptions<Service>): List<String> =
		sdk(credentials).contact.matchServicesBy(filter)

	suspend fun matchServicesBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Service>): List<String> =
		sdk(credentials).contact.matchServicesBySorted(filter)

	suspend fun filterServicesBy(credentials: Credentials, filter: BaseFilterOptions<Service>): List<DecryptedService> {
		val iterator = sdk(credentials).contact.filterServicesBy(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	suspend fun filterServicesBySorted(credentials: Credentials, filter: BaseSortableFilterOptions<Service>): List<DecryptedService> {
		val iterator = sdk(credentials).contact.filterServicesBySorted(filter)
		return buildList { while (iterator.hasNext()) addAll(iterator.next(100)) }
	}

	// WithLinks

	private suspend fun withLinks(credentials: Credentials, contact: DecryptedContact): ContactWithLinks {
		val patientIds = sdk(credentials).contact.decryptPatientIdOf(contact).map { it.entityId }.toSet()
		return ContactWithLinks(contact, patientIds)
	}

	private suspend fun serviceWithLinks(credentials: Credentials, service: DecryptedService): ServiceWithLinks {
		// TODO: use the metadata of the service as soon as the new Cardinal SDK is published
		val patientIds = service.contactId?.let { contactId ->
			sdk(credentials).contact.getContact(contactId)?.let { contact ->
				sdk(credentials).contact.decryptPatientIdOf(contact).map { it.entityId }.toSet()
			}
		} ?: emptySet()
		return ServiceWithLinks(service, patientIds)
	}

	suspend fun createContactWithLinks(credentials: Credentials, contact: DecryptedContact): ContactWithLinks =
		withLinks(credentials, createContact(credentials, contact))

	suspend fun createContactsWithLinks(credentials: Credentials, contacts: List<DecryptedContact>): List<ContactWithLinks> =
		createContacts(credentials, contacts).map { withLinks(credentials, it) }

	suspend fun getContactWithLinks(credentials: Credentials, entityId: String): ContactWithLinks? =
		getContact(credentials, entityId)?.let { withLinks(credentials, it) }

	suspend fun getContactsWithLinks(credentials: Credentials, entityIds: List<String>): List<ContactWithLinks> =
		getContacts(credentials, entityIds).map { withLinks(credentials, it) }

	suspend fun modifyContactWithLinks(credentials: Credentials, entity: DecryptedContact): ContactWithLinks =
		withLinks(credentials, modifyContact(credentials, entity))

	suspend fun modifyContactsWithLinks(credentials: Credentials, entities: List<DecryptedContact>): List<ContactWithLinks> =
		modifyContacts(credentials, entities).map { withLinks(credentials, it) }

	suspend fun undeleteContactByIdWithLinks(credentials: Credentials, id: String, rev: String): ContactWithLinks =
		withLinks(credentials, undeleteContactById(credentials, id, rev))

	suspend fun filterContactsByWithLinks(credentials: Credentials, filter: BaseFilterOptions<Contact>): List<ContactWithLinks> =
		filterContactsBy(credentials, filter).map { withLinks(credentials, it) }

	suspend fun filterContactsBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<Contact>): List<ContactWithLinks> =
		filterContactsBySorted(credentials, filter).map { withLinks(credentials, it) }

	suspend fun getServiceWithLinks(credentials: Credentials, serviceId: String): ServiceWithLinks? =
		getService(credentials, serviceId)?.let { serviceWithLinks(credentials, it) }

	suspend fun getServicesWithLinks(credentials: Credentials, serviceIds: List<String>): List<ServiceWithLinks> =
		getServices(credentials, serviceIds).map { serviceWithLinks(credentials, it) }

	suspend fun filterServicesByWithLinks(credentials: Credentials, filter: BaseFilterOptions<Service>): List<ServiceWithLinks> =
		filterServicesBy(credentials, filter).map { serviceWithLinks(credentials, it) }

	suspend fun filterServicesBySortedWithLinks(credentials: Credentials, filter: BaseSortableFilterOptions<Service>): List<ServiceWithLinks> =
		filterServicesBySorted(credentials, filter).map { serviceWithLinks(credentials, it) }
}
