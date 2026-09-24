package com.icure.cardinal.bridge.bench.support

import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.model.DecryptedContact
import com.icure.cardinal.sdk.model.DecryptedPatient
import com.icure.cardinal.sdk.model.base.CodeStub
import com.icure.cardinal.sdk.model.embed.AccessLevel
import com.icure.cardinal.sdk.model.embed.DecryptedContent
import com.icure.cardinal.sdk.model.embed.DecryptedService
import com.icure.cardinal.sdk.model.embed.DecryptedSubContact
import com.icure.cardinal.sdk.model.embed.Measure
import com.icure.cardinal.sdk.model.embed.ReferenceRange
import com.icure.cardinal.sdk.model.embed.ServiceLink
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A group of [contacts] lab result contacts with [servicesPerContact] services each.
 * If [stringBytes] is set, the string results have exactly this length instead of a realistic one (3 to 33 characters).
 */
data class Tier(val contacts: Int, val servicesPerContact: Int, val stringBytes: Int? = null) {
	val label: String get() = listOfNotNull(contacts, servicesPerContact, stringBytes).joinToString("x")

	companion object {
		/**
		 * The contacts of the request that got stuck on qa (11 contacts, the big ones with 187 services), scaled up, and
		 * a few contacts with huge string results.
		 */
		const val DEFAULT = "11x187,100x187,300x187,20x4x1000000"

		/**
		 * Parses a comma separated list of `<contacts>x<servicesPerContact>[x<stringBytes>]`.
		 */
		fun parseAll(spec: String): List<Tier> =
			spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { tier ->
				val parts = tier.split('x').map { it.toIntOrNull() }
				require(parts.size in 2..3 && parts.all { it != null && it > 0 }) {
					"Invalid tier `$tier`, expected <contacts>x<servicesPerContact>[x<stringBytes>]"
				}
				Tier(parts[0]!!, parts[1]!!, parts.getOrNull(2))
			}
	}
}

/**
 * A seeded [tier]: its contacts are the only ones with an opening date in `[startDate, endDate]`.
 */
data class SeededTier(val tier: Tier, val startDate: Long, val endDate: Long)

/**
 * Seeds contacts modelled after the lab result contacts returned by the request that got stuck on qa: a transaction
 * with one service per result, where about a fifth of the results are measures with a reference range and the others
 * short strings, and a sub contact linking all services. Only the structure is reproduced, all values are made up.
 */
@OptIn(ExperimentalUuidApi::class)
object DatasetSeeder {
	/** Approximate maximum size of a createContacts request. */
	private const val BATCH_BYTES = 16L * 1024 * 1024

	/** Opening date of the first tier, the start of the window of the request that got stuck on qa. */
	private const val FIRST_OPENING_DATE = 20251125000000L

	/** Distinct labels and lab codes of the results in a contact, as in the qa contacts. */
	private const val DISTINCT_LABELS = 56
	private const val DISTINCT_LAB_CODES = 55

	private val labResult = codeStub("CD-TRANSACTION", "labresult")
	private val labItem = codeStub("CD-ITEM", "lab")

	/**
	 * Creates the contacts of all [tiers] as the data owner of [author], for a single patient, and shares them with
	 * [readerId].
	 *
	 * Tier n gets the window from minute 2n to minute 2n + 1 of 2025-11-25, both bounds included like in the filter,
	 * and its contacts are opened on either bound, like the qa contacts. The first tier has the window of the qa request.
	 */
	suspend fun seed(author: CardinalSdk, readerId: String, tiers: List<Tier>): List<SeededTier> {
		require(tiers.size <= 30) { "At most 30 tiers are supported" }
		val user = author.user.getCurrentUser()
		val delegates = mapOf(readerId to AccessLevel.Read)
		val patient = author.patient.createPatient(
			author.patient.withEncryptionMetadata(
				DecryptedPatient(id = Uuid.random().toString(), firstName = "Bench", lastName = "Patient"),
				user = user,
				delegates = delegates,
			)
		)
		return tiers.mapIndexed { index, tier ->
			val startDate = FIRST_OPENING_DATE + index * 200L
			val endDate = startDate + 100
			// Content is encrypted, so it grows by a third once base64 encoded.
			val approxContactBytes = tier.servicesPerContact * (700 + (tier.stringBytes ?: 20) * 4L / 3) + 2_000
			val batchSize = (BATCH_BYTES / approxContactBytes).toInt().coerceIn(1, 100)
			(0 until tier.contacts).chunked(batchSize).forEach { batch ->
				author.contact.createContacts(
					batch.map { i ->
						author.contact.withEncryptionMetadata(
							labResultContact(tier, i, openingDate = if (i % 2 == 0) startDate else endDate),
							patient = patient,
							user = user,
							delegates = delegates,
						)
					}
				)
			}
			println("Seeded tier ${tier.label}")
			SeededTier(tier, startDate = startDate, endDate = endDate)
		}
	}

	private fun labResultContact(tier: Tier, contactIndex: Int, openingDate: Long): DecryptedContact {
		val services = (0 until tier.servicesPerContact).map { s ->
			DecryptedService(
				id = Uuid.random().toString(),
				label = "Lab test ${s % DISTINCT_LABELS + 1}",
				index = s.toLong(),
				valueDate = openingDate,
				openingDate = openingDate,
				tags = setOf(labItem, codeStub("CD-LAB", "test-${s % DISTINCT_LAB_CODES + 1}")),
				content = mapOf("fr" to labResultContent(tier, contactIndex, s)),
			)
		}
		return DecryptedContact(
			id = Uuid.random().toString(),
			openingDate = openingDate,
			closingDate = openingDate,
			encounterType = labResult,
			tags = setOf(labResult),
			services = services.toSet(),
			subContacts = setOf(DecryptedSubContact(services = services.map { ServiceLink(serviceId = it.id) })),
		)
	}

	private fun labResultContent(tier: Tier, contactIndex: Int, serviceIndex: Int): DecryptedContent =
		if (serviceIndex % 9 == 4 || serviceIndex % 9 == 8) {
			// Measures: most have a unit, all have one reference range, most of them with both bounds.
			DecryptedContent(
				measureValue = Measure(
					value = (serviceIndex * 13 + contactIndex) % 1000 / 10.0,
					unit = if (serviceIndex % 4 == 0) null else "mg/dL",
					referenceRanges = listOf(
						if (serviceIndex % 7 == 0) ReferenceRange(high = 50.0) else ReferenceRange(low = 1.0, high = 50.0)
					),
				)
			)
		} else {
			val length = tier.stringBytes ?: (3 + (serviceIndex * 7 + contactIndex) % 31)
			DecryptedContent(stringValue = "Bench lab result ".repeat(length / 17 + 1).take(length))
		}

	private fun codeStub(type: String, code: String) =
		CodeStub(id = "$type|$code|1", type = type, code = code, version = "1")
}
