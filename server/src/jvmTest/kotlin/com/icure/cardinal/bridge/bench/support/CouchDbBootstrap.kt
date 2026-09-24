package com.icure.cardinal.bridge.bench.support

import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * iCure Cloud CouchDB bootstrap helpers — writes the group / user / config docs
 * directly into CouchDB, no Kraken API involved.
 *
 * Adapted from `icure-multiplatform-sdk/buildSrc/src/main/kotlin/tasks/KrakenUtils.kt`
 * and the `bootstrapCloud(...)` recipe from `tasks/InitializeTestEnvironment.kt`.
 * Kept self-contained here so we don't need the `io.icure:icure-e2e-test-setup` library.
 *
 * Copied from khermes (`e2e/support/CouchDbBootstrap.kt`).
 */

/**
 * Bootstraps the Cloud environment for a new base group:
 *  - creates the `icure-$groupId-base` and `_users` CouchDB databases
 *  - creates the group user inside `icure-$groupId-base` and mirrors it into `icure-__-base`
 *  - creates a CouchDB DB user for the group
 *  - creates the group document in `icure-__-config`
 */
internal suspend fun bootstrapCloud(
	httpClient: HttpClient,
	groupId: String,
	groupPassword: String,
	groupUserId: String,
	groupUserLogin: String,
	groupUserPasswordHash: String,
	couchDbUrl: String,
	couchDbUser: String,
	couchDbPassword: String,
	rootUserRoles: Map<String, List<String>>,
	defaultQuotas: List<Int>,
) {
	createNewDatabase(httpClient, "icure-$groupId-base", couchDbUrl, couchDbUser, couchDbPassword)
	createNewDatabase(httpClient, "_users", couchDbUrl, couchDbUser, couchDbPassword)
	createUserIn(httpClient, groupId, groupUserId, groupUserLogin, groupUserPasswordHash, null, couchDbUrl, couchDbUser, couchDbPassword)
	createUserInBase(httpClient, groupId, groupUserId, groupUserLogin, groupUserPasswordHash, null, couchDbUrl, couchDbUser, couchDbPassword)
	createNewDbUser(httpClient, groupId, groupPassword, couchDbUrl, couchDbUser, couchDbPassword)
	createNewGroupInConfig(
		httpClient = httpClient,
		groupId = groupId,
		groupPassword = groupPassword,
		couchDBUrl = couchDbUrl,
		couchDbUser = couchDbUser,
		couchDbPassword = couchDbPassword,
		defaultRoles = rootUserRoles,
		quotas = defaultQuotas,
	)
}

internal suspend fun createNewDatabase(
	httpClient: HttpClient,
	databaseName: String,
	couchDBUrl: String,
	couchDbUser: String,
	couchDbPassword: String,
) {
	val response = httpClient.put("$couchDBUrl/$databaseName") {
		basicAuth(couchDbUser, couchDbPassword)
		contentType(ContentType.Application.Json)
		setBody("{}")
	}
	if (!response.status.isSuccess()) {
		println("Could not create database $databaseName: ${response.status} - ${response.bodyAsText()}")
	}
}

internal suspend fun createUserIn(
	httpClient: HttpClient,
	groupId: String,
	groupUserId: String,
	groupUserLogin: String,
	groupUserPasswordHash: String,
	groupUserHcpId: String? = null,
	couchDBUrl: String,
	couchDbUser: String,
	couchDbPassword: String,
) {
	val response = httpClient.post("$couchDBUrl/icure-$groupId-base") {
		basicAuth(couchDbUser, couchDbPassword)
		contentType(ContentType.Application.Json)
		setBody(
			"""{
		"_id" : "$groupUserId",
		"login" : "$groupUserLogin",
		"passwordHash" : "$groupUserPasswordHash",
		"isUse2fa" : true,
		"type" : "database",
		"status" : "ACTIVE",
""" +
				(if (groupUserHcpId != null) """ "healthcarePartyId": "$groupUserHcpId", """ else "") +
				""" "java_type" : "org.taktik.icure.entities.User" }"""
		)
	}
	if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
		throw RuntimeException("Could not create a new DB User in icure-$groupId-base")
	}
}

