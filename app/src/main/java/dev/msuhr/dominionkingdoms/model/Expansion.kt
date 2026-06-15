package dev.msuhr.dominionkingdoms.model

import android.content.Context
import android.util.Log
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable(with = ExpansionSizeSerializer::class)
enum class ExpansionSize(val text: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

@Entity(tableName = "expansions")
@Serializable
data class Expansion(
    @PrimaryKey val id: String, // e.g., "BASE", "INTRIGUE"
    val name: String,
    @SerialName("image_name") val imageName: String
)

@Entity(
    tableName = "editions",
    foreignKeys = [
        ForeignKey(
            entity = Expansion::class,
            parentColumns = ["id"],
            childColumns = ["expansionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["expansionId"])]
)
@Serializable
data class Edition(
    @PrimaryKey val id: String, // e.g., "BASE_1E", "BASE_2E"
    val expansionId: String,
    val editionNumber: Int,
    @SerialName("isOwned") val isOwned: Boolean,
    val year: Int,
    val size: ExpansionSize,
    @SerialName("image_name") val imageName: String,
    val cards: Int,
    val landscapes: Int
)

// Relation class
data class ExpansionWithEditions(
    @Embedded val expansion: Expansion,
    @Relation(
        parentColumn = "id",
        entityColumn = "expansionId"
    )
    val editions: List<Edition>
) {
    val name: String get() = expansion.name
    val id: String get() = expansion.id
    
    // Helpers to maintain compatibility with existing UI logic
    val firstEdition: Edition? get() = editions.find { it.editionNumber == 1 }
    val secondEdition: Edition? get() = editions.find { it.editionNumber == 2 }

    val isFirstEditionOwned: Boolean get() = firstEdition?.isOwned == true
    val isSecondEditionOwned: Boolean get() = secondEdition?.isOwned == true

    val isSharedSecondEdition: Boolean get() = secondEdition?.expansionId == "CORNUCOPIA_GUILDS"

    val hasMultipleEditions: Boolean get() = editions.size >= 2

    // Special logic for Cornucopia and Guilds
    val displayImageName: String get() =
        if (isSharedSecondEdition && !isSecondEditionOwned) {
            firstEdition?.imageName ?: ""
        } else { activeEdition?.imageName ?: "" }

    // TODO:  I REALLY don't like this
    val displayName: String
        get() = when {
            isSecondEditionOwned && secondEdition?.expansionId != id -> {
                // When second edition is owned and belongs to a different expansion, use that expansion's name
                when (secondEdition?.expansionId) {
                    "CORNUCOPIA_GUILDS" -> "Cornucopia & Guilds"
                    else -> name
                }
            }
            else -> name
        }

    fun isAnyOwned(): Boolean = editions.any { it.isOwned }
    fun isBothOwned(): Boolean = editions.count { it.isOwned } >= 2

    /**
     * Logic: Prefers Owned 2E -> any other Owned Edition -> Unowned 2E (for preview) -> fallback to First available.
     */
    val activeEdition: Edition? 
        get() = editions.find { it.editionNumber == 2 && it.isOwned } 
            ?: editions.find { it.isOwned } 
            ?: editions.find { it.editionNumber == 2 }
            ?: editions.firstOrNull()

    val ownershipText: String
        get() = when {
            secondEdition == null && firstEdition?.isOwned == true -> "Owned"
            firstEdition?.isOwned == true && secondEdition?.isOwned == true -> "Both editions"
            firstEdition?.isOwned == true -> "First edition"
            secondEdition?.isOwned == true -> "Second edition"
            else -> "Not owned"
        }
}

enum class OwnedEdition {
    NONE,
    FIRST,
    SECOND,
    BOTH
}

/**
 * Data class for parsing the hierarchical JSON structure
 */
@Serializable
data class ExpansionData(
    val expansions: List<Expansion>,
    val editions: List<Edition>
)

// To data package
fun loadExpansionsFromAssets(context: Context): ExpansionData {
    val jsonString: String
    try {
        val inputStream = context.assets.open("sets.json")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        jsonString = String(buffer, Charsets.UTF_8)
    } catch (e: IOException) {
        Log.e("loadExpansionsFromAssets", "Error reading from assets", e)
        return ExpansionData(emptyList(), emptyList())
    }

    val json = Json {
        ignoreUnknownKeys = false // Fail on unknown keys to catch typos/structural issues
        coerceInputValues = true // Use default values for missing fields
    }

    return json.decodeFromString(jsonString)
}

// Custom serializer for case-insensitive enum deserialization
object ExpansionSizeSerializer : KSerializer<ExpansionSize> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ExpansionSize", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ExpansionSize) {
        encoder.encodeString(value.text)
    }

    override fun deserialize(decoder: Decoder): ExpansionSize {
        val value = decoder.decodeString()
        return ExpansionSize.valueOf(value.uppercase())
    }
}
