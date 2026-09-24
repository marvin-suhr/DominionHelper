package dev.msuhr.dominionkingdoms.data

import dev.msuhr.dominionkingdoms.model.Kingdom
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
/** Thrown when the server rejects an upload because the daily quota is used up. */
class UploadLimitException(
    val dailyLimit: Int,
    val resetAt: Long,
) : IOException("Daily upload limit reached")

@Singleton
class KingdomSharingService @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {

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
        val uploadsRemaining: Int = -1,
        val dailyLimit: Int = -1,
        val resetAt: Long? = null,
    )

    /** Response of GET /api/kingdoms/:id - what the app receives when a shared link is opened. */
    @Serializable
    data class SharedKingdomDto(
        val id: String,
        val name: String,
        val playerCount: Int,
        val randomCards: List<Int>,
        val landscapeCards: List<Int>,
        val basicCards: Map<String, Int>,
        val dependentCards: Map<String, Int>,
        val startingCards: Map<String, Int>,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Uploads the kingdom and returns the public share URL.
     * @throws IOException on network problems or non-2xx responses.
     */
    suspend fun uploadKingdom(kingdom: Kingdom, playerCount: Int): UploadedShare {
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
                connection.setRequestProperty("X-Client-Id", userPrefsRepository.getClientInstanceId())

                connection.outputStream.use { out ->
                    out.write(json.encodeToString(ShareKingdomPayload.serializer(), payload).toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""

                when (code) {
                    429 -> {
                        val parsed = try {
                            json.decodeFromString(UploadQuota.serializer(), body)
                        } catch (e: Exception) {
                            null
                        }
                        throw UploadLimitException(
                            dailyLimit = parsed?.limit ?: 5,
                            resetAt = parsed?.resetAt ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000L),
                        )
                    }
                    !in 200..299 -> throw IOException("Upload failed (HTTP $code): $body")
                }

                val response = json.decodeFromString(ShareKingdomResponse.serializer(), body)
                UploadedShare(
                    url = response.url.ifEmpty { "${SHARE_SERVICE_BASE_URL}/${response.id}" },
                    uploadsRemaining = response.uploadsRemaining,
                    dailyLimit = response.dailyLimit,
                    resetAt = response.resetAt,
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Current upload quota for this device (asks the server, not a cache). */
    suspend fun getUploadQuota(): UploadQuota {
        return withContext(Dispatchers.IO) {
            val connection = URL("$SHARE_SERVICE_BASE_URL/api/upload-quota").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-Client-Id", userPrefsRepository.getClientInstanceId())

                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IOException("HTTP $code")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString(UploadQuota.serializer(), body)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** One entry of the shared-kingdoms feed (GET /api/kingdoms). */
    @Serializable
    data class UploadedShare(
        val url: String,
        val uploadsRemaining: Int,
        val dailyLimit: Int,
        val resetAt: Long? = null,
    )

    @Serializable
    data class UploadQuota(
        val remaining: Int,
        val limit: Int,
        val resetAt: Long? = null,
    )

    @Serializable
    data class SharedKingdomSummary(
        val id: String,
        val name: String,
        val playerCount: Int = 2,
        val cardCount: Int,
        val landscapeCount: Int = 0,
        val createdAt: Long,
        val ratingAverage: Double? = null,
        val ratingCount: Int = 0,
        val previewCardIds: List<Int> = emptyList(),
    )

    @Serializable
    data class SharedKingdomsPage(
        val kingdoms: List<SharedKingdomSummary>,
        val total: Int,
    )

    suspend fun getSharedKingdoms(
        limit: Int = 20,
        offset: Int = 0,
        sort: String = "recent", // "recent" | "rating"
    ): SharedKingdomsPage {
        return withContext(Dispatchers.IO) {
            val url = "$SHARE_SERVICE_BASE_URL/api/kingdoms?limit=$limit&offset=$offset&sort=$sort"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")

                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw IOException("HTTP $code: $errorBody")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString(SharedKingdomsPage.serializer(), body)
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Fetches a shared kingdom by id (GET /api/kingdoms/:id).
     * @throws IOException on network problems, 404 if the id does not exist.
     */
    suspend fun getSharedKingdom(id: String): SharedKingdomDto {
        return withContext(Dispatchers.IO) {
            val connection = URL("$SHARE_SERVICE_BASE_URL/api/kingdoms/$id").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")

                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw IOException("HTTP $code: $errorBody")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString(SharedKingdomDto.serializer(), body)
            } finally {
                connection.disconnect()
            }
        }
    }
}
