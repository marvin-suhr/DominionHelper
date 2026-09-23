package dev.msuhr.dominionkingdoms.data

import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.BuildConfig
import dev.msuhr.dominionkingdoms.BuildConfig.SHARE_SERVICE_BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads kingdoms to the share web service (web/ in this repo).
 * API contract:
 *   POST /api/kingdoms              -> { "id": "...", "url": "https://..." }
 *   GET  /api/kingdoms/:id          -> full kingdom JSON (used when opening a shared link)
 */
@Singleton
class KingdomSharingService @Inject constructor() {

    @Serializable
    data class ShareKingdomPayload(
        val name: String,
        val playerCount: Int,
        val randomCards: List<Int>,
        val landscapeCards: List<Int>,
        val basicCards: Map<String, Int>,
        val dependentCards: Map<String, Int>,
        val startingCards: Map<String, Int>,
    )

    @Serializable
    data class ShareKingdomResponse(
        val id: String,
        val url: String,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Uploads the kingdom and returns the public share URL.
     * @throws IOException on network problems or non-2xx responses.
     */
    suspend fun uploadKingdom(kingdom: Kingdom, playerCount: Int): String {
        return withContext(Dispatchers.IO) {
            val payload = ShareKingdomPayload(
                name = kingdom.name,
                playerCount = playerCount,
                randomCards = kingdom.randomCards.keys.map { it.id },
                landscapeCards = kingdom.landscapeCards.keys.map { it.id },
                basicCards = kingdom.basicCards.mapKeys { it.key.name },
                dependentCards = kingdom.dependentCards.mapKeys { it.key.name },
                startingCards = kingdom.startingCards.mapKeys { it.key.name },
            )

            val connection = URL("$SHARE_SERVICE_BASE_URL/api/kingdoms").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                connection.outputStream.use { out ->
                    out.write(json.encodeToString(ShareKingdomPayload.serializer(), payload).toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw IOException("Upload failed (HTTP $code): $errorBody")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val response = json.decodeFromString(ShareKingdomResponse.serializer(), body)
                response.url.ifEmpty { "${SHARE_SERVICE_BASE_URL}/${response.id}" }
            } finally {
                connection.disconnect()
            }
        }
    }
}
