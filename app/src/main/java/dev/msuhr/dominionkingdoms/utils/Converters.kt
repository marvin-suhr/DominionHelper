package dev.msuhr.dominionkingdoms.utils

import androidx.room.TypeConverter
import dev.msuhr.dominionkingdoms.model.Category
import dev.msuhr.dominionkingdoms.model.Type
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.msuhr.dominionkingdoms.model.Set

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromTypeList(value: List<Type>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTypeList(value: String): List<Type> {
        val type = object : TypeToken<List<Type>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromSetList(value: List<Set>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSetList(value: String): List<Set> {
        val set = object : TypeToken<List<Set>>() {}.type
        return gson.fromJson(value, set)
    }

    @TypeConverter
    fun fromCategoryList(value: List<Category>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCategoryList(value: String): List<Category> {
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        if (value == null) {
            return null
        }
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
    }
}