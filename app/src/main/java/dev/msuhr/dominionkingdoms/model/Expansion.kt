package dev.msuhr.dominionkingdoms.model

import android.content.Context
import android.util.Log
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

enum class ExpansionSize(val text: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

@Entity(tableName = "expansions")
data class Expansion(
    @PrimaryKey val id: String, // e.g., "BASE", "INTRIGUE"
    val name: String,
    @SerializedName("image_name") val imageName: String
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
data class Edition(
    @PrimaryKey val id: String, // e.g., "BASE_1E", "BASE_2E"
    val expansionId: String,
    val editionNumber: Int,
    @SerializedName("isOwned") val isOwned: Boolean,
    val year: Int,
    val size: ExpansionSize,
    @SerializedName("image_name") val imageName: String,
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
    val conceptImage: String get() = expansion.imageName
    
    // Helpers to maintain compatibility with existing UI logic
    val firstEdition: Edition? get() = editions.find { it.editionNumber == 1 }
    val secondEdition: Edition? get() = editions.find { it.editionNumber == 2 }

    val isFirstEditionOwned: Boolean get() = firstEdition?.isOwned == true
    val isSecondEditionOwned: Boolean get() = secondEdition?.isOwned == true

    val isSharedSecondEdition: Boolean get() = secondEdition?.expansionId == "CORNUCOPIA_GUILDS"

    val hasMultipleEditions: Boolean get() = editions.size >= 2

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

    val gson = GsonBuilder()
        .registerTypeAdapter(Set::class.java, SetTypeAdapter())
        .registerTypeAdapter(ExpansionSize::class.java, ExpansionSizeTypeAdapter())
        .create()

    return gson.fromJson(jsonString, ExpansionData::class.java)
}

class ExpansionSizeTypeAdapter : TypeAdapter<ExpansionSize>() {

    override fun write(out: JsonWriter, value: ExpansionSize?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.name) // Write as the enum name (e.g., "BASE")
        }
    }

    override fun read(reader: JsonReader): ExpansionSize? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val value = reader.nextString()
        return ExpansionSize.valueOf(value.uppercase()) // Convert from string to Set enum
    }
}
