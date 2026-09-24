package com.omrx.perch.auth

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.omrx.perch.logging.Logger
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions

private val logger = Logger.get("GmailAuth")
private val SCOPES = listOf(
    "https://mail.google.com/",
    "https://www.googleapis.com/auth/userinfo.email"
)
private val JSON_FACTORY = GsonFactory.getDefaultInstance()
private val TOKENS_DIR = File(System.getProperty("user.home"), ".config/perch/StoredTokens")

fun getGmailCredential(): Credential {
    logger.info("Retrieving Gmail credentials...")
    return try {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val secretsStream = object {}.javaClass.getResourceAsStream("/credentials.json")
            ?: error("Missing credentials.json in resources")
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(secretsStream))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(TOKENS_DIR))
            .setAccessType("offline")
            .build()

        // Ensure the tokens directory has secure permissions
        if (!TOKENS_DIR.exists()) {
            TOKENS_DIR.mkdirs()
            val perms = PosixFilePermissions.fromString("rwx------")
            Files.setPosixFilePermissions(Paths.get(TOKENS_DIR.toURI()), perms)
            logger.debug("Created secure tokens directory")
        }

        logger.info("Gmail credentials obtained successfully")
        AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
    } catch (e: Exception) {
        logger.error("Failed to retrieve Gmail credentials", e)
        throw e
    }
}

fun getUserEmail(credential: Credential): String {
    logger.info("Fetching user email...")
    return try {
        val transport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val requestFactory = transport.createRequestFactory(credential)

        val request = requestFactory.buildGetRequest(
            GenericUrl("https://www.googleapis.com/oauth2/v2/userinfo")
        )
        request.parser = JsonObjectParser(JSON_FACTORY)

        val response = request.execute()
        val json = response.parseAs(Map::class.java)
        val email = json["email"] as String
        logger.info("User email: $email")
        email
    } catch (e: Exception) {
        logger.error("Failed to fetch user email", e)
        throw e
    }
}