internal suspend fun createUserInBase(
	httpClient: HttpClient,
	groupId: String? = null,
	groupUserId: String,
	groupUserLogin: String,
	groupUserPasswordHash: String,
	groupUserHcpId: String? = null,
	couchDBUrl: String,
	couchDbUser: String,
	couchDbPassword: String,
) {
	val userId = if (groupId == null) groupUserId else "$groupId:$groupUserId"
	val response = httpClient.post("$couchDBUrl/icure-${if (groupId == null) "" else "__-"}base") {
		basicAuth(couchDbUser, couchDbPassword)
		contentType(ContentType.Application.Json)
		setBody(
			"""{
  "_id" : "$userId",
  "login" : "$groupUserLogin",
  "passwordHash" : "$groupUserPasswordHash",
  "type" : "database",
  "status" : "ACTIVE",
""" +
				(if (groupId != null) """"groupId" : "$groupId",
  "permissions": [
			{
			  "grants": [
				{
				  "java_type": "org.taktik.icure.entities.security.AlwaysPermissionItem",
				  "type": "ADMIN"
				}
			  ]
			}
		  ],
""" else "") +
				(if (groupUserHcpId != null) """ "healthcarePartyId": "$groupUserHcpId", """ else "") +
				"  \"java_type\" : \"org.taktik.icure.entities.User\"" +
				"}"
		)
	}
	if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
		throw RuntimeException("Could not create DB User $groupId:$groupUserId in icure-__-base")
	}
}

internal suspend fun createNewDbUser(
	httpClient: HttpClient,
	groupId: String,
	groupPassword: String,
	couchDBUrl: String,
	couchDbUser: String,
	couchDbPassword: String,
) {
	val response = httpClient.post("$couchDBUrl/_users") {
		basicAuth(couchDbUser, couchDbPassword)
		contentType(ContentType.Application.Json)
		setBody(
			"""{
  "_id" : "org.couchdb.user:$groupId",
  "name" : "$groupId",
  "password" : "$groupPassword",
  "roles" : [],
  "type" : "user"
}"""
		)
	}
	if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
		throw RuntimeException("Could not create a new DB User in _users")
	}
}

internal suspend fun createNewGroupInConfig(
	httpClient: HttpClient,
	groupId: String,
	groupPassword: String,
	couchDBUrl: String,
	couchDbUser: String,
	couchDbPassword: String,
	superGroup: String? = null,
	defaultRoles: Map<String, List<String>> = emptyMap(),
	quotas: List<Int> = listOf(1000, 2, 5),
) {
	val response = httpClient.post("$couchDBUrl/icure-__-config") {
		basicAuth(couchDbUser, couchDbPassword)
		contentType(ContentType.Application.Json)
		setBody(
			"""{
		  "_id": "$groupId",
		  "java_type": "org.taktik.icure.entities.Group",
		  "name": "$groupId",
		  "password": "$groupPassword",
		  ${superGroup?.let { "\"superGroup\": \"$it\"," } ?: ""}
		  "defaultUserRoles": {
			${defaultRoles.entries.joinToString(",") { (k, v) ->
				"\"$k\": [${v.joinToString(", ") { "\"$it\"" }}]"
			}}
		  },
		  "properties": [
			${quotas.mapIndexed { index, quota ->
				"""
					{
					  "type": {
						"identifier": "com.icure.dbs.quota.$index",
						"type": "INTEGER"
					  },
					  "typedValue": {
						"type": "INTEGER",
						"integerValue": $quota
					  }
					}
					""".trimIndent()
			}.joinToString(",")}
		  ],
		  "tags": [
			{
			  "id": "IC-GROUP|root|1.0",
			  "type": "IC-GROUP",
			  "code": "root",
			  "version": "1.0"
			}
		  ],
		  "rev_history": {},
		  "servers": []
		}"""
		)
	}
	if (!response.status.isSuccess() && response.status != HttpStatusCode.Conflict) {
		throw RuntimeException("Could not create a new groupDB in _config")
	}
}
