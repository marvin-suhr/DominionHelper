package dev.msuhr.dominionkingdoms.utils

import androidx.room.TypeConverter
import dev.msuhr.dominionkingdoms.model.Category
import dev.msuhr.dominionkingdoms.model.Type
import dev.msuhr.dominionkingdoms.model.Set
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = false // Fail on unknown keys to catch typos/structural issues
        coerceInputValues = true // Use default values for missing fields
    }

    @TypeConverter
    fun fromTypeList(value: List<Type>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toTypeList(value: String): List<Type> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromSetList(value: List<Set>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toSetList(value: String): List<Set> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromCategoryList(value: List<Category>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toCategoryList(value: String): List<Category> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        if (value == null) {
            return null
        }
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value == null) {
            return null
        }
        return json.decodeFromString(value)
    }
}