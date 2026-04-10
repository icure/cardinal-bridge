package com.icure.cardinal.bridge.serialization

import com.icure.cardinal.bridge.logic.HealthElementLogic
import com.icure.cardinal.bridge.logic.MessageLogic
import com.icure.cardinal.sdk.model.CalendarItem
import com.icure.cardinal.sdk.model.Contact
import com.icure.cardinal.sdk.model.Document
import com.icure.cardinal.sdk.model.Form
import com.icure.cardinal.sdk.model.HealthElement
import com.icure.cardinal.sdk.model.Message
import com.icure.cardinal.sdk.model.Patient
import com.icure.cardinal.sdk.model.base.Identifiable
import com.icure.cardinal.sdk.model.embed.Service
import com.icure.cardinal.sdk.model.filter.AbstractFilter
import com.icure.cardinal.sdk.utils.Serialization
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object FilterSerializers {

	val calendarItem = filterSerializer<CalendarItem>()

	val contact = filterSerializer<Contact>()

	val service = filterSerializer<Service>()

	val document = filterSerializer<Document>()

	val form = filterSerializer<Form>()

	val healthElement = filterSerializer<HealthElement>()

	val message = filterSerializer<Message>()

	val patient = filterSerializer<Patient>()

	@OptIn(ExperimentalSerializationApi::class)
	@Suppress("UNCHECKED_CAST")
	private inline fun <reified T : Identifiable<String>> filterSerializer(): KSerializer<AbstractFilter<T>> =
		Serialization.lenientJson.serializersModule.getContextual(
			AbstractFilter::class,
			listOf(typeMarkerSerializer<T>("AbstractFilter<${T::class.simpleName}>"))
		)!! as KSerializer<AbstractFilter<T>>

	private fun <T> typeMarkerSerializer(name: String): KSerializer<T> =
		object : TypeMarkerSerializer<T>() {
			override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)
		}

	private abstract class TypeMarkerSerializer<T> : KSerializer<T> {
		override fun serialize(encoder: Encoder, value: T) {
			throw UnsupportedOperationException("Type marker serializer can't actually serialize")
		}

		override fun deserialize(decoder: Decoder): T {
			throw UnsupportedOperationException("Type marker serializer can't actually serialize")
		}
	}
}