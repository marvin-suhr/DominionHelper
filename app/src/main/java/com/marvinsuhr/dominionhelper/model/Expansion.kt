package com.marvinsuhr.dominionhelper.model

import android.content.Context
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

data class ExpansionWithEditions(
    val name: String, // The name of the Expansion (e.g., "Base", "Intrigue")
    val firstEdition: Expansion? = null, // -> LIST of editions?
    val secondEdition: Expansion? = null,
    val image: String,
    val isExpanded: Boolean = false
)

enum class OwnedEdition {
    NONE,
    FIRST,
    SECOND,
    BOTH
}

enum class ExpansionSize(val text: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

@Entity(tableName = "expansions")
data class Expansion(
    @PrimaryKey(autoGenerate = false) val id: String,
    val name: String,
    val edition: Int,
    @SerializedName("image_name") val imageName: String,
    val isOwned: Boolean,
    val size: ExpansionSize,
    val year: Int
)

// To data package
fun loadExpansionsFromAssets(context: Context): List<Expansion> {
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
        return emptyList()
    }

    val gson = GsonBuilder()
        .registerTypeAdapter(Set::class.java, SetTypeAdapter())
        .registerTypeAdapter(ExpansionSize::class.java, ExpansionSizeTypeAdapter())
        .create()

    val expansionListType = object : TypeToken<List<Expansion>>() {}.type
    val expansionList: List<Expansion> = gson.fromJson(jsonString, expansionListType)
    return expansionList
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
